package org.matsim.utils.eventsfilecomparison;

/**
 * Result of event file comparison.
 */
public enum ComparisonResult {FILES_ARE_EQUAL, DIFFERENT_NUMBER_OF_TIMESTEPS, DIFFERENT_TIMESTEPS, DIFFERENT_EVENT_ATTRIBUTES, MISSING_EVENT, WRONG_EVENT_COUNT, FILE_ERROR}
