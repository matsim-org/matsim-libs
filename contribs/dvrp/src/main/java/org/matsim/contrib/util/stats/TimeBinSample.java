/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.util.stats;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class TimeBinSample<V> {
	public final int timeBin;
	public final V value;

	public TimeBinSample(int timeBin, V value) {
		this.timeBin = timeBin;
		this.value = value;
	}

	public int timeBin() {
		return timeBin;
	}

	public V value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TimeBinSample))
			return false;
		TimeBinSample<?> sample = (TimeBinSample<?>)o;
		return timeBin == sample.timeBin && Objects.equal(value, sample.value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(timeBin, value);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("timeBin", timeBin).add("value", value).toString();
	}
}
