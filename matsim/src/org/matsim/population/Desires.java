/* *********************************************************************** *
 * project: org.matsim.*
 * Knowledge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.population;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.utils.misc.Time;

public class Desires {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Desires.class);

	private String desc = null;
	private Map<String,Double> act_durs = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Desires(final String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// put methods
	//////////////////////////////////////////////////////////////////////
	
	public final boolean putActivityDuration(final String act_type, final double duration) {
		if (duration <= 0.0) { log.fatal("duration=" + duration + " not allowed!"); return false; }
		if (this.act_durs == null) { this.act_durs = new HashMap<String, Double>(); }
		this.act_durs.put(act_type,duration);
		return true;
	}
	
	protected final boolean putActivityDuration(final String act_type, final String duration) {
		double dur = Time.parseTime(duration);
		return this.putActivityDuration(act_type,dur);
	}
	
	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	public final boolean removeActivityDuration(final String act_type) {
		if (this.act_durs.remove(act_type) == null) { return false; }
		if (this.act_durs.isEmpty()) { this.act_durs = null; }
		return true;
	}
	
	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public final String getDesc() {
		return this.desc;
	}
	
	public final double getActivityDuration(final String act_type) {
		if (this.act_durs == null) { return Time.UNDEFINED_TIME; }
		Double d = this.act_durs.get(act_type);
		if (d == null) { return Time.UNDEFINED_TIME; }
		return d;
	}
	
	protected final Map<String,Double> getActivityDurations() {
		return this.act_durs;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[desc=" + this.desc + "]" + "[nof_act_durs=" + this.act_durs.size() + "]";
	}
}
