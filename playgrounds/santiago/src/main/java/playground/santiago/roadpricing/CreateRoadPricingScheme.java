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

package playground.santiago.roadpricing;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;

import playground.santiago.counts.CreateCountingStations;

public class CreateRoadPricingScheme {

	private static final Logger log = Logger.getLogger(CreateCountingStations.class) ;

	private static final String svnWorkingDir = "../../../shared-svn/projects/santiago/";	//Path: KT (SVN-checkout)
	private static final String workingDirInputFiles = svnWorkingDir + "scenario/inputFromElsewhere/exportedFilesFromDatabase/" ;
	private static final String outputDir = svnWorkingDir + "scenario/inputForMATSim/roadpricing/" ; //outputDir of this class -> input for Matsim (KT)

	//Data of tolled links (Input from Alejandro) 
	private static final String TollDataFILE = workingDirInputFiles + ".csv" ; //TODO: Name and include it
	//Network the CSIdFile correspondent to -> to generate the coordinates of tolled links for the case, that link-Ids have changed.
	private static final String NETFILE_tollLinks = workingDirInputFiles + "santiago_merged_cl.xml.gz";		//TODO: check if correct Network
		
	// recent network-File
	private static final String NETFILE_current = svnWorkingDir + "Kai_und_Daniel/inputForMATSim/network/network_merged_cl.xml.gz";	
	
	/**
	 * first preparations for Creation of RoadPricingScheme for the santiago scenario.
	 * TODO: Needs the input from Alejandro and than bringing the data in here.
	 * TODO: Read Ids of Links, generate there coordinates 
	 * TODO: Read costs and times 
	 * 
	 * Generates a RoadPricingScheme from some InputDate (not available yet).
	 * currently it is not VehicleTypeDependent, so "car" values are used.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		File file = new File(outputDir);
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETFILE_current);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		createRPScheme(scenario);
		
		log.info("### Done. all RPSchemes were created. ###");

	}

	private static void createRPScheme(Scenario scenario) {
		RoadPricingSchemeImpl rps = new RoadPricingSchemeImpl();
		rps.setName("Santiago_Roadpricing");
		rps.setDescription("RoadPricingScheme for the Santiago scenario. ");
		rps.setType(RoadPricingScheme.TOLL_TYPE_DISTANCE);
		
		extractCurrentLinkIds(scenario.getNetwork()); //TODO: not implemented in creation process here
		
		rps.addLinkCost(Id.createLinkId(356), 3600.0, 7200., 1);
		
		//Write final roadPricingScheme to file.	
		RoadPricingWriterXMLv1 rpWriter = new RoadPricingWriterXMLv1(rps);
		rpWriter.writeFile(outputDir + "rpScheme.xml");
	}

	//Assumption (KT: for each road (e.g. Autopista Central,..) there are the same costs ratios per meter
	//TODO: Import of the data.
	//TODO: Export the coordinates for the different links per Road --> be independent from any change of linkIds through changes and recreation of the network
	private static Map<String, ArrayList<Id<Link>>> extractCurrentLinkIds(Network network) {
		Map<String, ArrayList<Id<Link>>> roadName2LinkIdList = new TreeMap<String,ArrayList<Id<Link>>>();
		Map<String, ArrayList<Double>> oldLinkIdString2LinkCoordinates = new TreeMap<String, ArrayList<Double>>();
		
		String nameOfRoad = "nameOfRoad";
		
		//using oldLinkId as identifier (KT, 2015-09-07)
		//Search link (id) in current network
		roadName2LinkIdList.put("nameOfRoad", null); //TODO: enter correctName
		for (String oldLinkIdString : oldLinkIdString2LinkCoordinates.keySet()){
			Id<Link> linkId = null;
			for (Link link : network.getLinks().values()){
				if (link.getFromNode().getCoord().getX() == oldLinkIdString2LinkCoordinates.get(oldLinkIdString).get(0)){
					if (link.getFromNode().getCoord().getY() == oldLinkIdString2LinkCoordinates.get(oldLinkIdString).get(1)){
						if (link.getToNode().getCoord().getX() == oldLinkIdString2LinkCoordinates.get(oldLinkIdString).get(2)){
							if (link.getToNode().getCoord().getY() == oldLinkIdString2LinkCoordinates.get(oldLinkIdString).get(3)){
								linkId = link.getId();
								System.out.println("Link for CS:  " + oldLinkIdString + " is : " + linkId);
							} 
						} 
					} 
				} 
			}

			if (linkId != null) {
				roadName2LinkIdList.get("nameOfRoad").add(linkId);	//TODO: enter correctName
			} else {
				log.warn("Can't find link for CS: " + oldLinkIdString );
			}
		}
		
		log.info("collected linkIds of road");
		return roadName2LinkIdList;
	}


}
