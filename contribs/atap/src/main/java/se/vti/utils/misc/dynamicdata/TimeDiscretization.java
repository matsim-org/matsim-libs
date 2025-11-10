/**
 * se.vti.utils
 * 
 * Copyright (C) 2015-2025 by Gunnar Flötteröd (VTI, LiU).
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

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeDiscretization {

	private final int startTime_s;

	private final int binSize_s;

	private final int binCnt;

	public TimeDiscretization(final int startTime_s, final int binSize_s, final int binCnt) {
		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.binCnt = binCnt;
	}

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getBinSize_s() {
		return this.binSize_s;
	}

	public int getBinCnt() {
		return this.binCnt;
	}

	// TODO NEW
	public int getBin(final double time_s) {
		return (int) ((time_s - this.startTime_s) / this.binSize_s);
	}

	// TODO NEW
	public int getBinStartTime_s(final int bin) {
		return this.startTime_s + bin * this.binSize_s;
	}

	// TODO NEW
	public int getBinCenterTime_s(final int bin) {
		return this.getBinStartTime_s(bin) + this.binSize_s / 2;
	}

	// TODO NEW
	public int getBinEndTime_s(final int bin) {
		return this.getBinStartTime_s(bin + 1);
	}

	// TODO NEW
	public int getEndTime_s() {
		return this.getBinStartTime_s(this.binCnt);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " start time = " + Time.strFromSec(this.startTime_s) + ", bin size = "
				+ Time.strFromSec(this.binSize_s) + ", number of bins = " + this.binCnt + ".";
	}

}
