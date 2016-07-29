/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationCreation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.census2000v2;

import java.io.IOException;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class DilutedZurichFilter {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(DilutedZurichFilter.class);

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void filterDemand(Config config) {

		log.info("MATSim-DB: filterDemand...");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
//		World world = new World();

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

		System.out.println("  reading the network xml file...");
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  running world modules... ");
		// new WorldConnectLocations(config).run(world);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  calculate area of interest... ");
		double radius = 30000.0;
		final Coord center = new Coord(683518.0, 246836.0);
		final Map<Id<Link>, Link> areaOfInterest = new HashMap<>();
		log.info("    => area of interest (aoi): center=" + center + "; radius=" + radius);

		log.info("    extracting links of the aoi... " + (new Date()));
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcEuclideanDistance(from.getCoord(), center) <= radius) || (CoordUtils.calcEuclideanDistance(to.getCoord(), center) <= radius)) {
				areaOfInterest.put(link.getId(),link);
			}
		}
		log.info("    done. " + (new Date()));
		log.info("    => aoi contains: " + areaOfInterest.size() + " links.");
		log.info("  done. " + (new Date()));

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up population objects...");
//		Population reader = (Population) scenario.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
		StreamingUtils.setIsStreaming(reader, true);
		StreamingPopulationWriter pop_writer = new StreamingPopulationWriter(null, scenario.getNetwork());
		pop_writer.startStreaming(null);//config.plans().getOutputFile());
//		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(pop_writer,areaOfInterest, network);
		filter.setAlternativeAOI(center,radius);
		final PersonAlgorithm algo = filter;
		reader.addAlgorithm(algo);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
//		pop_reader.readFile(config.plans().getInputFile());
		reader.readFile(config.plans().getInputFile());
		pop_writer.closeStreaming();
		PopulationUtils.printPlansCount(reader) ;
		System.out.println("    => filtered persons: " + filter.getCount());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  writing network xml file... ");
		throw new RuntimeException("no network-output filename specified. see code");
//		new NetworkWriter(network).write(config.network().getOutputFile());
//		log.info("  done.");
//
//		log.info("  writing facilities xml file... ");
//		new FacilitiesWriter(facilities).write(config.facilities().getOutputFile());
//		log.info("  done.");
//
//		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws IOException {

		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);

		filterDemand(config);

		Gbl.printElapsedTime();
	}
}
