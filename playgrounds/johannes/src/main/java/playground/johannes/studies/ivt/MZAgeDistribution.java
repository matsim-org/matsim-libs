/* *********************************************************************** *
 * project: org.matsim.*
 * MZAgeDistribution.java
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
package playground.johannes.studies.ivt;

import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.AgeAccessibilityTask;
import playground.johannes.socialnetworks.graph.social.io.Population2SocialGraph;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.graph.spatial.analysis.GridAccessibility;

import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class MZAgeDistribution {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Population2SocialGraph reader = new Population2SocialGraph();
		SocialGraph graph = reader.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.01.xml", CRSUtils.getCRS(21781));
		
		Accessibility access = new GridAccessibility(new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator()), 1500);
		GraphAnalyzer.analyze(graph, new AgeAccessibilityTask(access), "/Users/jillenberger/Work/phd/doc/tex/ch5/fig/data/");
	}

}
