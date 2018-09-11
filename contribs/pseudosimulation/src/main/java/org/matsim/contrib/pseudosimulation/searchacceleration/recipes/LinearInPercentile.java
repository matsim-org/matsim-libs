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
package org.matsim.contrib.pseudosimulation.searchacceleration.recipes;

import java.util.List;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinearInPercentile {

	// NEW, USE FOR NOW ONLY FOR BOOKKEEPING

	// OLD STUFF
	
	public static final double MAX_PERCENTILE = 99.0;

	private Double percentile = null;

	private double delta = 0;

	public LinearInPercentile() {
	}

	public Double getPercentile() {
		return this.percentile;
	}

	public double getDelta() {
		return this.delta;
	}

	public void update(final double lastExperiencedUtilityChange, final double lastUniformExpectedUtilityChange,
			final double lastAcceleratedExpectedUtilityChange, final List<Double> lastCriticalDeltas) {

		if (this.percentile == null) {
			int percentileIndex = 0;
			while ((lastCriticalDeltas.get(percentileIndex) < 0.0)
					&& (percentileIndex + 1 < lastCriticalDeltas.size())) {
				percentileIndex++;
			}
			this.percentile = Math.min(MAX_PERCENTILE, percentileIndex * 100.0 / lastCriticalDeltas.size());
		}

		final double unboundedPercentile;
		if (lastExperiencedUtilityChange <= 1e-8) {
			unboundedPercentile = MAX_PERCENTILE;
		} else {
			unboundedPercentile = this.percentile + (100.0 - this.percentile) * (1.0 - lastExperiencedUtilityChange
					/ (lastUniformExpectedUtilityChange - lastAcceleratedExpectedUtilityChange));
		}
		this.percentile = Math.max(0, Math.min(unboundedPercentile, MAX_PERCENTILE));

		final int index = Math.max(0, Math.min(lastCriticalDeltas.size() - 1,
				(int) (unboundedPercentile * lastCriticalDeltas.size() / 100.0)));
		this.delta = lastCriticalDeltas.get(index);
	}
}
