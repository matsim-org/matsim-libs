/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * Class doing a simple one road straight line simulation.
 * Uses normal QSim atm, is meant to be implemented with other Sim though
 *   in order to test their simulation model.
 * Demand is a configurable number of people departing at regular times;
 *   the frequency of departure is configurable.
 * The network is also configurable in capacity and number of links.
 * 
 * @author ssix
 */

public class LangeStreckeSzenario {
	
	/**
	 * @param args
	 */
	
	private Scenario scenario;
	
	private double length;
	private int numberOfLinks;
	private int[] capacities;
	
	public LangeStreckeSzenario(double length, int numberOfLinks, int[] capacities){
		this.length = length;
		this.numberOfLinks = numberOfLinks;
		if (capacities.length == numberOfLinks) {
			this.capacities = capacities;
		} else {
			throw new IllegalArgumentException("In the present case, the array 'capacities' must have " + numberOfLinks + " links!");
		}
		
		Config config = ConfigUtils.createConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;
		
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT) ;
		// this may lead to abort during execution.  In such cases, please fix the configuration.  if necessary, talk
		// to me (kn).

		this.scenario = ScenarioUtils.createScenario(config);
	}

	
	public static void main(String[] args) {
		int[] capacities = {500};//must have a size of numberOfLinks!
		new LangeStreckeSzenario(5000.0,1,capacities).run();

	}
	
	public void run(){
		fillNetworkData();
		createPopulation((long)5000, 3600/400);
		
		EventsManager events = EventsUtils.createEventsManager();
		LinkStatusSpy linkSpy = new LinkStatusSpy(/*this.scenario,*/ (Id) new IdImpl((long)(1)));
		Link link = scenario.getNetwork().getLinks().get(linkSpy.getLinkId());
		FundamentalDiagrams fundi = new FundamentalDiagrams(scenario, link.getId());
		
		events.addHandler(linkSpy);
		events.addHandler(fundi);
		
		runqsim(events);
		linkSpy.sameLeavingOrderAsEnteringOrder();
		fundi.saveAsPng("./output");
	}
	
	private void fillNetworkData(){
		Network network = scenario.getNetwork();
		
		int capmax=0;
		for (int j=0; j<numberOfLinks; j++){
			if (capacities[j]>capmax)
				capmax=capacities[j];
		}
		int capMax = 10*capmax;
		
		//nodes
		for (int i = 0; i<numberOfLinks+1; i++){
			double x=0;
			if (i==0)
				x = 0;
			else{
				for (int j=0; j<i; j++){
					x += this.length/this.numberOfLinks;
				}
			}
			Coord coord = scenario.createCoord(x, 0.0);
			Id id = new IdImpl((long)(i+1));
			
			Node node = new NetworkFactoryImpl(network).createNode(id, coord);
			network.addNode(node);
		}
		Coord coord = scenario.createCoord(-20.0, 0.0);
		Id id = new IdImpl(0);
		Node startNode = new NetworkFactoryImpl(network).createNode(id, coord);
		network.addNode(startNode);
		coord = scenario.createCoord(length+20.0, 0.0);
		id = new IdImpl(numberOfLinks+2);
		Node endNode = new NetworkFactoryImpl(network).createNode(id, coord);
		network.addNode(endNode);
		
// preferred syntax:
//		Link link = network.getFactory().createLink(id, startNode, endNode) ;
//		link.setCapacity(50.) ;
//		network.addLink(link) ;
		
		//links
		for (int i = 0; i<numberOfLinks; i++){
			id = new IdImpl((long)(i+1));
			Id idto = new IdImpl((long)(i+2));
			Node from = network.getNodes().get(id);
			Node to = network.getNodes().get(idto);
			double laenge = this.length/this.numberOfLinks;
			double freespeed = 50.0/3.6;
			double capacity = capacities[i];
			double lanes = 1.0;
			
			Link link = new LinkFactoryImpl().createLink(id, from, to, network, laenge, freespeed, capacity, lanes);
			network.addLink(link);
		}
		Link startLink = new LinkFactoryImpl().createLink(new IdImpl(0), network.getNodes().get(new IdImpl(0)), network.getNodes().get(new IdImpl(1)), network, 20.0, 50/3.6, capMax, 1.0);
		network.addLink(startLink);
		Link endLink = new LinkFactoryImpl().createLink(new IdImpl(numberOfLinks+1), network.getNodes().get(new IdImpl(numberOfLinks+1)), network.getNodes().get(new IdImpl(numberOfLinks+2)), network, 20.0, 50/3.6, capMax, 1.0);
		network.addLink(endLink);
		
		
		//check with .xml and Visualizer
		//NetworkWriter writer = new NetworkWriter(network);
		//writer.write("./input/network.xml");
	}

	private void createPopulation(long anzahl, int sekundenFrequenz){
		//TODO:make it more flexible
		Population population = scenario.getPopulation();
		
		for (long i = 0; i<anzahl; i++){
			
			Person person = population.getFactory().createPerson(createId(i+1));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(sekundenFrequenz, i+1));
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addLeg(leg);
			plan.addActivity(createWork());
			
			person.addPlan(plan);
			population.addPerson(person);
		}
		
		//check with xml
		//PopulationWriter writer = new PopulationWriter(population, scenario.getNetwork());
		//writer.write("./input/plans.xml");
	}
	
	private void runqsim(EventsManager events){
		Netsim qSim = new QSimFactory().createMobsim(scenario, events);
		prepareForSim();
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, (QSim)qSim);
		
		OTFClientLive.run(scenario.getConfig(), server);
		qSim.run();
	}
	
	private Activity createHome(int sekundenFrequenz, long identifier){
		Id homeLinkId = new IdImpl(0);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("home", homeLinkId);
		
		Random r = new Random();
//		Method 1: The order of leaving people is guaranteed by the minimum time step between people: first person 1 leaves, then 2, then 3 etc...
		long plannedEndTime = 6*3600 + (identifier-1)*sekundenFrequenz;
		double endTime = plannedEndTime - sekundenFrequenz/2.0 + r.nextDouble()*sekundenFrequenz;

		///*Method 2: With the expected frequency, the maximal departure time is computed and people are randomly departing within this huge time chunk.
//		long TimeChunkSize = scenario.getPopulation().getPersons().size() * sekundenFrequenz;
//		double endTime = 6 * 3600 + r.nextDouble() * TimeChunkSize; 
		//*/
		//NB:Method 2 is significantly better for the quality of fundamental diagrams;
		activity.setEndTime(endTime);
		
		return activity;
	}
	
	private Activity createWork(){
		Id workLinkId = new IdImpl(numberOfLinks+1);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("work", workLinkId);
		return activity;
	}
	
	private Id createId(long id){
		return new IdImpl(id);
	}
	
	private void prepareForSim() {
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), scenario.getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				TravelTimeCalculator travelTimes = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
				TravelDisutility travelCosts = new TravelCostCalculatorFactoryImpl().createTravelDisutility(travelTimes, scenario.getConfig().planCalcScore());
				PlansCalcRoute plansCalcRoute = new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), travelCosts, travelTimes, new DijkstraFactory(), ((PopulationFactoryImpl)(scenario.getPopulation().getFactory())).getModeRouteFactory());
				return new PersonPrepareForSim(plansCalcRoute, (ScenarioImpl)scenario);
			}
		});
	}

}
