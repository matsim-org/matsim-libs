/*
 * Copyright 2015 Gunnar Flötteröd
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
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import floetteroed.utilities.DynamicData;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class RenderableDynamicData<K> extends DynamicData<K> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private int bin = 0;

	// -------------------- CONSTRUCTION --------------------

	public RenderableDynamicData(int startTime_s, int binSize_s, int binCnt) {
		super(startTime_s, binSize_s, binCnt);
		this.bin = 0;
	}

	public RenderableDynamicData(final DynamicData<K> data) {
		this(data.getStartTime_s(), data.getBinSize_s(), data.getBinCnt());
		for (K key : data.keySet()) {
			for (int bin = 0; bin < data.getBinCnt(); bin++) {
				this.put(key, bin, data.getBinValue(key, bin));
			}
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	int currentBin() {
		return this.bin;
	}

	void toStart() {
		this.bin = 0;
	}

	void toEnd() {
		this.bin = this.getBinCnt() - 1;
	}

	void toTime(final int time_s) {
		this.bin = bin(time_s);
		if (this.bin < 0) {
			this.bin = 0;
		} else if (this.bin >= this.getBinCnt()) {
			this.bin = this.getBinCnt() - 1;
		}
	}

	void fwd() {
		this.bin++;
		if (this.bin >= this.getBinCnt()) {
			this.bin = 0;
		}
	}

	void bwd() {
		this.bin--;
		if (this.bin < 0) {
			this.bin = this.getBinCnt() - 1;
		}
	}

	double getCurrentValue(final K key) {
		return this.getBinValue(key, this.bin);
	}
}
