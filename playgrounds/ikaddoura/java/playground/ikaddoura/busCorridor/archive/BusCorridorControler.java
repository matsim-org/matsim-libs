/* *********************************************************************** *
 * project: org.matsim.*
 * BusCoridorControler_Simulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.archive;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.config.TransitConfigGroup;

import playground.ikaddoura.analysis.distance.Modus;

/**
 * @author Ihab
 *
 */

public class BusCorridorControler {

	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input/network_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input/population.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input/config_busline.xml";
	static String outputDirectoryPath = "../../shared-svn/studies/ihab/busCorridor/output/outputSimulation";
	static int lastInternalIteration = 0;
	static double busStopTime = 30;
	static double travelTimeBus = 3*60;
	
	private int numberOfBuses = 5;
	private String directoryExtIt = null;
	private String transitVehiclesFile = null;
	private String transitScheduleFile = null;
	private int extItNr = 0;
	private double highestScore = 0.0;
	private int iterationWithHighestScore = 0;

	Map<Integer, Double> iteration2providerScore = new HashMap<Integer, Double>();
	Map<Integer, Integer> iteration2numberOfBuses = new HashMap<Integer, Integer>();
	Map<Integer, Double> iteration2userScore = new HashMap<Integer,Double>(); // avg. executed Score
	
	public static void main(String[] args) throws IOException {
		BusCorridorControler simulation = new BusCorridorControler();
		simulation.runExternalSimulation();
	}
	
	private void runExternalSimulation() throws IOException {
	
		for (int extIt = 0; extIt <= 3 ; extIt++){
			this.setExtItNr(extIt);
			this.setDirectoryExtIt(outputDirectoryPath+"/extITERS/extIt."+extIt);
			File directory = new File(this.getDirectoryExtIt());
			directory.mkdirs();
			
			writeVehiclesAndSchedule();
			runInternalSimulation();
			calculateProviderScore();
			analyzeProviderScores();
			analyzeUserScores();
			providerStrategy();
		}
		writeScoreStats();
	}
	
	private void writeScoreStats() {
		TextFileWriter stats = new TextFileWriter();
		stats.writeFile(outputDirectoryPath, this.iteration2numberOfBuses, this.iteration2providerScore, this.iteration2userScore);
	}

	private void providerStrategy() {
		if (this.getExtItNr()==0){
			int newNumberOfBuses = this.getNumberOfBuses() + 1;  // Start-Strategie
			this.setNumberOfBuses(newNumberOfBuses);
		}
		else {
			int numberOfBusesBefore = this.iteration2numberOfBuses.get(this.getExtItNr()-1);
			double scoreBefore = this.iteration2providerScore.get(this.getExtItNr()-1);
			double score = this.iteration2providerScore.get(this.extItNr);
			if(numberOfBusesBefore < this.getNumberOfBuses() & scoreBefore < score){
				// mehr Busse, score angestiegen
				this.setNumberOfBuses(this.numberOfBuses+1);
			}
			if(numberOfBusesBefore < this.getNumberOfBuses() & scoreBefore > score){
				// mehr Busse, score gesunken
				this.setNumberOfBuses(this.numberOfBuses-1);
			}
			if (numberOfBusesBefore > this.getNumberOfBuses() & scoreBefore < score){
				// weniger Busse, score angestiegen
				this.setNumberOfBuses(this.numberOfBuses-1);
			}
			if (numberOfBusesBefore > this.getNumberOfBuses() & scoreBefore > score){
				// weniger Busse, score gesunken
				this.setNumberOfBuses(this.numberOfBuses+2);
			}
			else {
				System.out.println("******************** Score unverändert ******************");
				this.setNumberOfBuses(this.getNumberOfBuses());
			}
		}
	}

	private void analyzeProviderScores() {

		for (Integer ii : iteration2providerScore.keySet()){
			if (this.iteration2providerScore.get(ii) > this.getHighestScore()){
				this.setHighestScore(this.iteration2providerScore.get(ii));
				this.setIterationWithHighestScore(ii);
			}
			else {}
		}		
	}

	private void analyzeUserScores() {
		List<Double> scores = new ArrayList<Double>();
		double scoreSum = 0.0;
		
		String outputPlanFile = this.getDirectoryExtIt()+"/outputInternalSimulation/output_plans.xml.gz";
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(outputPlanFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();

		for(Person person : population.getPersons().values()){
			double score = person.getSelectedPlan().getScore();
			scores.add(score);
		}
		
		for (Double score : scores){
			scoreSum = scoreSum+score;
		}
		
		this.iteration2userScore.put(this.getExtItNr(), scoreSum/scores.size());
		
	}
	
	private void calculateProviderScore() {
		
		String configFile = this.getDirectoryExtIt()+"/outputInternalSimulation/output_config.xml.gz";
		String lastEventFile = this.getDirectoryExtIt()+"/outputInternalSimulation/ITERS/it."+lastInternalIteration+"/"+lastInternalIteration+".events.xml.gz";

//		Config config = ConfigUtils.loadConfig(configFile);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		EventsManager events = new BusCorridorEventsManagerImpl();
//		
//		BusCorridorLinkLeaveEventHandler handler1 = new BusCorridorLinkLeaveEventHandler(scenario);
//		BusCorridorActivityEndEventHandler handler2 = new BusCorridorActivityEndEventHandler(scenario);
//		BusCorridorPersonEntersVehicleEventHandler handler3 = new BusCorridorPersonEntersVehicleEventHandler(scenario);
//		BusCorridorPersonLeavesVehicleEventHandler handler4 = new BusCorridorPersonLeavesVehicleEventHandler(scenario);
//
//		events.addHandler(handler1);	
//		events.addHandler(handler2);
//		events.addHandler(handler3);
//		events.addHandler(handler4);
//		
//
//		MatsimEventsReader reader = new MatsimEventsReader(events);
//		reader.readFile(eventFile);
		
		// berechne aus der EventsFile einen Provider-Score
//		double busCostsPerDay = 500;
//		double busCostsPerKm = 1;
//		double fixCosts = this.getNumberOfBuses() * busCostsPerDay;
//		double varCosts = busKm * busCostsPerKm;
//		double providerScore = - ( fixCosts + varCosts ) + earnings
		
		double providerScore = 333;
		iteration2providerScore.put(this.getExtItNr(), providerScore);
		
		System.out.println("Events file "+lastEventFile+" read!");		
	}

	private void runInternalSimulation() {
		String population = null;
		if (this.getExtItNr()==0){
			population = populationFile;
		}
		else {
			population = outputDirectoryPath+"/extITERS/extIt."+(this.getExtItNr()-1)+"/outputInternalSimulation/output_plans.xml.gz";
		}

		Config config = new Config();
		config.addCoreModules();
		
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configFile);
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);

