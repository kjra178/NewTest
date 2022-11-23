package nz.gen.wellington.rsstotwitter.twitter;

import com.google.common.base.Strings;
import nz.gen.wellington.rsstotwitter.mastodon.MastodonService;
import nz.gen.wellington.rsstotwitter.model.*;
import nz.gen.wellington.rsstotwitter.repositories.mongo.TwitterHistoryDAO;
import nz.gen.wellington.rsstotwitter.timers.Updater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TwitterUpdater implements Updater {

    private final static Logger log = LogManager.getLogger(TwitterUpdater.class);

    private final TwitterHistoryDAO twitterHistoryDAO;
    private final TwitterService twitterService;
    private final TweetFromFeedItemBuilder tweetFromFeedItemBuilder;
    private final MastodonService mastodonService;

    @Autowired
    public TwitterUpdater(TwitterHistoryDAO twitterHistoryDAO, TwitterService twitterService, TweetFromFeedItemBuilder tweetFromFeedItemBuilder,
                          MastodonService mastodonService) {
        this.twitterHistoryDAO = twitterHistoryDAO;
        this.twitterService = twitterService;
        this.tweetFromFeedItemBuilder = tweetFromFeedItemBuilder;
        this.mastodonService = mastodonService;
    }

    public void updateFeed(Account account, Feed feed, List<FeedItem> feedItems, Destination destination) {
        log.info("Calling update feed for account '" + account.getUsername() + "' with " + feedItems.size() + " feed items");
        final long tweetsSentInLastHour = twitterHistoryDAO.getNumberOfTwitsInLastHour(feed, account);
        final long tweetsSentInLastTwentyForHours = twitterHistoryDAO.getNumberOfTwitsInLastTwentyFourHours(feed, account);
        log.info("Tweets sent in last hour: " + tweetsSentInLastHour);
        log.info("Tweets sent in last 24 hours: " + tweetsSentInLastTwentyForHours);

        long tweetsSentThisRound = 0;
        for (FeedItem feedItem : feedItems) {
            if (hasExceededMaxTweetsPerHourRateLimit(tweetsSentInLastHour + tweetsSentThisRound) || hasExceededMaxTweetsPerDayFeedRateLimit(tweetsSentInLastTwentyForHours + tweetsSentThisRound)) {
                log.info("Feed '" + feed.getUrl() + "' has exceeded maximum tweets per hour or day rate limit; returning");
                return;
            }

            boolean publisherRateLimitExceeded = isPublisherRateLimitExceed(feed, feedItem.getAuthor());
            if (!publisherRateLimitExceeded) {
                if (processItem(account, feedItem, destination)) {
                    tweetsSentThisRound++;
                }
            } else {
                log.info("Publisher '" + feedItem.getAuthor() + "' has exceed the rate limit; skipping feeditem from this publisher");
            }
        }

        log.info("Twitter update completed for feed: " + feed.getUrl());
    }

    private boolean processItem(Account account, FeedItem feedItem, Destination destination) {
        final String guid = feedItem.getGuid();

        final boolean isLessThanOneWeekOld = isLessThanOneWeekOld(feedItem);
        if (!isLessThanOneWeekOld) {
            log.debug("Not tweeting as the item's publication date is more than one week old: " + guid);
            return false;
        }

        if (!twitterHistoryDAO.hasAlreadyBeenTweeted(account, guid, Destination.TWITTER)) {
            try {
                final Tweet tweet = tweetFromFeedItemBuilder.buildTweetFromFeedItem(feedItem);
                final Tweet updatedStatus = twitterService.tweet(tweet, account);
                if (updatedStatus != null) {
                    twitterHistoryDAO.markAsTweeted(account, feedItem, updatedStatus, Destination.TWITTER);

                    // Echo to Mastodon spike
                    // TODO move to separate updater
                    mastodonService.post(tweet.getText());

                    return true;
                }

            } catch (Exception e) {
                log.warn("Failed to tweet: " + feedItem.getTitle(), e);
            }

        } else {
            log.debug("Not tweeting as guid has already been tweeted: " + guid);
        }
        return false;
    }

    private boolean hasExceededMaxTweetsPerHourRateLimit(long tweetsSent) {
        return tweetsSent >= TwitterSettings.MAX_TWITS_PER_HOUR;
    }

    private boolean hasExceededMaxTweetsPerDayFeedRateLimit(long tweetsSent) {
        return tweetsSent >= TwitterSettings.MAX_TWITS_PER_DAY;
    }

    private boolean isPublisherRateLimitExceed(Feed feed, String publisher) {
        if (Strings.isNullOrEmpty(publisher)) {
            return false;
        }

        final int numberOfPublisherTwitsInLastTwentyFourHours = twitterHistoryDAO.getNumberOfTwitsInLastTwentyFourHours(feed, publisher);
        log.debug("Publisher '" + publisher + "' has made " + numberOfPublisherTwitsInLastTwentyFourHours + " twits in the last 24 hours");
        return numberOfPublisherTwitsInLastTwentyFourHours >= TwitterSettings.MAX_PUBLISHER_TWITS_PER_DAY;
    }

    private boolean isLessThanOneWeekOld(FeedItem feedItem) {
        final DateTime sevenDaysAgo = new DateTime().minusDays(7);
        return new DateTime(feedItem.getPublishedDate()).isAfter(sevenDaysAgo);
    }

}
