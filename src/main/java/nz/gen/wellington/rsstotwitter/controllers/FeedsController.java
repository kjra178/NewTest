package nz.gen.wellington.rsstotwitter.controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import nz.gen.wellington.rsstotwitter.feeds.FeedService;
import nz.gen.wellington.rsstotwitter.forms.FeedDetails;
import nz.gen.wellington.rsstotwitter.model.*;
import nz.gen.wellington.rsstotwitter.repositories.mongo.JobDAO;
import nz.gen.wellington.rsstotwitter.repositories.mongo.TwitterHistoryDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FeedsController {

  private final static Logger log = LogManager.getLogger(FeedsController.class);

  private final LoggedInUserFilter loggedInUserFilter;
  private final JobDAO jobDAO;
  private final FeedService feedService;
  private final TwitterHistoryDAO twitterHistoryDAO;

  private final List<Destination> allDestinations = Lists.newArrayList(Destination.values());

  @Autowired
  public FeedsController(LoggedInUserFilter loggedInUserFilter, JobDAO jobDAO, FeedService feedService, TwitterHistoryDAO twitterHistoryDAO) {
    this.loggedInUserFilter = loggedInUserFilter;
    this.jobDAO = jobDAO;
    this.feedService = feedService;
    this.twitterHistoryDAO = twitterHistoryDAO;
  }

  @RequestMapping(value = "/new", method = RequestMethod.GET)
  public ModelAndView newFeed(@ModelAttribute("feedDetails") FeedDetails feedDetails, HttpServletRequest request) {
    Account loggedInUser = loggedInUserFilter.getLoggedInUser(request);
    if (loggedInUser != null) {
      return renderNewFeedForm(feedDetails, loggedInUser);

    } else {
      return redirectToSignInPage();
    }
  }

  @RequestMapping(value = "/new", method = RequestMethod.POST)
  public ModelAndView newFeedSubmit(@Valid @ModelAttribute("feedDetails") FeedDetails feedDetails, BindingResult result, HttpServletRequest request) {
    Account loggedInUser = loggedInUserFilter.getLoggedInUser(request);
    if (loggedInUser != null) {
      if (result.hasErrors()) {
        log.info("Feed form errors: " + result.getAllErrors());
        return renderNewFeedForm(feedDetails, loggedInUser);
      }

      Feed feed = new Feed(feedDetails.getUrl());
      FeedToTwitterJob job = new FeedToTwitterJob(feed, loggedInUser, Sets.newHashSet(allDestinations));
      log.info("Creating job: " + job);

      jobDAO.save(job);

      return new ModelAndView(new RedirectView("/"));

    } else {
      return redirectToSignInPage();
    }
  }

  @RequestMapping(value = "/feeds/{id}", method = RequestMethod.GET)
  public ModelAndView feed(@PathVariable String id, HttpServletRequest request) {
    Account loggedInUser = loggedInUserFilter.getLoggedInUser(request);
    if (loggedInUser != null) {
      FeedToTwitterJob job = jobDAO.getByObjectId(id);
      List<FeedItem> feedItems = feedService.loadFeedItems(job.getFeed());

      long numberOfTwitsInLastHour = allDestinations.stream().mapToLong( destination ->
              twitterHistoryDAO.getNumberOfTwitsInLastHour(job.getFeed(), job.getAccount(), destination)
      ).sum();
      long numberOfTwitsInLastTwentyFourHours = allDestinations.stream().mapToLong( destination ->
              twitterHistoryDAO.getNumberOfTwitsInLastTwentyFourHours(job.getFeed(), job.getAccount(), destination)
      ).sum();

      ActivitySummary activity = new ActivitySummary(numberOfTwitsInLastHour, numberOfTwitsInLastTwentyFourHours);

      List<Pair<FeedItem, List<TwitterEvent>>> withTweets = feedItems != null ? feedItems.stream().map(
              feedItem -> new Pair<>(feedItem, allDestinations.stream().map (destination -> twitterHistoryDAO.tweetsForGuid(job.getAccount(), feedItem.getGuid(), destination)).flatMap(List::stream).collect(Collectors.toList()))
      ).collect(Collectors.toList()) : Lists.newArrayList();  // TODO push this null back up

      return new ModelAndView("feed").
              addObject("accounts", loggedInUserFilter.connectedAccountsFor(loggedInUser)).
              addObject("job", job).
              addObject("tweetEvents", twitterHistoryDAO.getTweetEvents(job.getFeed(), job.getAccount())).
              addObject("activity", activity).
              addObject("feedItems", feedItems).
              addObject("feedItemsWithTweets", withTweets);

    } else {
      return redirectToSignInPage();
    }
  }

  private ModelAndView renderNewFeedForm(FeedDetails feedDetails, Account account) {
    return new ModelAndView("newfeed").
            addObject("feedDetails", feedDetails).
            addObject("accounts", loggedInUserFilter.connectedAccountsFor(account));
  }

  private ModelAndView redirectToSignInPage() {
    return new ModelAndView(new RedirectView("/"));
  }

}
