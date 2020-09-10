package com.mapbox.navigation.ui.feedback;

import com.mapbox.navigation.core.telemetry.events.CachedNavigationFeedbackEvent;

import java.util.List;

/**
 * Interface notified on interaction with the possible feedback opportunities
 * that the Navigation UI SDK provides.
 */
public interface FeedbackFlowListener {

  void onDetailedFeedbackFlowFinished(
          List<CachedNavigationFeedbackEvent> cachedNavigationFeedbackEvents
  );

  void onArrivalExperienceFeedbackFinished(FeedbackItem arrivalFeedbackItem);
}
