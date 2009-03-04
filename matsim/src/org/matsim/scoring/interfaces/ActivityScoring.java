package org.matsim.scoring.interfaces;

import org.matsim.interfaces.core.v01.Act;

public interface ActivityScoring {

	public void startActivity(final double time, final Act act);

	public void endActivity(final double time);

}
