/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.sharedTaxiBerlin.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TaxiTrips2CSV {

	static double maxY = 5823460;
	static double minY = 5814023;
	static double minX = 388558;
	static double maxX = 396548;
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("Y:/bvg-taxi/old-taxidata-comparison/berlin_brb.xml.gz");
//		new PopulationReader(scenario).readFile("Y:/bvg-taxi/old-taxidata-comparison/OD_20130420_SCALE_1.0_plans.xml.gz");
		new PopulationReader(scenario).readFile("Y:/bvg-taxi/old-taxidata-comparison/OD_20130421_SCALE_1.0_plans.xml.gz");
		List<String> trips = new ArrayList<>();
		Random r = MatsimRandom.getRandom();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, "EPSG:25833");
		for (Person p : scenario.getPopulation().getPersons().values())
		{
			Plan plan  = p.getSelectedPlan();
			Activity act0 = (Activity) plan.getPlanElements().get(0);
			Coord coord0 = ParkingUtils.getRandomPointAlongLink(r, scenario.getNetwork().getLinks().get(act0.getLinkId())); 
			Activity act1 = (Activity) plan.getPlanElements().get(2);
			Coord coord1 = ParkingUtils.getRandomPointAlongLink(r, scenario.getNetwork().getLinks().get(act1.getLinkId())); 
			Trip trip = new Trip();
			trip.departureTime=act0.getEndTime();
			trip.fromCoord = ct.transform(coord0);
			trip.toCoord = ct.transform(coord1);
			String[] agentId = p.getId().toString().split("_");
			trip.fromLoc = agentId[0];
			trip.toLoc = agentId[1];
			if (isValidTrip(trip))
			trips.add(trip.toString());
			
		}
		
//        JbUtils.collection2Text(trips,"Y:/bvg-taxi/old-taxidata-comparison/taxitripsSat.csv", "time;fromLOR;toLOR;fromX;fromY;toX;toY");
        JbUtils.collection2Text(trips,"Y:/bvg-taxi/old-taxidata-comparison/taxitripsSun.csv", "time;fromLOR;toLOR;fromX;fromY;toX;toY");

	}

	/**
	 * @param trip
	 * @return
	 */
	private static boolean isValidTrip(Trip trip) {
	
//		if ((trip.departureTime>18*3600)&&(trip.fromCoord.getX()>minX&&trip.fromCoord.getX()<maxX&trip.fromCoord.getY()>minY&&trip.fromCoord.getY()<maxY)&&(trip.toCoord.getX()>minX&&trip.toCoord.getX()<maxX&trip.toCoord.getY()>minY&&trip.toCoord.getY()<maxY))
		if ((trip.departureTime<5*3600)&&(trip.fromCoord.getX()>minX&&trip.fromCoord.getX()<maxX&trip.fromCoord.getY()>minY&&trip.fromCoord.getY()<maxY)&&(trip.toCoord.getX()>minX&&trip.toCoord.getX()<maxX&trip.toCoord.getY()>minY&&trip.toCoord.getY()<maxY))
		return true;
			else return false;
	}

}
