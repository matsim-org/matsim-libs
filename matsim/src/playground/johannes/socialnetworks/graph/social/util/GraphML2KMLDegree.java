/* *********************************************************************** *
 * project: org.matsim.*
 * GraphML2KML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.social.util;

import java.io.IOException;

import org.matsim.core.api.population.Person;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.io.SNGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.io.KMLDegreeStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;

/**
 * @author illenberger
 *
 */
public class GraphML2KMLDegree {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SocialNetwork<Person> socialnet = SNGraphMLReader.loadFromConfig(args[0], args[1]);
		
		KMLWriter writer = new KMLWriter();
		KMLDegreeStyle vertexStyle = new KMLDegreeStyle(writer.getVertexIconLink());
		vertexStyle.setLogscale(false);
		writer.setVertexStyle(vertexStyle);
		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
		writer.write(socialnet, args[2]);
	}
}
