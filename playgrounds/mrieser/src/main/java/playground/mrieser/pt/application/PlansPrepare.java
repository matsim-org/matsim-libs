/* *********************************************************************** *
 * project: org.matsim.*
 * PlansPrepare.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.application;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.filters.population.PersonIntersectAreaFilter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlansFilterByLegMode;
import org.matsim.core.population.algorithms.PlansFilterByLegMode.FilterType;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mrieser.PseudoScenario;

public class PlansPrepare {

	private final static String NETWORK_FILENAME = "/Volumes/Data/VSP/svn/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml";
	private final static String ALL_PLANS_FILENAME = "/Volumes/Data/VSP/svn/shared-svn/studies/schweiz-ivtch/baseCase/plans/plans_complete/plans.xml.gz";
	private final static String DILUTED_100PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch100pct.dilZh30km.xml.gz";
	private final static String DILUTED_10PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch10pct.dilZh30km.xml.gz";
	private final static String DILUTED_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.xml.gz";
	private final static String DILUTED_CAR_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.car.xml.gz";
	private final static String DILUTED_PT_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.pt.xml.gz";

	private final MutableScenario scenario;

	private static final Logger log = Logger.getLogger(PlansPrepare.class);

	public PlansPrepare() {
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	public void loadNetwork(final String filename) {
		log.info("reading network: " + filename);
		new MatsimNetworkReader(this.scenario.getNetwork()).readFile(filename);
		log.info("network-statistics:");
		log.info("  # nodes = " + this.scenario.getNetwork().getNodes().size());
		log.info("  # links = " + this.scenario.getNetwork().getLinks().size());
	}

	public void createDilutedPlans(final Coord center, final double radius, final String fromFile, final String toFile) {
		final Map<Id<Link>, Link> areaOfInterest = new HashMap<>();

		Network network = this.scenario.getNetwork();

		log.info("extracting aoi:");
		log.info("  center: " + center.getX() + " / " + center.getY());
		log.info("  radius: " + radius);
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcEuclideanDistance(from.getCoord(), center) <= radius) || (CoordUtils.calcEuclideanDistance(to.getCoord(), center) <= radius)) {
				System.out.println("    link " + link.getId().toString());
				areaOfInterest.put(link.getId(),link);
			}
		}
		log.info("  # links in aoi: " + areaOfInterest.size());

		log.info("creating diluted dpopulation:");
		log.info("  input-file:  " + fromFile);
		log.info("  output-file: " + toFile);
//		Population reader = (Population) ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( ScenarioUtils.createScenario( ConfigUtils.createConfig() ) ) ;
		StreamingDeprecated.setIsStreaming(reader, true);

		StreamingPopulationWriter writer = new StreamingPopulationWriter();
		writer.startStreaming(toFile);

		final PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(writer, areaOfInterest, network);
		filter.setAlternativeAOI(center, radius);
		reader.addAlgorithm(filter);
		
		if ( true ) {
			throw new RuntimeException("does not work any more after change of streaming api.  kai, jul'16" ) ;
		}

		// I commented out the following because it did not compile any more after changing the streaming api.  kai, jul'16
//		new MatsimPopulationReader(new PseudoScenario(this.scenario, reader)).readFile(fromFile);

		writer.closeStreaming();

		PopulationUtils.printPlansCount(reader) ;
		log.info("persons in output: " + filter.getCount());
	}

	public void createSamplePopulation(final String fromFile, final String toFile, final double percentage) {
//		Population reader = (Population) ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		StreamingDeprecated.setIsStreaming(reader, true);
		final StreamingPopulationWriter plansWriter = new StreamingPopulationWriter(null, percentage);
		plansWriter.startStreaming(toFile);
		reader.addAlgorithm(plansWriter);
		
		if ( true ) {
			throw new RuntimeException("don't know who the following should work after change of streaming api.  kai, jul'16") ;
		}
//		PopulationReader plansReader = new MatsimPopulationReader(new PseudoScenario(this.scenario, reader));

		log.info("extracting sample from population:");
		log.info("  input-file:  " + fromFile);
		log.info("  output-file: " + toFile);
		log.info("  sample-size: " + percentage);
//		plansReader.readFile(fromFile);
		reader.readFile( fromFile );
		PopulationUtils.printPlansCount(reader) ;
		plansWriter.closeStreaming();
	}

	public void filterMode(final String fromFile, final String toFile, final String mode) {

		Population pop = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();

		log.info("reading plans from file: " + fromFile);
		new PopulationReader(new PseudoScenario(this.scenario, pop)).readFile(fromFile);

		log.info("filter plans with " + mode + "-legs");
		new PlansFilterByLegMode(mode, FilterType.keepAllPlansWithMode).run(pop);
		log.info("# persons remaining: " + pop.getPersons().size());

		log.info("writing plans to file: " + toFile);
		new PopulationWriter(pop, this.scenario.getNetwork()).write(toFile);
	}

	public static void main(final String[] args) {
		PlansPrepare app = new PlansPrepare();
		app.loadNetwork(NETWORK_FILENAME);
//		app.createDilutedPlans(new CoordImpl(683518.0, 246836.0), 30000, ALL_PLANS_FILENAME, DILUTED_100PCT_PLANS_FILENAME);
//		app.createSamplePopulation(DILUTED_100PCT_PLANS_FILENAME, DILUTED_10PCT_PLANS_FILENAME, 0.1);
//		app.createSamplePopulation(DILUTED_10PCT_PLANS_FILENAME, DILUTED_1PCT_PLANS_FILENAME, 0.1);
		app.filterMode(DILUTED_1PCT_PLANS_FILENAME, DILUTED_CAR_1PCT_PLANS_FILENAME, TransportMode.car);
		app.filterMode(DILUTED_1PCT_PLANS_FILENAME, DILUTED_PT_1PCT_PLANS_FILENAME, TransportMode.pt);
		log.info("done.");
	}

}
