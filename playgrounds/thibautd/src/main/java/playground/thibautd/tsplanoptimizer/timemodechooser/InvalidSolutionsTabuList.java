/* *********************************************************************** *
 * project: org.matsim.*
 * InvalidSolutionsTabuList.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;
import playground.thibautd.tsplanoptimizer.framework.Value;

/**
 * @author thibautd
 */
public class InvalidSolutionsTabuList implements TabuChecker {

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
		Solution result = move.apply( solution );

		int now = 0;
		for (Value val : result.getRepresentation()) {
			Integer value = (Integer) val.getValue();

			if (value < now) {
				return true;
			}

			now = value;
		}

		return false;
	}
}

