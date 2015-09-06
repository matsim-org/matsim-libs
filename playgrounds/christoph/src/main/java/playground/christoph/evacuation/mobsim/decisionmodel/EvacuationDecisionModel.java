/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationDecisionModel.java
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

package playground.christoph.evacuation.mobsim.decisionmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfig.EvacuationDecisionBehaviour;
import playground.christoph.evacuation.config.EvacuationConfig.EvacuationReason;
import playground.christoph.evacuation.config.EvacuationConfig.PreEvacuationTime;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;

public class EvacuationDecisionModel implements HouseholdDecisionModel {
	
	private static final Logger log = Logger.getLogger(EvacuationDecisionModel.class);
	
	public static final String evacuationDecisionModelFile = "evacuationDecisionModel.txt.gz";
	
	public static enum EvacuationDecision {IMMEDIATELY, LATER, NEVER, UNDEFINED};
	public static enum Participating {TRUE, FALSE, UNDEFINED};
	
	private final Scenario scenario;
	private final Random random;
	private final DecisionDataProvider decisionDataProvider;
	
	private int laterCount = 0;
	private int immediatelyCount = 0;
	private int neverCount = 0;
	private int undefindedCount = 0;
	
	public EvacuationDecisionModel(Scenario scenario, Random random, DecisionDataProvider decisionDataProvider) {
		this.scenario = scenario;
		this.random = random;
		this.decisionDataProvider = decisionDataProvider;
	}
	
	@Override
	public void runModel(Household household) {
				
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId()); 
		
		/*
		 * If the EvacuationDecisionBehaviour is set to SHARE, only a subset of all households
		 * (defined by the share parameter) evacuates. Otherwise a model is used to select the
		 * participating households based on factors like the location of all members.
		 * This setup only supports the two possibilities evacuate immediately or never. 
		 */
		if (EvacuationConfig.evacuationDecisionBehaviour == EvacuationDecisionBehaviour.SHARE) {
			runSimpleModel(hdd);
		}
		/*
		 * Run evacuation decision model to determine the household's evacuation behaviour.
		 */
		else if (EvacuationConfig.evacuationDecisionBehaviour == EvacuationDecisionBehaviour.MODEL) {
			runSurveyBasedModel(household, hdd);
		} else {
			throw new RuntimeException("Unexpected EvacuationDecisionBehaviour was found " + EvacuationConfig.evacuationDecisionBehaviour);
		}
		
