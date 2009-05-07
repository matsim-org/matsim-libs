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
package playground.johannes.socialnet.util;

import java.io.IOException;

import org.matsim.core.api.population.Person;

import playground.johannes.graph.io.PajekClusteringColorizer;
import playground.johannes.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.socialnet.io.SNGraphMLReader;
import playground.johannes.socialnet.io.SNPajekWriter;

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
		SocialNetwork<Person> socialNet = SNGraphMLReader.loadFromConfig(args[0], args[1]);
		
		PajekDegreeColorizer<Ego<Person>, SocialTie> colorizer1 = new PajekDegreeColorizer<Ego<Person>, SocialTie>(socialNet, false);
		PajekClusteringColorizer<Ego<Person>, SocialTie> colorizer2 = new PajekClusteringColorizer<Ego<Person>, SocialTie>(socialNet);
		SNPajekWriter<Person> pwriter = new SNPajekWriter<Person>();
		pwriter.write(socialNet, colorizer1, args[2] + "socialnet.degree.net");
		pwriter.write(socialNet, colorizer2, args[2] + "socialnet.clustering.net");
	}

}
