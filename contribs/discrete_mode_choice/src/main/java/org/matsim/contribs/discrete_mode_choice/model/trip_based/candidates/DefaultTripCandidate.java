package org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates;

/**
 * Default implementation for a TripCandidate.
 *
 * @author sebhoerl
 */
public class DefaultTripCandidate implements TripCandidate {
  private final double utility;
  private final String mode;
  private final double duration;

  public DefaultTripCandidate(double utility, String mode, double duration) {
    this.utility = utility;
    this.mode = mode;
    this.duration = duration;
  }

  @Override
  public double getUtility() {
    return utility;
  }

  @Override
  public String getMode() {
    return mode;
  }

  @Override
  public double getDuration() {
    return duration;
  }
}
