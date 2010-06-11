/* *********************************************************************** *
 * project: org.matsim.*
 * SeedColorizer.java
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
package playground.johannes.socialnetworks.snowball2.io;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;
import org.matsim.contrib.sna.graph.spatial.io.Colorizable;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SeedColorizer implements Colorizable {

	private Map<SampledVertex, Color> colorMap;
	
	public SeedColorizer(Set<? extends SampledVertex> seeds) {
		int nSeeds = Math.max(6, seeds.size() + 1);
		int i = 1;
		colorMap = new HashMap<SampledVertex, Color>();
		for(SampledVertex vertex : seeds) {
			Color c = ColorUtils.getGRBColor(i/(double)(nSeeds - i));
			colorMap.put(vertex, c);
			i++;
		}
		
	}
	@Override
	public Color getColor(Object object) {
		Color c = colorMap.get(((SampledVertex) object).getSeed());
		if(c == null)
			c = Color.BLACK;
		return c;
	}

}
