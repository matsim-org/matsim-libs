/* *********************************************************************** *
 * project: org.matsim.*
 * SN2PajekDegree.java
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

import playground.johannes.socialnetworks.graph.io.PajekClusteringColorizer;
import playground.johannes.socialnetworks.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnetworks.graph.social.io.SNGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.io.PajekDistanceColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;
import playground.johannes.socialnetworks.sim.SimSocialEdge;
import playground.johannes.socialnetworks.sim.SimSocialGraph;
import playground.johannes.socialnetworks.sim.SimSocialVertex;

/**
 * @author illenberger
 *
 */
public class GraphML2PajekDegree {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SimSocialGraph socialNet = SNGraphMLReader.loadFromConfig(args[0], args[1]);
		
		PajekDegreeColorizer<SimSocialVertex, SimSocialEdge> colorizer1 = new PajekDegreeColorizer<SimSocialVertex, SimSocialEdge>(socialNet, false);
		PajekClusteringColorizer<SimSocialVertex, SimSocialEdge> colorizer2 = new PajekClusteringColorizer<SimSocialVertex, SimSocialEdge>(socialNet);
		PajekDistanceColorizer colorizer3 = new PajekDistanceColorizer(socialNet, false);
		SpatialPajekWriter pwriter = new SpatialPajekWriter();
		pwriter.write(socialNet, colorizer1, args[2] + "socialnet.degree.net");
		pwriter.write(socialNet, colorizer2, args[2] + "socialnet.clustering.net");
		pwriter.write(socialNet, colorizer3, args[2] + "socialnet.distance.net");
	}

}
