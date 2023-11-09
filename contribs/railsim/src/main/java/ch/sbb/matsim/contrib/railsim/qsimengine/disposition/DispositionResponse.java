package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

/**
 * Response from {@link TrainDisposition}.
 */
public record DispositionResponse(double reservedDist, Detour detour) {

}
