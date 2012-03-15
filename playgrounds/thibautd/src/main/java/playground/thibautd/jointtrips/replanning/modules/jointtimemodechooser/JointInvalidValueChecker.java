/* *********************************************************************** *
 * project: org.matsim.*
 * JointInvalidValueChecker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser;

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;
import playground.thibautd.tsplanoptimizer.framework.Value;
import playground.thibautd.tsplanoptimizer.timemodechooser.IntegerValueChanger;

/**
 * @author thibautd
 */
public class JointInvalidValueChecker implements TabuChecker {

	@Override
	public void notifyMove(
			final Solution solution,
			final Move move,
			final double newScore) {
		// nothing to do
	}

	@Override
	public boolean isTabu(
			final Solution solution,
			final Move move) {
		if (move instanceof IntegerValueChanger) {
			Solution result = move.apply( solution );

			for (Value val : result.getRepresentation()) {
				Object value = val.getValue();

				if (value instanceof Integer) {
					if (((Integer) value) < 0) {
						return true;
					}
				}
			}
		}

		return false;
	}
}

