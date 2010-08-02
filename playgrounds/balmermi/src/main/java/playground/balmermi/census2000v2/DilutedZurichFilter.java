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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.filters.PersonIntersectAreaFilter;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldConnectLocations;

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

		ScenarioImpl scenario = new ScenarioImpl(config);
		World world = scenario.getWorld();

		//////////////////////////////////////////////////////////////////////

		log.info("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		log.info("  done.");

		log.info("  extracting output directory... ");
		String outdir = config.facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("    "+outdir);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading facilities xml file...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		world.complete(config);
		log.info("  done.");

		System.out.println("  reading the network xml file...");
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		world.complete(config);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  running world modules... ");
		new WorldConnectLocations(config).run(world);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  calculate area of interest... ");
		double radius = 30000.0;
		final CoordImpl center = new CoordImpl(683518.0,246836.0);
		final Map<Id, Link> areaOfInterest = new HashMap<Id, Link>();
		log.info("    => area of interest (aoi): center=" + center + "; radius=" + radius);

		log.info("    extracting links of the aoi... " + (new Date()));
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= radius) || (CoordUtils.calcDistance(to.getCoord(), center) <= radius)) {
				areaOfInterest.put(link.getId(),link);
			}
		}
		log.info("    done. " + (new Date()));
		log.info("    => aoi contains: " + areaOfInterest.size() + " links.");
		log.info("  done. " + (new Date()));

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up population objects...");
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter pop_writer = new PopulationWriter(pop, scenario.getNetwork());
		pop_writer.startStreaming(config.plans().getOutputFile());
		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(pop_writer,areaOfInterest, network);
		filter.setAlternativeAOI(center,radius);
		pop.addAlgorithm(filter);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		pop_reader.readFile(config.plans().getInputFile());
		pop_writer.closeStreaming();
		pop.printPlansCount();
		System.out.println("    => filtered persons: " + filter.getCount());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  writing network xml file... ");
		new NetworkWriter(network).write(config.network().getOutputFile());
		log.info("  done.");

		log.info("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(config.facilities().getOutputFile());
		log.info("  done.");

		log.info("  writing config xml file... ");
		new ConfigWriter(config).write(config.config().getOutputFile());
		log.info("  done.");

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		filterDemand(config);

		Gbl.printElapsedTime();
	}
}
