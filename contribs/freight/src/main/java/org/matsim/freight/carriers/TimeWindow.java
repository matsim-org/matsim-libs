/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;


import org.matsim.core.utils.misc.Time;

/**
 * Q: What happens/should happen if the time window is not sufficient to unload, or
 * the vehicle arrives after the time window?
 * <p></p>
 * A: First, I (=sschroeder) interpret time-window as period of time when an
 * (un)loading operation can start (which does not mean that it has to be
 * finished by the end of the time window). This is - I guess - in line with
 * most OR literature dealing with time-windows. There are 3 options to deal
 * with late arrivals:<ol>
 * <li> agent does not (un)load and immediately goes to the
 * next activity. missed operation is then penalized in the scoring part (which
 * is - I think - far from reality)
 * <li> agent (un)loads at arr_time and late
 * arrTime is penalyzed in the scoring part
 * <li> agent gets an extra waiting_time
 * before (un)loading. optionally, late arr_time is then penalyzed in the
 * scoring part additionally.
 * </ol>
 * I guess that it depends on the freight sector. In last mile of the CEP market
 * (parcels), I'd prefer option 2. When it comes to long haul transport, e.g.
 * delivering Bosch with (raw) material, option 3 might be better. Here, I know
 * that carriers need to book time-windows. If they are in time, they can unload
 * immediately. If not, they need to wait for the next available time slot which
 * might be 3hours later. I think both options (2,3) might be (easily)
 * incorporated with WithinDayReplanning(ReScheduling)-Module.
 * <p></p>
 *
 * @author (of code) sschroeder (of docu) kai based on sschroeder
 */

public class TimeWindow {

	public static TimeWindow newInstance(double start, double end) {
		return new TimeWindow(start, end);
	}

	private final double start;

	private final double end;

	private TimeWindow(final double start, final double end) {
		this.start = start;
		this.end = end;
	}

	public double getStart() {
		return start;
	}

	public double getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "[start=" + Time.writeTime(start) + ", end=" + Time.writeTime(end) + "]";
	}

	@Override
	public boolean equals(Object o){
		if(o == this) return true;
		if((o == null) || (o.getClass() != this.getClass())) {
			return false;
		}
		TimeWindow t1 = (TimeWindow) o;
		return (t1.getStart() == this.getStart() && t1.getEnd() == this.getEnd());
	}

	@Override
	public int hashCode(){
		int result = 59;
		int startHash = Double.hashCode(start);
		result = 31 * result + startHash;
		int endHash = Double.hashCode(end);
		result = 31 * result + endHash;
		return result;
	}

}
