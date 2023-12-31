package nz.gen.wellington.rsstotwitter.repositories.mongo;

import nz.gen.wellington.rsstotwitter.model.*;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TwitterHistoryDAOTest {

    String mongoDatabase = "rsstotwittertest" + UUID.randomUUID();

    @Test
    public void canRecordTweetedItemsByFeedAndGUID() {
        String mongoHost = System.getenv("MONGO_HOST");
        if (mongoHost == null) {
            mongoHost = "localhost";
        }

        DataStoreFactory dataStoreFactory = new DataStoreFactory("mongodb://" + mongoHost + ":27017", mongoDatabase);
        TwitterHistoryDAO twitterHistoryDAO = new TwitterHistoryDAO(dataStoreFactory);
        AccountDAO accountDAO = new AccountDAO(dataStoreFactory);

        Feed feed = new Feed("https://wellington.gen.nz/rss");
        String link = "https://wellington.gen.nz/a-post";
        FeedItem feedItem = new FeedItem(feed, "A post", link, link, null, null, null);
        Tweet tweet = new Tweet();

        Account account = new Account();
        account.setId(123L);
        accountDAO.saveAccount(account);

        Account anotherAccount = new Account();
        anotherAccount.setId(456L);
        accountDAO.saveAccount(anotherAccount);

        twitterHistoryDAO.markAsTweeted(account, feedItem, tweet, Destination.TWITTER);

        assertTrue(twitterHistoryDAO.hasAlreadyBeenPublished(account, link, Destination.TWITTER));
        assertFalse(twitterHistoryDAO.hasAlreadyBeenPublished(account, "http://localhost/not-seen-before", Destination.TWITTER));
        assertFalse(twitterHistoryDAO.hasAlreadyBeenPublished(account, link, Destination.MASTODON));
        assertFalse(twitterHistoryDAO.hasAlreadyBeenPublished(anotherAccount, link, Destination.TWITTER));
    }

}