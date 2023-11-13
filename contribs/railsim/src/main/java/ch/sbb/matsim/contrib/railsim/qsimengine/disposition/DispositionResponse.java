package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

/**
 * Response from {@link TrainDisposition}.
 * @param approvedDist distance cleared for the train
 * @param approvedSpeed speed cleared after approved distance, if 0 the train needs to stop.
 * @param detour detour to be taken, if any.
 */
public record DispositionResponse(double approvedDist, double approvedSpeed, Detour detour) {

}