		NetworkConfigGroup network = controler.getConfig().network();
		network.setInputFile(networkFile);
		
		PlansConfigGroup plans = controler.getConfig().plans();
			
		plans.setInputFile(population);

		StrategyConfigGroup strategy = controler.getConfig().strategy();
		strategy.setMaxAgentPlanMemorySize(1);
//		strategy.addParam("ModuleProbability_1", "0.7");
//		strategy.addParam("Module_1", "BestScore");
//		strategy.addParam("ModuleProbability_2", "0.3");
//		strategy.addParam("Module_2", "TransitTimeAllocationMutator");
		
		TransitConfigGroup transit = controler.getConfig().transit();
		transit.setTransitScheduleFile(this.getTransitScheduleFile());
		transit.setVehiclesFile(this.getTransitVehiclesFile());
		
		ControlerConfigGroup controlerConfGroup = controler.getConfig().controler();
		controlerConfGroup.setFirstIteration(0);
		controlerConfGroup.setLastIteration(lastInternalIteration);
		controlerConfGroup.setWriteEventsInterval(1);
		controlerConfGroup.setWritePlansInterval(1);
		controlerConfGroup.setOutputDirectory(this.getDirectoryExtIt()+"/outputInternalSimulation");

//		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();			
//		BusCorridorCharyparNagelScoringFunctionFactory factory = new BusCorridorCharyparNagelScoringFunctionFactory(planCalcScoreConfigGroup);
//		controler.setScoringFunctionFactory(factory);
		
		controler.run();
	}

	private void writeVehiclesAndSchedule() throws IOException {
		
		this.iteration2numberOfBuses.put(this.getExtItNr(), this.numberOfBuses);
		
		this.setTransitScheduleFile(this.getDirectoryExtIt()+"/transitSchedule_it."+this.getExtItNr()+".xml");
		this.setTransitVehiclesFile(this.getDirectoryExtIt()+"/transitVehicles_it."+this.getExtItNr()+".xml");
		
		BusCorridorScheduleVehiclesGenerator generator = new BusCorridorScheduleVehiclesGenerator();
		generator.setStopTime(busStopTime);
		generator.setTravelTimeBus(travelTimeBus);
		generator.setNetworkFile(networkFile);
		generator.setScheduleFile(this.getTransitScheduleFile());
		generator.setVehicleFile(this.getTransitVehiclesFile());
		
		generator.setTransitLineId(new IdImpl("Bus Line"));
		generator.setRouteId1(new IdImpl("West-Ost"));
		generator.setRouteId2(new IdImpl("Ost-West"));
		
		generator.setVehTypeId(new IdImpl("Bus"));
		generator.setSeats(15);
		generator.setStandingRoom(20);
		
		generator.setNumberOfBusses(this.getNumberOfBuses()); // Anzahl der Busse
		generator.setStartTime(7*3600);
		generator.setEndTime(17*3600);
		
		generator.createVehicles();
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();
	}

	public void setNumberOfBuses(int numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}

	public int getNumberOfBuses() {
		return numberOfBuses;
	}

	public void setDirectoryExtIt(String directoryExtIt) {
		this.directoryExtIt = directoryExtIt;
	}

	public String getDirectoryExtIt() {
		return directoryExtIt;
	}

	public void setTransitScheduleFile(String transitScheduleFile) {
		this.transitScheduleFile = transitScheduleFile;
	}

	public String getTransitScheduleFile() {
		return transitScheduleFile;
	}

	public String getTransitVehiclesFile() {
		return transitVehiclesFile;
	}

	public void setTransitVehiclesFile(String transitVehiclesFile) {
		this.transitVehiclesFile = transitVehiclesFile;
	}

	public void setExtItNr(int extItNr) {
		this.extItNr = extItNr;
	}

	public int getExtItNr() {
		return extItNr;
	}

	public double getHighestScore() {
		return highestScore;
	}

	public void setHighestScore(double highestScore) {
		this.highestScore = highestScore;
	}

	public void setIterationWithHighestScore(int iterationWithHighestScore) {
		this.iterationWithHighestScore = iterationWithHighestScore;
	}

	public int getIterationWithHighestScore() {
		return iterationWithHighestScore;
	}
}
