/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mrieser.core.sim.features;

import org.matsim.core.mobsim.framework.Steppable;

public interface SimFeature extends Steppable {

	/**
	 * Specifies if this SimFeature has still work to do and wishes to continue the
	 * simulation, or if the simulation could be stopped from the point of view of
	 * this SimFeature. Returning <code>true</code> does not force the simulation
	 * to shut down, usually that only happens once all SimFeatures returned
	 * <code>true</code>, so the SimFeature has to continue to work correctly no
	 * matter what it returns. Most SimFeatures that only have to correctly setup
	 * some data structures as long as the simulation is running may simply return
	 * <code>true</code> all the time.
	 *
	 * @return <code>true</code> if this SimFeature has nothing left to do,
	 * 		<code>false</code> otherwise.
	 */
	public boolean isFinished();

}
