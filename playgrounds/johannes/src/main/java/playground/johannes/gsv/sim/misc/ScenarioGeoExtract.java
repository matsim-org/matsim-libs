/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.misc;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetgen.util.MatsimCoordUtils;

/**
 * @author johannes
 *
 */
public class ScenarioGeoExtract {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private static Population extractPopulation(Population all, Population sub, Geometry geo, Network network) {
		PreparedGeometry prepGeo = PreparedGeometryFactory.prepare(geo);
		
		for(Person person : all.getPersons().values()) {
			boolean keep = false;
			
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					Link link = network.getLinks().get(act.getLinkId());
					if(link != null) {
						Point p1 = MatsimCoordUtils.coordToPoint(link.getFromNode().getCoord());
						Point p2 = MatsimCoordUtils.coordToPoint(link.getToNode().getCoord());
						
						if(prepGeo.contains(p1) || prepGeo.contains(p2)) {
							keep = true;
							break;
						}
					}
				}
				
				if(keep) break;
			}
			
			if(keep) {
				sub.addPerson(person);
			}
		}
		
		return sub;
	}
}
