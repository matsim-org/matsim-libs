package playground.wrashid.scoring.interfaces;

import org.matsim.interfaces.core.v01.Leg;

public interface LegScoring {
	public abstract void startLeg(final double time, final Leg leg);

	public abstract void endLeg(final double time);
}