		/*
		 * Assign household's decisions (participate yes/no) also to all
		 * household members;
		 */
		Participating participating = hdd.getParticipating();
		for (Id<Person> agentId : household.getMemberIds()) {
			this.decisionDataProvider.getPersonDecisionData(agentId).setParticipating(participating);
		}	
	}

	@Override
	public void runModel(Households households) {
		for (Household household : households.getHouseholds().values()) runModel(household);
	}

	private void runSimpleModel(HouseholdDecisionData hdd) {
		if (this.random.nextDouble() <= EvacuationConfig.householdParticipationShare) {
			hdd.setEvacuationDecision(EvacuationDecision.IMMEDIATELY);
			hdd.setParticipating(Participating.TRUE);
		} else {
			hdd.setEvacuationDecision(EvacuationDecision.NEVER);
			hdd.setParticipating(Participating.FALSE);
		}
	}
	
	/*
	 * Determine evacuation decision based on model estimated on survey data by Koot and Kowald.
	 */
	private void runSurveyBasedModel(Household household, HouseholdDecisionData hdd) {
		
		/*
		 * Check whether household has children.
		 */
		boolean hasChildren = hdd.hasChildren();
		
		/*
		 * Check whether household is joined.
		 */
//		Id householdId = household.getId();
//		boolean isJoined = this.decisionDataProvider.getHouseholdDecisionData(householdId).getHouseholdPosition().isHouseholdJoined();
		HouseholdPosition hp = hdd.getHouseholdPosition();
		boolean isJoined = hp.isHouseholdJoined();
		
		/*
		 * Calculate pickup decision for every household member. Then decide base on
		 * a monte carlo simulation.
		 */
		int immediately = 0;
		int later = 0;
		int never = 0;
		for (Id<Person> personId : household.getMemberIds()) {
			Person person = scenario.getPopulation().getPersons().get(personId);
			int age = PersonUtils.getAge(person);
			boolean drivingLicense = PersonUtils.hasLicense(person);
			
			EvacuationDecision evacuationDecision = runSurveyBasedPersonModel(age, drivingLicense, hasChildren, isJoined);
			if (evacuationDecision == EvacuationDecision.IMMEDIATELY) immediately++;
			else if (evacuationDecision == EvacuationDecision.LATER) later++;
			else if (evacuationDecision == EvacuationDecision.NEVER) never++;
		}
		
		// Decide based on monte carlo simulation.
		double total = immediately + later + never;
		double rand = random.nextDouble();
		if (rand < (immediately / total)) {
			immediatelyCount++;
			hdd.setEvacuationDecision(EvacuationDecision.IMMEDIATELY);
			hdd.setParticipating(Participating.TRUE);
		}else if (rand < ((immediately + later) / total)) {
			laterCount++;
			hdd.setEvacuationDecision(EvacuationDecision.LATER);
			hdd.setParticipating(Participating.TRUE);
		} else {
			neverCount++;
			hdd.setEvacuationDecision(EvacuationDecision.NEVER);
			hdd.setParticipating(Participating.FALSE);
		}
	}
	
	private EvacuationDecision runSurveyBasedPersonModel(int age, boolean drivingLicense, boolean hasChildren, boolean joined) {
		
		int is31To60 = 0;
		int is61plus = 0;
		int children = 0;
		int license = 0;
		int isAtomic = 0;
		int isFire = 0;
		int isChemical = 0;
		int isTime8 = 0;
		int isTime16 = 0;
		int isJoined = 0;
			
		if (age >= 31 && age <= 60) is31To60 = 1;
		else if (age >= 61) is61plus = 1;
		
		if (hasChildren) children = 1;
		
		if (drivingLicense) license = 1;
		
		if (joined) isJoined = 1;
		
		EvacuationReason reason = EvacuationConfig.leaveModelEvacuationReason;
		if (reason == EvacuationConfig.EvacuationReason.ATOMIC) isAtomic = 1;
		else if (reason == EvacuationConfig.EvacuationReason.FIRE) isFire = 1;
		else if (reason == EvacuationConfig.EvacuationReason.CHEMICAL) isChemical = 1;
		
		/*
		 * TODO for later studies: check whether linear interpolation for the
		 * PreEvacuationTime is possible.
		 */
		PreEvacuationTime time = EvacuationConfig.leaveModelPreEvacuationTime;
		if (time == EvacuationConfig.PreEvacuationTime.TIME8) isTime8 = 1;
		else if (time == EvacuationConfig.PreEvacuationTime.TIME16) isTime16 = 1;
				
		double V1 = EvacuationConfig.leaveModelImmediatelyConst + 
				isAtomic * EvacuationConfig.leaveModelImmediatelyAtomic +
				isChemical * EvacuationConfig.leaveModelImmediatelyChemical +
				isFire * EvacuationConfig.leaveModelImmediatelyFire + 
				is31To60 * EvacuationConfig.leaveModelImmediatelyAge31to60 +
				is61plus * EvacuationConfig.leaveModelImmediatelyAge61plus +
				isTime8 * EvacuationConfig.leaveModelImmediatelyTime8 * (1 + isJoined * EvacuationConfig.leaveModelImmediatelyHouseholdUnited1) +
				isTime16 * EvacuationConfig.leaveModelImmediatelyTime16 * (1 + isJoined * EvacuationConfig.leaveModelImmediatelyHouseholdUnited2) +			
				children * EvacuationConfig.leaveModelHasChildren +
				license * EvacuationConfig.leaveModelHasDrivingLicense;
		
		double V2 = EvacuationConfig.leaveModelLaterConst + 
				isAtomic * EvacuationConfig.leaveModelLaterAtomic +
				isChemical * EvacuationConfig.leaveModelLaterChemical +
				isFire * EvacuationConfig.leaveModelLaterFire + 
				is31To60 * EvacuationConfig.leaveModelLaterAge31to60 +
				is61plus * EvacuationConfig.leaveModelLaterAge61plus +
				isTime8 * EvacuationConfig.leaveModelLaterTime8 * (1 + isJoined * EvacuationConfig.leaveModelLaterHouseholdUnited1) +
				isTime16 * EvacuationConfig.leaveModelLaterTime16 * (1 + isJoined * EvacuationConfig.leaveModelLaterHouseholdUnited2) +			
				children * EvacuationConfig.leaveModelHasChildren +
				license * EvacuationConfig.leaveModelHasDrivingLicense;
		
		double V3 = 0;
		
		double ExpV = Math.exp(V1) + Math.exp(V2) + Math.exp(V3);
		
		double immediately = Math.exp(V1) / ExpV;
		double later = Math.exp(V2) / ExpV;
//		double never = Math.exp(V3) / ExpV;
		
		double rand = random.nextDouble();
		if (rand < immediately) return EvacuationDecision.IMMEDIATELY;
		else if (rand < immediately + later) return EvacuationDecision.LATER;
		else return EvacuationDecision.NEVER;
	}
	
	@Override
	public void printStatistics() {
		log.info("evacuation decisions:");
		log.info("immediately\t" + immediatelyCount);
		log.info("later\t" + laterCount);
		log.info("never\t" + neverCount);
		log.info("undefined\t" + undefindedCount);
	}

	@Override
	public void writeDecisionsToFile(String file) {
		
		try {
			BufferedWriter modelWriter = IOUtils.getBufferedWriter(file);
			
			writeHeader(modelWriter);
			writeRows(modelWriter);
			
			modelWriter.flush();
			modelWriter.close();			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeHeader(BufferedWriter modelWriter) throws IOException {
		modelWriter.write("householdId");
		modelWriter.write(delimiter);
		modelWriter.write("evacuation decision");
		modelWriter.write(newLine);
	}
	
	private void writeRows(BufferedWriter modelWriter) throws IOException {
		for (HouseholdDecisionData hdd : this.decisionDataProvider.getHouseholdDecisionData()) {
			modelWriter.write(hdd.getHouseholdId().toString());
			modelWriter.write(delimiter);
			modelWriter.write(hdd.getEvacuationDecision().toString());
			modelWriter.write(newLine);
		}
	}

	@Override
	public void readDecisionsFromFile(String file) {
		try {
			BufferedReader modelReader = IOUtils.getBufferedReader(file);
			
			readHeader(modelReader);
			readRows(modelReader);
			
			modelReader.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}
	
	private void readHeader(BufferedReader modelReader) throws IOException {
		// just skip header
		modelReader.readLine();
	}
	
	private void readRows(BufferedReader modelReader) throws IOException {
		
		String line = null;
		while ((line = modelReader.readLine()) != null) {
			String[] columns = line.split(delimiter);
			Id<Household> householdId = Id.create(columns[0], Household.class);
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			if (columns[1].equals(EvacuationDecision.IMMEDIATELY.toString())) {
				hdd.setEvacuationDecision(EvacuationDecision.IMMEDIATELY);
				hdd.setParticipating(Participating.TRUE);
				immediatelyCount++;
			} else if (columns[1].equals(EvacuationDecision.LATER.toString())) {
				hdd.setEvacuationDecision(EvacuationDecision.LATER);
				hdd.setParticipating(Participating.TRUE);
				laterCount++;
			} else if (columns[1].equals(EvacuationDecision.NEVER.toString())) {
				hdd.setEvacuationDecision(EvacuationDecision.NEVER);
				hdd.setParticipating(Participating.FALSE);
				neverCount++;
			} else if (columns[1].equals(EvacuationDecision.UNDEFINED.toString())) {
				hdd.setEvacuationDecision(EvacuationDecision.UNDEFINED);
				hdd.setParticipating(Participating.UNDEFINED);
				undefindedCount++;
				log.warn("Found household with undefined evacuation decision: " + columns[0]);
			} else throw new RuntimeException("Could not parse households's evacuation decision: " + line);
			
			/*
			 * Assign household's decisions (participate yes/no) also to all
			 * household members;
			 */
			Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
			Participating participating = hdd.getParticipating();
			for (Id<Person> agentId : household.getMemberIds()) {
				this.decisionDataProvider.getPersonDecisionData(agentId).setParticipating(participating);
			}
		}
	}
	
//	/*
//	 * TODO: move this code to a test case.
//	 */
//	public static void main(String[] args) {
//		if (args.length == 0) return;
//		
//		Config config = ConfigUtils.loadConfig(args[0]);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		new EvacuationConfigReader().readFile(args[1]);
//
//		ObjectAttributes householdObjectAttributes = new ObjectAttributes();
//		new ObjectAttributesXmlReader(householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
//		
//		EvacuationConfig.evacuationTime = 0.0;
//		config.getQSimConfigGroup().setEndTime(1.0);
//		
//		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
//		SHPFileUtil util = new SHPFileUtil();
//		for (String file : EvacuationConfig.evacuationArea) features.addAll(ShapeFileReader.getAllFeatures(file));
//		Geometry affectedArea = util.mergeGeometries(features);
//		
//		CoordAnalyzer coordAnalyzer = new CoordAnalyzer(affectedArea);
//		HouseholdsTracker householdsTracker = new HouseholdsTracker(scenario);
//		DecisionDataProvider decisionDataProvider = new DecisionDataProvider();
//		
//		/*
//		 * Create a DecisionDataGrabber and run notifyMobsimInitialized(...)
//		 * which inserts decision data into the DecisionDataProvider.
//		 */
//		DecisionDataGrabber decisionDataGrabber = new DecisionDataGrabber(scenario, decisionDataProvider, coordAnalyzer, 
//				householdsTracker, householdObjectAttributes);	
////		decisionDataGrabber.notifyMobsimInitialized(null);
//		
//		Mobsim mobsim = new QSimFactory().createMobsim(scenario, EventsUtils.createEventsManager());
//		// SimulationListeners are fired in reverse order! Therefore add householdsTracker after decisionDataGrabber.
//		((QSim) mobsim).addQueueSimulationListeners(decisionDataGrabber);
//		((QSim) mobsim).addQueueSimulationListeners(householdsTracker);
//		mobsim.run();
//		
//		EvacuationDecisionModel model = new EvacuationDecisionModel(scenario, MatsimRandom.getLocalInstance(), decisionDataProvider);
//		model.runModel(((ScenarioImpl) scenario).getHouseholds());
//		model.printStatistics();		
//	}
}