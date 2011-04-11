/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.scenarioFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.PersonFilterSelectedPlan;

public class ScenarioFilter {

	private final static Logger log = Logger.getLogger(ScenarioFilter.class);

	private final Scenario scenario;

	private final String networkFileIn;
	private final String networkFileOut;
	private final String populationFileIn;
	private final String populationFileOut;
	private final String eventsFileIn;
	private final String eventsFileOut;

	public ScenarioFilter(final String networkFileIn, final String networkFileOut, final String populationFileIn, final String populationFileOut, final String eventsFileIn, final String eventsFileOut) {
		this.networkFileIn = networkFileIn;
		this.networkFileOut = networkFileOut;
		this.populationFileIn = populationFileIn;
		this.populationFileOut = populationFileOut;
		this.eventsFileIn = eventsFileIn;
		this.eventsFileOut = eventsFileOut;

		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(this.scenario).readFile(this.networkFileIn);
	}

	public void filterByLinks(Set<Id> linkIds) {
		// events
		log.info("filtering events...");
		EventsManager writerManager = (EventsManager) EventsUtils.createEventsManager();
		EventWriterXML xmlWriter = new EventWriterXML(this.eventsFileOut);
		writerManager.addHandler(xmlWriter);
		EventsFilterByLink eventsFilter = new EventsFilterByLink(writerManager, linkIds);
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(eventsFilter);
		new MatsimEventsReader(events).readFile(this.eventsFileIn);
		xmlWriter.closeFile();
		((EventsManagerImpl) events).printEventsCount();


		// population
		log.info("filtering population...");
		PopulationImpl population = (PopulationImpl) this.scenario.getPopulation();
		population.setIsStreaming(true);
		population.addAlgorithm(new PersonFilterSelectedPlan());
		population.addAlgorithm(new PopulationFilterByLink(linkIds));
		PopulationWriter populationWriter = new PopulationWriter(population, this.scenario.getNetwork());
		population.addAlgorithm(populationWriter);
		populationWriter.startStreaming(this.populationFileOut);
		new MatsimPopulationReader(this.scenario).readFile(this.populationFileIn);
		populationWriter.closeStreaming();

		// network
		log.info("filtering network...");
		List<Link> allLinks = new ArrayList<Link>(this.scenario.getNetwork().getLinks().values());
		for (Link link : allLinks) {
			if (!linkIds.contains(link.getId())) {
				this.scenario.getNetwork().removeLink(link.getId());
			}
		}
		List<Node> allNodes = new ArrayList<Node>(this.scenario.getNetwork().getNodes().values());
		for (Node node : allNodes) {
			if (node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) {
				this.scenario.getNetwork().removeNode(node.getId());
			}
		}
		new NetworkWriter(this.scenario.getNetwork()).write(this.networkFileOut);

		log.info("done.");
	}

	public void filterByRegion(final double xmin, final double ymin, final double xmax, final double ymax) {
		filterByLinks(getLinkIdsInRegion(xmin, ymin, xmax, ymax));
	}

	private Set<Id> getLinkIdsInRegion(final double xmin, final double ymin, final double xmax, final double ymax) {
		Set<Id> linkIds = new HashSet<Id>(1000);
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			if (isPointInRegion(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY(), xmin, ymin, xmax, ymax) ||
					isPointInRegion(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY(), xmin, ymin, xmax, ymax)) {
				linkIds.add(link.getId());
			}
		}
		System.out.println("# linkIds: " + linkIds.size());
		return linkIds;
	}

	private boolean isPointInRegion(final double x, final double y, final double xmin, final double ymin, final double xmax, final double ymax) {
//		System.out.println("checking point: " + x + " / " + y);
		return ((x >= xmin) && (x <= xmax) && (y >= ymin) && (y <= ymax));
	}

	public static void main(String[] args) {
		new ScenarioFilter("/data/dissVis/output_tr100pct2/output_network.xml.gz", "/data/dissVis/network.filtered.xml.gz",
				"/data/dissVis/output_tr100pct2/output_plans.xml.gz", "/data/dissVis/population.selPlanOnly.xml.gz",
//				"/data/dissVis/output_tr100pct2/it.100/100.events.xml.gz", "/data/dissVis/events.filtered.xml.gz").filterByRegion(663300, 225000, 686300, 289000);
				"/data/dissVis/output_tr100pct2/it.100/100.events.xml.gz", "/data/dissVis/events.filtered.xml.gz").filterByRegion(683300, 245000, 686300, 249000);
	}
}
