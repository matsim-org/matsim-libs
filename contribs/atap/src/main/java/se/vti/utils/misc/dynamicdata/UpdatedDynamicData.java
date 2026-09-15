/**
 * se.vti.utils
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.utils.misc.dynamicdata;

import java.util.Map;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
@SuppressWarnings("serial")
public class UpdatedDynamicData<K extends Object> extends DynamicData<K> {

	public UpdatedDynamicData(TimeDiscretization timeDiscretization) {
		super(timeDiscretization);
	}

	public UpdatedDynamicData(int startTime_s, int binSize_s, int binCnt) {
		super(startTime_s, binSize_s, binCnt);
	}

	public UpdatedDynamicData(final UpdatedDynamicData<K> parent) {
		super(parent.getStartTime_s(), parent.getBinSize_s(), parent.getBinCnt());
		this.add(parent, 1.0); // ensures deep copy of double[] data holding arrays
	}

	public boolean timeCompatible(final UpdatedDynamicData<K> other) {
		return ((this.getStartTime_s() == other.getStartTime_s()) && (this.getBinCnt() == other.getBinCnt())
				&& (this.getBinSize_s() == other.getBinSize_s()));
	}

	public void checkTimeCompatibility(final UpdatedDynamicData<K> other) {
		if (!this.timeCompatible(other)) {
			throw new RuntimeException("incompatible time dimensions");
		}
	}

	public void multiply(final double factor) {
		for (double[] series : this.data.values()) {
			for (int i = 0; i < series.length; i++) {
				series[i] *= factor;
			}
		}
	}

	public void add(final UpdatedDynamicData<K> other, final double otherFactor) {
		this.checkTimeCompatibility(other);
		for (Map.Entry<K, double[]> otherEntry : other.data.entrySet()) {
			final double[] otherSeries = otherEntry.getValue();
			final double[] mySeries = this.getNonNullDataArray(otherEntry.getKey());
			for (int i = 0; i < mySeries.length; i++) {
				mySeries[i] += otherFactor * otherSeries[i];
			}
		}
	}
}