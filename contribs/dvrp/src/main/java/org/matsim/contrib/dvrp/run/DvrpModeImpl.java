/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.run;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * This class is based on guava's NamedImpl.
 *
 * @author Michal Maciejewski (michalm)
 */
class DvrpModeImpl implements DvrpMode, Serializable {
	private final String value;

	DvrpModeImpl(String value) {
		this.value = checkNotNull(value, "value");
	}

	public String value() {
		return this.value;
	}

	public int hashCode() {
		// This is specified in java.lang.Annotation.
		return (127 * "value".hashCode()) ^ value.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof DvrpMode)) {
			return false;
		}

		DvrpMode other = (DvrpMode)o;
		return value.equals(other.value());
	}

	public String toString() {
		return "@" + DvrpMode.class.getName() + "(value=" + value + ")";
	}

	public Class<? extends Annotation> annotationType() {
		return DvrpMode.class;
	}

	private static final long serialVersionUID = 0;
}
