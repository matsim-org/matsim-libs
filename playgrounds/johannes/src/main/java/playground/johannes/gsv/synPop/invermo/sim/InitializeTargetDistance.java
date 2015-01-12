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

import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.gsv.synPop.invermo.InvermoKeys;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.run.ProxyTaskRunner;
import playground.johannes.gsv.synPop.sim3.TargetDistanceHamiltonian;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class InitializeTargetDistance implements ProxyPlanTask {

	private final GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
	
	private final DistanceCalculator dCalc = OrthodromicDistanceCalculator.getInstance();
	
//	private final double detourFactor;
	
	public InitializeTargetDistance() {
//		this.detourFactor = detourFactor;
	}
	
	@Override
	public void apply(ProxyPlan plan) {
		for(int i = 0; i < plan.getLegs().size(); i++) {
			ProxyObject prev = plan.getActivities().get(i);
			ProxyObject leg = plan.getLegs().get(i);
			ProxyObject next = plan.getActivities().get(i + 1);
			
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
		
		XMLParser parser = new XMLParser();
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
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
//		ProxyTaskRunner.run(new InitializeTargetDistance(TargetDistanceHamiltonian.DEFAULT_DETOUR_FACTOR), persons);
		ProxyTaskRunner.run(new InitializeTargetDistance(), persons);
		
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/invermo/5.pop.dist.xml", persons);
	}
}
