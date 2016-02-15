/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.invermo.sim;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import playground.johannes.gsv.synPop.invermo.InvermoKeys;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.TaskRunner;

import java.util.Set;

/**
 * @author johannes
 *
 */
public class InitializeTargetDistance implements EpisodeTask {

	private final GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
	
	private final DistanceCalculator dCalc = OrthodromicDistanceCalculator.getInstance();
	
//	private final double detourFactor;
	
	public InitializeTargetDistance() {
//		this.detourFactor = detourFactor;
	}
	
	@Override
	public void apply(Episode plan) {
		for(int i = 0; i < plan.getLegs().size(); i++) {
			Attributable prev = plan.getActivities().get(i);
			Attributable leg = plan.getLegs().get(i);
			Attributable next = plan.getActivities().get(i + 1);
			
			String sourceStr = prev.getAttribute(InvermoKeys.COORD);
			String destStr = next.getAttribute(InvermoKeys.COORD);
			
			if(sourceStr != null && destStr != null) {
				Point source = string2Coord(sourceStr);
				Point dest = string2Coord(destStr);
				
				double d = dCalc.distance(source, dest);
//				d = d * detourFactor;
//				d = d * TargetDistanceHamiltonian.calcDetourFactor(d);
				
				leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(d));
			}
		}

	}
	
	private Point string2Coord(String str) {
		String tokens[] = str.split(",");
		double x = Double.parseDouble(tokens[0]);
		double y = Double.parseDouble(tokens[1]);

		Point p = factory.createPoint(new Coordinate(x, y));
		p.setSRID(4326);
		return p;
	}

	
	public static void main(String args[]) {
		Logger logger = Logger.getLogger(InitializeTargetDistance.class);
		
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
	
		parser.addToBlacklist("workLoc");
		parser.addToBlacklist("homeLoc");
		parser.addToBlacklist("homeCoord");
		parser.addToBlacklist("location");
		parser.addToBlacklist("state");
		parser.addToBlacklist("inhabClass");
		parser.addToBlacklist("index");
		parser.addToBlacklist("roundTrip");
		parser.addToBlacklist("origin");
		parser.addToBlacklist("purpose");
		parser.addToBlacklist("delete");
		
		logger.info("Loading persons...");
		parser.parse("/home/johannes/gsv/invermo/5.pop.xml");
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
//		TaskRunner.run(new InitializeTargetDistance(TargetDistanceHamiltonian.DEFAULT_DETOUR_FACTOR), persons);
		TaskRunner.run(new InitializeTargetDistance(), persons);
		
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/invermo/5.pop.dist.xml", persons);
	}
}
