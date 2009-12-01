/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySpace.java
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

package playground.jhackney.activitySpaces;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class ActivitySpace {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected static final int DEBUG_LEVEL = 1;

	protected String act_type = null;
	protected TreeMap<String, Double> params = new TreeMap<String,Double>();

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	protected abstract void addParam(final String name, final Double value);
	public abstract void addParam(final String name, final String value);

	public void addParams(final TreeMap<String,Double> params) {
		Set<Map.Entry<String, Double>> entries = params.entrySet();
		Iterator<Map.Entry<String, Double>> iter = entries.iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Double> entry = iter.next();
			addParam(entry.getKey(), entry.getValue());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getActType() {
		return this.act_type;
	}

	public final Double getParam(final String name) {
		return this.params.get(name);
	}

	public final TreeMap<String,Double> getParams() {
		return this.params;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	public final boolean isComplete() {
		if (this.params.containsValue(null)) {
			return false;
		}
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[act_type="+this.act_type+"]"+
			"[nof_values="+this.params.size()+"]";
	}
}
