package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

/**
 * Response from {@link TrainDisposition}.
 * @param approvedDist distance cleared for the train.
 * @param stopSignal  if true, the train should stop after remaining approved dist.
 * @param detour detour to be taken, if any.
 */
public record DispositionResponse(double approvedDist, boolean stopSignal, Detour detour) {

}
