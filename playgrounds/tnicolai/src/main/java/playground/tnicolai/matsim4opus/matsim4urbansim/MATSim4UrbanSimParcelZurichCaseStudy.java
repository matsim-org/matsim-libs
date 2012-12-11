
/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimERSA.java
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;

import playground.tnicolai.matsim4opus.scenario.zurich.ZurichUtilities;
import playground.tnicolai.matsim4opus.scenario.zurich.ZurichUtilitiesIVTCHOSMNetwork;

/**
 * This class extends MATSim4UrbanSimV2 including two extra accessibility measurements
 * 1) Grid-based accessibility using a shape file (better for plotting)
 * 2) Grid-based accessibility using network only, dumping out different girdsize resolutions (easier to use)
 * tnicolai feb'12
 * 
 * Added custom boundig box for Grid-based accessibility using the network only
 * 	- reason: avoiding "out of memory" issues 
 * tnicolai march'12
 * 
 * @author thomas
 *
 */
class MATSim4UrbanSimParcelZurichCaseStudy extends MATSim4UrbanSimParcel{
	
	// Logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimParcelZurichCaseStudy.class);
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimParcelZurichCaseStudy(String args[]){
		super(args);
	}
	
	/**
	 * This modifies the MATSim network according to the given
	 * test parameter in the MATSim config file (from UrbanSim)
	 */
	@Override
	void modifyNetwork(Network network){
		log.info("");
		log.info("Checking for network modifications ...");
		// check given test parameter for desired modifications
//		String testParameter = scenario.getConfig().getParam(Constants.URBANSIM_PARAMETER, Constants.TEST_PARAMETER_PARAM);
		String testParameter = getUrbanSimParameterConfig().getTestParameter();
		if(testParameter.equals("")){
			log.info("No modifications to perform.");
			log.info("");
			return;
		}
		else{
			String scenarioArray[] = testParameter.split(",");
			ZurichUtilitiesIVTCHOSMNetwork.modifyNetwork(network, scenarioArray);
//			ZurichUtilitiesZurichBigRoads.modifyNetwork(network, scenarioArray);
			log.info("Done modifying network.");
			log.info("");
		}
	}
	
	/**
	 * This removes plan elements from existing plans that
	 * contain a removed link
	 */
	@Override
	void modifyPopulation(Population population){
		ZurichUtilities.deleteRoutesContainingRemovedLinks(population);
	}
	
	/**
	 * This method allows to add additional listener
	 * to the super class
	 */
	@Override
	void addFurtherControlerListener(Controler controler, ActivityFacilitiesImpl parcels){
		
	}
	
	/**
	 * This is the program entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		long start = System.currentTimeMillis();
		
		MATSim4UrbanSimParcelZurichCaseStudy m4uZurich = new MATSim4UrbanSimParcelZurichCaseStudy(args);
		m4uZurich.runMATSim();
		m4uZurich.matsim4UrbanSimShutdown();
		
		log.info("Computation took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}

}

