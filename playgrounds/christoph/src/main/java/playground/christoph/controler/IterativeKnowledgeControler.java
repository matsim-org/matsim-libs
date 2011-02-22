/* *********************************************************************** *
 * project: org.matsim.*
 * IterativeKnowledgeControler.java
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

package playground.christoph.controler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;

import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.util.SubNetworkCreator;
import playground.christoph.router.costcalculators.KnowledgeTravelCostWrapper;

public class IterativeKnowledgeControler extends Controler{
	
	private static final Logger log = Logger.getLogger(IterativeKnowledgeControler.class);

	protected boolean knowledgeLoaded = false;
	protected boolean createSubNetworks = false;
	protected int numOfThreads = 2;
	
	/*
	 * Select which size the known Areas should have in the Simulation.
	 * The needed Parameter is the Name of the Table in the MYSQL Database.
	 */
	private static final String tableName = "BatchTable1_5";

	public IterativeKnowledgeControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

	private void setConstructorParameters() {
//		// Use MyLinkImpl. They can carry some additional Information like their
//		// TravelTime or VehicleCount.
//		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());
	}

	private void setScoringFunction() {
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	private void setCalculators() {
		// Use knowledge when Re-Routing
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(this.getTravelTimeCalculator());
		final KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
		travelCostWrapper.checkNodeKnowledge(true);

		this.setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

			@Override
			public PersonalizableTravelCost createTravelCostCalculator(
					PersonalizableTravelTime timeCalculator,
					PlanCalcScoreConfigGroup cnScoringGroup) {
				return travelCostWrapper;
			}
			
		});
//		this.setTravelCostCalculator(travelCost);
	}

	private void initialReplanning() {
		log.info("Do initial Replanning");

//		// Use a Wrapper - by doing this, already available MATSim
//		// CostCalculators can be used
//		TravelTime travelTime = new FreespeedTravelTimeCost(this.config.charyparNagelScoring());
//		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(this.getTravelTimeCalculator());
//		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
//		travelCostWrapper.checkNodeKnowledge(true);
//
//		CloneablePlansCalcRoute dijkstraRouter = new CloneablePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCostWrapper, travelTime);
		
		/*
		 * We don't use the knowledge here - the initial routes will be anyway on the shortest path.
		 */
		PersonalizableTravelTime travelTime = new FreespeedTravelTimeCost(this.config.planCalcScore());
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
		PlansCalcRoute dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCost, travelTime);
		
		for (Person person : this.getPopulation().getPersons().values())
		{
			dijkstraRouter.run(person.getSelectedPlan());
		}
	}

	private void loadKnowledgeFromDB() {
		log.info("Loading Knowledge from Database");
	
		if (numOfThreads == 1) {
			SubNetworkCreator snc = new SubNetworkCreator(network);
			
			for (Person person : this.getPopulation().getPersons().values()) {
				Map<String, Object> customAttributes = person.getCustomAttributes();
				
				customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());
				
				MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
				mapKnowledgeDB.setPerson(person);
				mapKnowledgeDB.setNetwork(network);
				mapKnowledgeDB.setTableName(tableName);
				
				mapKnowledgeDB.readFromDB();
				
				customAttributes.put("NodeKnowledge", mapKnowledgeDB);
				
				if (createSubNetworks)
				{
					SubNetwork subNetwork = snc.createSubNetwork(mapKnowledgeDB);
					customAttributes.put("SubNetwork", subNetwork);
				}
			}
		}
		else {
			LoadKnowledgeFromDBThread[] threads = new LoadKnowledgeFromDBThread[numOfThreads];
			
			// create the Threads
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new LoadKnowledgeFromDBThread();
			}
			
			int distributor = 0;
			for (Person person : this.getPopulation().getPersons().values()) {
				threads[distributor % numOfThreads].addPerson(person);
			}
		
			// start the Threads
			for (LoadKnowledgeFromDBThread thread : threads) thread.start();
			
			// wait until the Threads are finished
			try {
				for (LoadKnowledgeFromDBThread thread : threads) thread.join();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	@Override
	protected void setUp() {
		super.setUp();

		setScoringFunction();
		setCalculators();
	}

	@Override
	protected void runMobSim() {
		if (!knowledgeLoaded) {
			loadKnowledgeFromDB();
			initialReplanning();
			knowledgeLoaded = true;
		}

		super.runMobSim();
	}

	private class LoadKnowledgeFromDBThread extends Thread {
		private List<Person> persons;
		private SubNetworkCreator snc = new SubNetworkCreator(network);
		
		public LoadKnowledgeFromDBThread() {
			this.persons = new ArrayList<Person>();
		}
		
		public void addPerson(Person person) {
			this.persons.add(person);
		}
			
		@Override
		public void run() {
			for (Person person : persons) {
				Map<String, Object> customAttributes = person.getCustomAttributes();

				customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());

				MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
				mapKnowledgeDB.setPerson(person);
				mapKnowledgeDB.setNetwork(network);
				mapKnowledgeDB.setTableName(tableName);

				mapKnowledgeDB.readFromDB();

				customAttributes.put("NodeKnowledge", mapKnowledgeDB);
				
				if (createSubNetworks) {
					SubNetwork subNetwork = snc.createSubNetwork(mapKnowledgeDB);
					customAttributes.put("SubNetwork", subNetwork);
				}
			}
		}
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		}
		else {
			final IterativeKnowledgeControler controler = new IterativeKnowledgeControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
}
