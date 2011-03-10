/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkGenerator.java
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
package playground.johannes.socialnetworks.sim.interaction;

import gnu.trove.TObjectIntIterator;

import java.io.IOException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author illenberger
 *
 */
public class NetworkGenerator {

	private static final Logger logger = Logger.getLogger(NetworkGenerator.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String netFile = args[0];
		String facFile = args[1];
		String popFile = args[2];
		String graphFile = args[3];
		double proba = Double.parseDouble(args[4]);

		GeometryFactory geoFactory = new GeometryFactory();

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse(netFile);

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
		facReader.parse(facFile);

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);

		logger.info("Building empty graph...");
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(CRSUtils.getCRS(21781));
		SocialSparseGraph graph = builder.createGraph();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			SocialPerson sPerson = new SocialPerson((PersonImpl) person);
			Coord home = ((Activity) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
			builder.addVertex(graph, sPerson, geoFactory.createPoint(new Coordinate(home.getX(), home.getY())));
		}

		logger.info("Initializing interactors...");

		BefriendInteractor interactor = new BefriendInteractor(graph, builder, proba, 0);
		InteractionSelector selector = new InteractionSelector() {
			@Override
			public Collection<Id> select(Id v, Collection<Id> choiceSet) {
				return choiceSet;
			}
		};

		InteractionHandler handler = new InteractionHandler(selector, interactor);

		EventsManagerImpl manager = new EventsManagerImpl();
//		manager.
		manager.addHandler(handler);

		TravelTime travelTime = new TravelTimeCalculator(scenario.getNetwork(), 60 * 60, 24 * 60 * 60,
				new TravelTimeCalculatorConfigGroup());
		PseudoSim sim = new PseudoSim();
		logger.info("Running pseudo sim...");
		sim.run(scenario.getPopulation(), scenario.getNetwork(), travelTime, manager);

		TObjectIntIterator<String> it = interactor.getActTypes().iterator();
		for (int i = 0; i < interactor.getActTypes().size(); i++) {
			it.advance();
			logger.info(String.format("Act type = %1$s, edges = %2$s.", it.key(), it.value()));
		}

		logger.info("Writing graph...");
		SocialGraphMLWriter writer = new SocialGraphMLWriter();
		writer.setPopulationFile(popFile);
		writer.write(graph, graphFile);
		logger.info("Done.");

	}

}
