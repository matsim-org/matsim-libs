/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.zhCity.scenario;

import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PopulationWriter;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class SelectCityAgents {

	public static void main(String[] args) {
		// String inputPlansFile = "C:/eTmp/census2000v2_ZhCut10km_5pct.xml.gz";
		String inputPlansFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_plans.xml.gz";
		String inputNetworkFile = "H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String inputFacilities = "H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";

		// 8km=max. radius zh city= study area
		int scenarioAreaRadius = 8000;
		
		double outputFraction = 1.0;
		
		String outputPlansFile = "C:/eTmp/census2000v2_8kmCut_10pct_no_network_info.xml.gz";

		Scenario scenario = GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);

		Coord coordinatesQuaiBridgeZH = ParkingHerbieControler.getCoordinatesQuaiBridgeZH();

		HashSet<Id> filterOutPerson = new HashSet();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			boolean filterOut = true;
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					
					if (GeneralLib.getDistance(coordinatesQuaiBridgeZH, act.getCoord()) < scenarioAreaRadius) {
						filterOut = false;
						break;
					}
				}
			}
			if (filterOut) {
				filterOutPerson.add(person.getId());
			}
		}

		for (Id personId : filterOutPerson) {
			scenario.getPopulation().getPersons().remove(personId);
		}

		// remove pt route from plans
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if (leg.getMode().equals(TransportMode.pt)) {
							leg.setRoute(null);
						}
					}
				}
			}
		}

		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), outputFraction).write(outputPlansFile);
	}

}
