package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Leg;

public interface LegScoringFunction {
	public abstract void startLeg(final double time, final Leg leg);

	public abstract void endLeg(final double time);
}
