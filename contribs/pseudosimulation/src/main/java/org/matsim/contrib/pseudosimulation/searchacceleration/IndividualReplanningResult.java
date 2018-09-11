/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class IndividualReplanningResult implements Comparable<IndividualReplanningResult> {

	final double criticalDelta;
	final double expectedScoreChange;
	final boolean isActualReplanner;
	final boolean wouldBeUniformReplanner;
	final boolean wouldBeGreedyReplanner;

	IndividualReplanningResult(final double deltaForUniformReplanning, final double expectedScoreChange,
			final boolean isActualReplanner, final boolean wouldBeUniformReplanner, final boolean wouldBeGreedyReplanner) {
		this.criticalDelta = deltaForUniformReplanning;
		this.expectedScoreChange = expectedScoreChange;
		this.isActualReplanner = isActualReplanner;
		this.wouldBeUniformReplanner = wouldBeUniformReplanner;
		this.wouldBeGreedyReplanner = wouldBeGreedyReplanner;
	}
	
	int getIsActualReplannerIndicator() {
		return (this.isActualReplanner ? 1 : 0);
	}
	
	int getWouldBeUniformReplannerIndicator() {
		return (this.wouldBeUniformReplanner ? 1 : 0);
	}

	@Override
	public int compareTo(final IndividualReplanningResult other) {
		return Double.compare(this.criticalDelta, other.criticalDelta);
	}

}
