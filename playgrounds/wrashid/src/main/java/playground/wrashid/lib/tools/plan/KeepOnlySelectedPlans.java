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
package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class KeepOnlySelectedPlans {

	public static void main(String[] args) {
		String inputPlansFile = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/v-temp/for ESD/july-2014/input/run.2010.output_plans.xml.gz";
		String inputNetworkFile = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/v-temp/for ESD/july-2014/input/multimodalNetwork2010final.xml.gz";
		String inputFacilities = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/v-temp/for ESD/july-2014/input/run.2010.output_facilities.xml.gz";
		String outputPlansFile = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/v-temp/for ESD/july-2014/output/onlySelectedPlans_run.2010.output_plans.xml.gz";
		
		// String inputPlansFile =
		// "I:/MATSim2030/03_output/01_Delivered/01_run2010baselineDelivered/run.2010.output_plans.xml.gz";
		// String inputNetworkFile =
		// "I:/MATSim2030/02_input/02_runs/13_run2010baseline_Feb14_4/multimodalNetwork2010final.xml.gz";
		// String inputFacilities =
		// "I:/MATSim2030/03_output/01_Delivered/01_run2010baselineDelivered/run.2010.output_facilities.xml.gz";
	//	String outputPlansFile = "I:/data/for ESD/july-2014/onlySelectedPlans_run.2010.output_plans.xml";
		
		Scenario scenario = GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonImpl p = (PersonImpl) person;
			PersonUtils.removeUnselectedPlans(p);
		}

		GeneralLib.writePopulation(scenario.getPopulation(), scenario.getNetwork(), outputPlansFile);
	}
}
