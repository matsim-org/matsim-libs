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

package org.matsim.core.utils.misc;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OptionalTimes {
	@CanIgnoreReturnValue
	public static OptionalTime requireDefined(OptionalTime time) {
		Preconditions.checkArgument(time.isDefined(), "Time must be defined");
		return time;
	}
	
	public static OptionalTime add(OptionalTime time1, OptionalTime time2) {
		if (time1.isUndefined() || time2.isUndefined()) {
			return OptionalTime.undefined();
		} else {
			return OptionalTime.defined(time1.seconds() + time2.seconds());
		}
	}
	
	public static OptionalTime subtract(OptionalTime time1, OptionalTime time2) {
		if (time1.isUndefined() || time2.isUndefined()) {
			return OptionalTime.undefined();
		} else {
			return OptionalTime.defined(time1.seconds() - time2.seconds());
		}
	}
}
