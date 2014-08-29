/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.coopsim.analysis;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author johannes
 *
 */
public class ArrivalLoadTask extends TransitionLoadTask {

	public ArrivalLoadTask() {
		super("t_arr");
	}

	@Override
	protected double getTime(Trajectory t, int idx) {
		return t.getTransitions().get(idx + 1);
	}

}
