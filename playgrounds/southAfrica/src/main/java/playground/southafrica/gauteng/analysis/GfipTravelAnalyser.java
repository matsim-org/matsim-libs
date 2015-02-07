/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.gauteng.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;

import playground.southafrica.utilities.Header;

public class GfipTravelAnalyser {
	private final static Logger LOG = Logger.getLogger(GfipTravelAnalyser.class);
	private static List<Id<Link>> links = null;
	
	private static GfipTravelEventsHandler handler;

	public static void main(String[] args) {
		Header.printHeader(GfipTravelAnalyser.class.toString(), args);
		
		links = parseGfipLinkIds();
		
		String populationFile = null;
		String populationAttributesFile = null;
		String networkFile = null;
		String vehiclesFile = null;
		String eventsFile = null;
		String outputFile = null;
		
		if(args.length == 6){
			populationFile = args[0];
			populationAttributesFile = args[1];
			networkFile = args[2];
			vehiclesFile = args[3];
			eventsFile = args[4];
			outputFile = args[5];
		} else{
			LOG.error("Incorrect number of arguments. \n\tRequire the following: \n" + 
					"\t(1) Population file; \n\t(2) Population attributes file;\n" + 
					"\t(3) Network file; \n\t(4) Vehicles file;\n" + 
					"\t(5) Events file; and\n\t	(6) Output file");
		}
		
		/* Read all the scenario elements. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(populationFile);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(populationAttributesFile);
		new MatsimNetworkReader(sc).parse(networkFile);
		((ScenarioImpl)sc).createTransitVehicleContainer();
		new VehicleReaderV1(sc.getVehicles()).parse(vehiclesFile);
		
		calculateGfipTravel(sc, eventsFile);
		writeGfipTravelToFile(outputFile);
		
		Header.printFooter();
	}
	
	protected GfipTravelAnalyser() {
	}
	
	public static List<Id<Link>> parseGfipLinkIds(){
		List<Id<Link>> links = new ArrayList<>();
		String latestFile = "input/gauteng/gfip/GFIP_link_ids_201403.txt.gz";
		
		BufferedReader br = IOUtils.getBufferedReader(new File(latestFile).getAbsolutePath()
				);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				Id<Link> id = Id.create(line, Link.class);
				if(!links.contains(id)){
					links.add(id);
				} else{
					LOG.error("Link Id " + id.toString() + " already exists. Ignored.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + latestFile);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + latestFile);
			}
		}
		LOG.info("Parsed " + links.size() + " IDs from " + latestFile);
		return links;
	}
	
	public static void calculateGfipTravel(Scenario sc, String eventsFile){
		EventsManager em = new EventsManagerImpl();
		handler = new GfipTravelEventsHandler(sc, links);
		em.addHandler(handler);
		
		new MatsimEventsReader(em).readFile(eventsFile);
		
		handler.reportAggregateStatistics();
	}
	
	public static void writeGfipTravelToFile(String outputFile){
		LOG.info("Writing vehicle kilometers travelled to " + outputFile);
		Map<Id<VehicleType>, Map<Id<Vehicle>, Double>> map = handler.getMap();
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("Id,Type,Distance");
			bw.newLine();
			for(Id<VehicleType> id : map.keySet()){
				Map<Id<Vehicle>, Double> classMap = map.get(id);
				for(Id<Vehicle> vehId : classMap.keySet()){
					bw.write(String.format("%s,%s,%.0f\n", vehId.toString(), id.toString(), classMap.get(vehId) ));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		LOG.info("Done writing.");
	}

}
