/* *********************************************************************** *
 * project: org.matsim.*
 * LinguisticHistogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;

/**
 * @author illenberger
 *
 */
public class LinguisticHistogram {

	public static TObjectDoubleHashMap<String> create(Collection<String> values) {
		TObjectDoubleHashMap<String> hist = new TObjectDoubleHashMap<String>();
		for(String value : values) {
			hist.adjustOrPutValue(value, 1.0, 1.0);
		}
		return hist;
	}
}
