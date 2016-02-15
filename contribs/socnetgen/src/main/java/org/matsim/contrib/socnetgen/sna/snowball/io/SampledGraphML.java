/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphML.java
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
package org.matsim.contrib.socnetgen.sna.snowball.io;

import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.core.utils.collections.Tuple;
import org.xml.sax.Attributes;

import java.util.List;

/**
 * Utility class to de- and encode snowball sampling information into GraphML data.
 * 
 * @author illenberger
 *
 */
public class SampledGraphML {

	public static final String DETECTED_ATTR = "detected";
	
	public static final String SAMPLED_ATTR = "sampled";

	/**
	 * Parses the attributes data for information of the detected state of the
	 * vertex and sets the vertex to the corresponding state.
	 * 
	 * @param v a sampled vertex.
	 * @param attrs the attributes data.
	 */
	public static void applyDetectedState(SampledVertex v, Attributes attrs) {
		String str = attrs.getValue(DETECTED_ATTR);
		if(str != null)
			v.detect(new Integer(str));
	}
	
	/**
	 * Parses the attributes data for information of the sampled state of the
	 * vertex and sets the vertex to the corresponding state.
	 * 
	 * @param v a sampled vertex.
	 * @param attrs the attributes data.
	 */
	public static void applySampledState(SampledVertex v, Attributes attrs) {
		String str = attrs.getValue(SAMPLED_ATTR);
		if(str != null)
			v.sample(new Integer(str));
	}

	/**
	 * Encodes the detected and sampled state of a vertex into attributes data.
	 * 
	 * @param v a sampled vertex.
	 * @param attributes the attributes data.
	 */
	public static void addSnowballAttributesData(SampledVertex v, List<Tuple<String, String>> attributes) {
		if(v.isDetected())
			attributes.add(new Tuple<String, String>(DETECTED_ATTR, String.valueOf(v.getIterationDetected())));
		if(v.isSampled())
			attributes.add(new Tuple<String, String>(SAMPLED_ATTR, String.valueOf(v.getIterationSampled())));
		
	}
}
