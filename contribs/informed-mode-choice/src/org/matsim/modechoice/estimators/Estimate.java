package org.matsim.modechoice.estimators;

public final class Estimate {

	private final double duration;
	private final double utility;


	public Estimate(double duration, double utility) {
		this.duration = duration;
		this.utility = utility;
	}

	public double getDuration() {
		return duration;
	}

	public double getUtility() {
		return utility;
	}
}
