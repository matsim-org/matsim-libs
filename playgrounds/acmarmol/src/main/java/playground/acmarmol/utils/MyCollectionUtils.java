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

package playground.acmarmol.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.StringUtils;

import playground.acmarmol.matsim2030.microcensus2010.MZ2010ToXmlFiles;

/**
 * @author mrieser
 */
public abstract class MyCollectionUtils {
	
	private final static Logger log = Logger.getLogger(MyCollectionUtils.class);

	public static final String idSetToString(final Set<Id> values) {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (Id id : values) {
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
	
	public static final String arrayToTabSeparatedString(final String[] values) {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (String mode : values) {
			if (!isFirst) {
				str.append("\t");
			}
			str.append(mode);
			isFirst = false;
		}
		return str.toString();
	}
	
	public static final String integerArrayToTabSeparatedString(final int[] values) {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (int mode : values) {
			if (!isFirst) {
				str.append("\t");
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
	
	
	public static final double sum(double[] values){
		
		double result = 0;
		   for (double value:values)
		     result += value;
		   return result;
		
		}

	public static final void printMap(Map mp) {
	    Iterator it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	
}


