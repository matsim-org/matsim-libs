/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimCUPUM.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.tnicolai.urbansim.cupum;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.urbansim.MATSim4Urbansim;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;

/**
 * @author thomas
 *
 */
public class MATSim4UrbansimCUPUM extends MATSim4Urbansim{
	
	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbansimCUPUM.class);
	
	private static String testParam = "Ferry"; // default scenario
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbansimCUPUM(String args[]){
		super(args);
		
		if(args.length > 1)
			testParam = args[1];
	}
	
	public void runMATSim(){
		log.info("Starting MATSim from Urbansim");

		// checking for if this is only a test run
		// a test run only validates the xml config file by initializing the xml config via the xsd.
		if(scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.IS_TEST_RUN).equalsIgnoreCase(Constants.TRUE)){
			log.info("TestRun was successful...");
			return;
		}

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkImpl network = scenario.getNetwork();
		cleanNetwork(network);
		
		if(!testParam.equalsIgnoreCase("Ferry"))	// than its the Highway scenario
			modifyLinks(network, testParam);
		
		// get the data from urbansim (parcels and persons)
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( Integer.parseInt( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.YEAR) ), benchmark );
		// read UrbanSim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
		
		readUrbansimParcelModel(readFromUrbansim, facilities, zones);
		Population newPopulation = readUrbansimPersons(readFromUrbansim, facilities, network);
		Map<Id,WorkplaceObject> numberOfWorkplacesPerZone = ReadUrbansimJobs(readFromUrbansim);
		
		log.info("### DONE with demand generation from urbansim ###") ;

		// set population in scenario
		scenario.setPopulation(newPopulation);

		runControler(zones, numberOfWorkplacesPerZone, facilities, readFromUrbansim);
	}
	
	/**
	 * modifies the links in the travel network without changing the file
	 * @param network
	 */
	private void modifyLinks(NetworkImpl network, String testParam){
		
		double capacity = -1; // -1 means, no capacity changes on the network/links
		try{
			capacity = Double.parseDouble(testParam);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		log.info("Modifiying network now ...");
		
		double newFreespeed = 70*0.44704; // 70mph -> meter/sec
		
		ArrayList<IdImpl> wantedIdSet = new ArrayList<IdImpl>(){
			private static final long serialVersionUID = 1L;
			{
				add(new IdImpl(8325));
				add(new IdImpl(9711));
				add(new IdImpl(7691));
				add(new IdImpl(9710));
				add(new IdImpl(9709));
				add(new IdImpl(7143));
				add(new IdImpl(7142));
				add(new IdImpl(7127));
				add(new IdImpl(7126));
				add(new IdImpl(2060));
			}
		};
		
		for(int i = 0; i < wantedIdSet.size(); i++){
			IdImpl id = wantedIdSet.get(i);
			LinkImpl link = (LinkImpl)network.getLinks().get(id);
			
			// modify free speed
			link.setFreespeed( newFreespeed );
			
			// modify link capacity, if available
			if(capacity >= 0.)
				link.setCapacity(capacity);
			
			printLinkInfo(link);			
		}
		log.info("Finished modifying network.");
	}
	
	/**
	 * prints key data of a link
	 * 
	 * @param link
	 */
	private void printLinkInfo(LinkImpl link){
		
		log.info("#########################################");
		log.info("Link ID:" + link.getId());
		log.info("Link Capacity:" + link.getCapacity()); // capacity in vehicles/hour (standard for german autobahn = 2000)
		log.info("Link FlowCapacity:" + link.getFlowCapacity());
		log.info("Link Freespeed:" + link.getFreespeed()); // freespeed in meter/sec
		log.info("Link FreespeedTravelTime:" + link.getFreespeedTravelTime()); // = length/freespeed
		log.info("Link NumberOfLanes:" + link.getNumberOfLanes());
		log.info("Link AllowedModes:" + link.getAllowedModes());
		log.info("Link Length:" + link.getLength());
		log.info("Link Type:" + link.getType());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSim4UrbansimCUPUM m4uc = new MATSim4UrbansimCUPUM(args);
		m4uc.runMATSim();
	}
}

