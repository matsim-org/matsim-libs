/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.braunschweig;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.jbischoff.utils.JbUtils;
import java.util.Random;

/**
 * @author jbischoff
 *
 */

public class CreateTaxibusDemand {
	Geometry fromGeo;
	Scenario scenario;
	Id<Link> destinationLinkId = Id.createLinkId(52060);
	Random locationRandom = MatsimRandom.getLocalInstance();
	int persons = 50;
	private void run() {
		String input = "../../../shared-svn/projects/braunschweig/scenario/taxibus-example/input/";
		fromGeo = JbUtils.readShapeFileAndExtractGeometry(input+"taxibus-data/oring-hbf.shp", "ID").get("oring");
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input+"network.xml.gz");
		
		for (int i = 0; i<persons; i++ ){
			Coord home = getRandomPointInFeature(fromGeo);
			String type = "train08";
			Id<Person> pid = Id.createPersonId("p_"+i);
			double departureTime = 7.5*3600;
			if (i>persons/2){
				departureTime = 8.5;
				type = "train09";
			}
			scenario.getPopulation().addPerson(createAgent(pid, home, departureTime, type));
			
		}
		new PopulationWriter(scenario.getPopulation()).write(input+"population_"+persons+".xml");
		
		
	}
	
	Person createAgent(Id<Person> pId, Coord homeCoord, double outboundDepartureTime, String destActName){
		PopulationFactory fac = scenario.getPopulation().getFactory();
		Person p = fac.createPerson(pId);
		Plan plan = fac.createPlan();
		Activity act = fac.createActivityFromCoord("home", homeCoord);
		act.setEndTime(outboundDepartureTime);
		plan.addActivity(act);
		Leg leg = fac.createLeg("taxibus");
		plan.addLeg(leg);
		Activity work = fac.createActivityFromLinkId(destActName, destinationLinkId);
		plan.addActivity(work);
		p.addPlan(plan);
		return p;
	}

	public static void main(String[] args) {
		new CreateTaxibusDemand().run();
	}
	
	private Coord getRandomPointInFeature(Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + this.locationRandom.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + this.locationRandom.nextDouble()
					* (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return MGC.point2Coord(p);
	}
}
