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

package playground.boescpa.projects.topdad.subscenario;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.filters.population.PersonIntersectAreaFilter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

/**
 * ZurichFromSwitzerland creates a Zurich-subscenario from a full Switzerland-scenario.
 * 
 * This class is a customization of the class
 * /balmermi/src/main/java/playground/balmermi/census2000v2/DilutedZurichFilter.java
 * 
 * @author pboesch
 *
 */
public class ZurichFromSwitzerland {
	
	private final static Logger log = Logger.getLogger(ZurichFromSwitzerland.class);

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		int radiusZone = Integer.parseInt(args[2]);
		createSubscenario(config, args[1], radiusZone);
	}
	
	public static void createSubscenario(Config config, String filenameOutput, int radiusZone) {

		log.info("Create Subscenario...");
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		//////////////////////////////////////////////////////////////////////
		
		log.info("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading facilities xml file...");
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		log.info("  done.");

		log.info("  reading the network xml file...");
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  calculate area of interest... ");
		double radius = radiusZone;// 30000.0;
		final Coord center = new Coord(683518.0, 246836.0);
		final Map<Id<Link>, Link> areaOfInterest = new HashMap<>();
		log.info("    => area of interest (aoi): center=" + center + "; radius=" + radius);

		log.info("    extracting links of the aoi... " + (new Date()));
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcEuclideanDistance(from.getCoord(), center) <= radius)
					|| (CoordUtils.calcEuclideanDistance(to.getCoord(), center) <= radius)) {
				areaOfInterest.put(link.getId(),link);
			}
		}
		log.info("    done. " + (new Date()));
		log.info("    => aoi contains: " + areaOfInterest.size() + " links.");
		log.info("  done. " + (new Date()));

		//////////////////////////////////////////////////////////////////////

		log.info("  setting up population objects...");
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter pop_writer = new PopulationWriter(pop, scenario.getNetwork());
		pop_writer.startStreaming(filenameOutput);
		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(pop_writer, areaOfInterest, network);
		filter.setAlternativeAOI(center, radius);
		pop.addAlgorithm(filter);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading, processing, writing plans...");
		pop_reader.readFile(config.plans().getInputFile());
		pop_writer.closeStreaming();
		pop.printPlansCount();
		log.info("    => filtered persons: " + filter.getCount());
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("done.");
	}
}
