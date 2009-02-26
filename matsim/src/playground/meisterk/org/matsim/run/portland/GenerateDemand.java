/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateDemand.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.run.portland;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

public class GenerateDemand {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		Gbl.createConfig(args);
		GenerateDemand.generateDemand();

	}

	private static void generateDemand() {

//		World world = Gbl.createWorld();

		System.out.println("Reading network...");
		NetworkLayer networkLayer = new NetworkLayer();
		new MatsimNetworkReader(networkLayer).readFile(Gbl.getConfig().network().getInputFile());
		Gbl.getWorld().setNetworkLayer(networkLayer);
		System.out.println("Reading network...done.");

		System.out.println("Reading facilities...");
		Facilities facilityLayer = new Facilities();
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(facilityLayer);
		//facilities_reader.setValidating(false);
		facilities_reader.readFile(Gbl.getConfig().facilities().getInputFile());
		facilityLayer.printFacilitiesCount();
		Gbl.getWorld().setFacilityLayer(facilityLayer);
		Gbl.getWorld().complete();
		System.out.println("Reading facilities...done.");

		System.out.println("Setting up plans objects...");
		Population plans = new PopulationImpl(PopulationImpl.USE_STREAMING);
		PopulationWriter plansWriter = new PopulationWriter(plans);
		PopulationReader plansReader = new MatsimPopulationReader(plans, networkLayer);
		System.out.println("Setting up plans objects...done.");

		System.out.println("Setting up person modules...");
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		plans.addAlgorithm(new PlansCalcRoute(networkLayer, timeCostCalc, timeCostCalc));
		System.out.println("Setting up person modules...done.");

		System.out.println("Reading, processing and writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
		System.out.println("Reading, processing and writing plans...done.");

	}

}
