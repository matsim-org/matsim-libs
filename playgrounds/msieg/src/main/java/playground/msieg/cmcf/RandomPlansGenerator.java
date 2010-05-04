/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlanGenerator.java
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

package playground.msieg.cmcf;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.utils.misc.NetworkUtils;
import org.xml.sax.SAXException;

/**
 * This class is designed to create random plan files for
 * a given network.
 *
 * @author msieg
 *
 */
public class RandomPlansGenerator {

	protected NetworkLayer network;

	private Random rand;

	private String networkFile;

	private LeastCostPathCalculator routingAlgo;

	private final int workDuration = 4 * 3600; //4 hours of work

	public RandomPlansGenerator(){
		this.setSeed(this.hashCode());
	}


	public RandomPlansGenerator(final String s){
		this();
		this.setNetworkFile(s);
		try {
			this.read(new ScenarioImpl());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSeed(final long seed){
		if(this.rand == null)
			this.rand = new Random(seed);
		else
			this.rand.setSeed(seed);
	}

	public String getNetworkFile() {
		return this.networkFile;
	}

	public void setNetworkFile(final String networkFile) {
		this.networkFile = networkFile;
	}

	public void read(ScenarioImpl scenario) throws IOException {
		this.network = scenario.getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		try {
			netReader.parse(this.networkFile);
			this.network.connect();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			throw e;
		}
	}

	private void initDijkstra(){
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(this.network);
		final FreespeedTravelTimeCost costFunction = new FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup());
		this.routingAlgo = new Dijkstra(this.network, costFunction, costFunction, preProcessData);
	}

	public Population createPlans(final int numberOfAgents){
		return this.createPlans(numberOfAgents, 6*3600, 7*3600);
	}

	public Population createPlans(final int numberOfAgents, final int startTime, final int endTime){
		Population pop = new ScenarioImpl().getPopulation();
		this.initDijkstra();

		for(int i=1; i <= numberOfAgents; i++){
			PersonImpl person = (new PersonImpl(new IdImpl(i)));
			PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
			person.addPlan(plan);

			//create random home and work link
			Link lHome, lWork;
			LeastCostPathCalculator.Path lPath = null;
			double randTime;
			do{
				lHome = this.getRandomLink();
				lWork = lHome;
				while(lHome == lWork)
					lWork = this.getRandomLink();
				randTime = startTime+this.rand.nextInt(endTime-startTime);
				lPath = this.routingAlgo.calcLeastCostPath(lHome.getToNode(), lWork.getFromNode(), randTime);
			}while(lPath == null);
			Coord homeCoord = lHome.getCoord(), workCoord = lWork.getCoord();

			//create home act
			ActivityImpl a = plan.createAndAddActivity("h", homeCoord);
			a.setLinkId(lHome.getId());
			a.setStartTime(startTime);
			a.setEndTime(endTime);

			//create leg to work
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			leg.setDepartureTime(randTime);
			NetworkRoute route = new LinkNetworkRouteImpl(lHome.getId(), lWork.getId());
			route.setLinkIds(lHome.getId(), NetworkUtils.getLinkIds(lPath.links), lWork.getId());
			leg.setRoute(route);

			//create work act
			a = plan.createAndAddActivity("h", workCoord);
			a.setLinkId(lWork.getId());
			a.setDuration(this.workDuration);

			//finally add person to population instance
			pop.addPerson(person);
		}

		return pop;
	}

	private Link getRandomLink(){
		int i = this.rand.nextInt(this.network.getLinks().size());
		Link randomLink = null;
		for (Iterator<Link> iterator = this.network.getLinks().values().iterator(); i >= 0; i--) {
			randomLink = iterator.next();
		}
		return randomLink;
	}

	public void writePlans(final Population plans) {
		new PopulationWriter(plans, this.network).write(Gbl.getConfig().plans().getOutputFile());
	}

	public void writePlans(final Population plans, final String file) {
		new PopulationWriter(plans, this.network).write(file);
	}

	public static void main(final String[] args) {
		String inFile = "examples/siouxfalls/network.xml";
		if(args.length == 0){
			System.out.println("usage: java "+RandomPlansGenerator.class.getSimpleName() +
					"networkFile [numberOfAgents] [startTime] [endTime]");
			//System.exit(10);
		}
		else
			inFile = args[0];
		int number=3, start=6*3600, end=7*3600;
		if(args.length > 1)
			number = Integer.parseInt(args[1]);
		if(args.length > 2)
			start = Integer.parseInt(args[2]);
		if(args.length > 3)
			end = Integer.parseInt(args[3]);

		RandomPlansGenerator rpg = new RandomPlansGenerator(inFile);

		Population pop = rpg.createPlans(number, start, end);

		String outFile = "";
		if(inFile.lastIndexOf('/') != -1)
			outFile = inFile.substring(0, inFile.lastIndexOf('/')+1);
		outFile += "randPlans"+number+"_"+System.currentTimeMillis()+".xml";

		System.out.println(pop);
		rpg.writePlans(pop, outFile);
		System.out.println("\tNew Plans-File written to: "+outFile);
	}
}

