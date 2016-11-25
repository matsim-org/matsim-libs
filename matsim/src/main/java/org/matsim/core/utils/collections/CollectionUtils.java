/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.collections;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author mrieser
 */
public abstract class CollectionUtils {

	public static final <T> String idSetToString(final Set<Id<T>> values) {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (Id<?> id : values) {
			if (!isFirst) {
				str.append(',');
			}
			str.append(id.toString());
			isFirst = false;
		}
		return str.toString();
	}

	public static final String setToString(final Set<String> values) {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (String s : values) {
			if (!isFirst) {
				str.append(',');
			}
			str.append(s);
			isFirst = false;
		}
		return str.toString();
	}

	public static final String arrayToString(final String[] values) {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (String mode : values) {
			if (!isFirst) {
				str.append(',');
			}
			str.append(mode);
			isFirst = false;
		}
		return str.toString();
	}

	public static final String[] stringToArray(final String values) {
		Set<String> tmp = stringToSet(values);
		return tmp.toArray(new String[tmp.size()]);
	}

	public static final Set<String> stringToSet(final String values) {
		if (values == null) {
			return Collections.emptySet();
		}
		String[] parts = StringUtils.explode(values, ',');
		Set<String> tmp = new LinkedHashSet<String>();
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				tmp.add(trimmed.intern());
			}
		}
		return tmp;
	}

	public static final Set<String> stringArrayToSet(final String [] array) {
		if (array == null) {
			return Collections.emptySet();
		}
		Set<String> tmp = new LinkedHashSet<>();
		for (String part : array) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				tmp.add(trimmed.intern());
			}
		}
		return tmp;
	}
}
