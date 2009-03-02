package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Act;

public interface ActivityScoringFunction {

	public void startActivity(final double time, final Act act);

	public void endActivity(final double time);

}
