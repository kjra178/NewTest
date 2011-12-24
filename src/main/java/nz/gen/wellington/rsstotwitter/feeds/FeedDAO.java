package nz.gen.wellington.rsstotwitter.feeds;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nz.gen.wellington.rsstotwitter.model.Feed;
import nz.gen.wellington.rsstotwitter.model.FeedItem;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.GeoRSSUtils;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

public class FeedDAO {

    public final Logger log = Logger.getLogger(FeedDAO.class);

    @SuppressWarnings("unchecked")
	public List<FeedItem> loadFeedItems(Feed feed) {
    	SyndFeed syndfeed = loadSyndFeedWithFeedFetcher(feed.getUrl());
    	if (syndfeed == null) {
    		log.warn("Could not load syndfeed from url: " + feed.getUrl());    		
    	}
    	
    	List<FeedItem> feedItems = new ArrayList<FeedItem>();
        Iterator<SyndEntry> feedItemsIterator = syndfeed.getEntries().iterator();
        while (feedItemsIterator.hasNext()) {        	
        	SyndEntry feedItem = (SyndEntry) feedItemsIterator.next();
        	
        	Double latitude = null;
        	Double longitude = null;        	
			GeoRSSModule geoModule = (GeoRSSModule) GeoRSSUtils.getGeoRSS(feedItem);
			if (geoModule != null && geoModule.getPosition() != null) {
				latitude = geoModule.getPosition().getLatitude();
				longitude = geoModule.getPosition().getLongitude();
				log.debug("Rss item '" + feedItem.getTitle() + "' has position information: " + latitude + "," + longitude);
			}
        	
        	feedItems.add(new FeedItem(feed, feedItem.getTitle(), feedItem.getUri(), feedItem.getLink(), feedItem.getPublishedDate(), feedItem.getAuthor(), latitude, longitude));
        }
        return feedItems;
    }
    
    private SyndFeed loadSyndFeedWithFeedFetcher(String feedUrl) {
        log.info("Loading SyndFeed from url: " + feedUrl);
    
        URL url;
        try {
            url = new URL(feedUrl);
            FeedFetcher fetcher = new HttpURLFeedFetcher();            
            SyndFeed feed = fetcher.retrieveFeed(url);
            return feed;
        } catch (Exception e) {
            log.warn("Error while fetching feed: " + e.getMessage());
        }
        return null;
    }

}
