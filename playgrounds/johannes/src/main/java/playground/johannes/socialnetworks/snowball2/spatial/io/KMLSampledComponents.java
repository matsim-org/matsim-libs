/* *********************************************************************** *
 * project: org.matsim.*
 * KMLSampledComponents.java
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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import gnu.trove.TIntIntHashMap;

import java.util.Arrays;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.spatial.io.KMLComponents;

/**
 * @author illenberger
 *
 */
public class KMLSampledComponents extends KMLComponents {

	@Override
	public void addDetail(FolderType kmlFolder,	Set<? extends SpatialVertex> partition) {
		TIntIntHashMap detected = new TIntIntHashMap();
		TIntIntHashMap sampled = new TIntIntHashMap();
		for(Vertex v : partition) {
			sampled.adjustOrPutValue(((SampledVertex)v).getIterationSampled(), 1, 1);
			detected.adjustOrPutValue(((SampledVertex)v).getIterationDetected(), 1, 1);
		}
		
		int keys[] = sampled.keys();
		Arrays.sort(keys);
		StringBuilder builder = new StringBuilder();
		builder.append("<table><tr><th>Iteration</th><th>Sampled</th><th>Detected</th></tr>");
		for(int key : keys) {
			builder.append("<tr><td>");
			builder.append(String.valueOf(key));
			builder.append("</td><td>");
			builder.append(String.valueOf(sampled.get(key)));
			builder.append("</td><td>");
			builder.append(String.valueOf(detected.get(key)));
			builder.append("</td></tr>");
		}
		builder.append("</table>");
		
		kmlFolder.setDescription(builder.toString());
		kmlFolder.setName(String.format("n=%1$s, seeds=%2$s", partition.size(), sampled.get(0)));
	}

}
