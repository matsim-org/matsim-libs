/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToPickupModel.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.PersonDecisionData;

/**
 * Decides which agents would pick up other agents and which ones would not.
 * 
 * @author cdobler
 */
public class PickupModel implements PersonDecisionModel {
	
	private static final Logger log = Logger.getLogger(PickupModel.class);
	
	public static final String pickupModelFile = "pickupModel.txt.gz";
	
	public enum PickupDecision {IFSPACE, ALWAYS, NEVER, UNDEFINED};	// undefined is used for people younger than 18 -> no driving license
	
	private final DecisionDataProvider decisionDataProvider;
	private final Random random;
	
	private int ifSpace = 0;
	private int always = 0;
	private int never = 0;
	private int undefined = 0;
	
	public PickupModel(DecisionDataProvider decisionDataProvider) {
		
		this.decisionDataProvider = decisionDataProvider;
		this.random = MatsimRandom.getLocalInstance();
	}
	
	/*
	 * Print statistics
	 * 
	 * Survey results:
	 * if space ... 41%
	 * always ... 57%
	 * never ... 2%
	 */
	@Override
	public void printStatistics() {
		log.info("pickup decisions:");
		log.info("if space\t" + ifSpace);
		log.info("always\t" + always);
		log.info("never\t" + never);
		log.info("undefined\t" + undefined);
	}
	
	@Override
	public void runModel(Person person) {
		Person p = person;
		PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(person.getId());
		
		int age = PersonImpl.getAge(p);
		String sex = PersonImpl.getSex(p);
		boolean drivingLicense = PersonImpl.hasLicense(p);
		boolean hasChildren = pdd.hasChildren();
		
		PickupDecision pickupDecision = runModel(age, drivingLicense, sex, hasChildren);
		pdd.setPickupDecision(pickupDecision);
		
		if (pickupDecision == PickupDecision.IFSPACE) ifSpace++;
		else if (pickupDecision == PickupDecision.ALWAYS) always++;
		else if (pickupDecision == PickupDecision.NEVER) never++;
		else if (pickupDecision == PickupDecision.UNDEFINED) undefined++;
	}
	
	@Override
	public void runModel(Population population) {
		for (Person person : population.getPersons().values()) runModel(person);
	}
	
	private PickupDecision runModel(int age, boolean drivingLicense, String sex, boolean hasChildren) {
		
		int is31To60 = 0;
		int is61To70 = 0;
		int is71plus = 0;
		int children = 0;
		int license = 0;
		int isFemale = 0;
		
		// People under 18 cannot pickup other people since they have no driving license
		if (age < 18) return PickupDecision.UNDEFINED;
		
		if (age >= 31 && age <= 60) is31To60 = 1;
		else if (age >= 61 && age <= 70) is61To70 = 1;
		else if (age >= 71) is71plus = 1;
		
		if (hasChildren) children = 1;
		
		if (drivingLicense) license = 1;
		
		if (sex.toLowerCase().equals("f")) isFemale = 1;
		
		double V1 = EvacuationConfig.pickupModelIfSpaceConst + 
				is31To60 * EvacuationConfig.pickupModelIfSpaceAge31to60 +
				is61To70 * EvacuationConfig.pickupModelIfSpaceAge61to70 +
				is71plus * EvacuationConfig.pickupModelIfSpaceAge71plus +
				children * EvacuationConfig.pickupModelIfSpaceHasChildren +
				license * EvacuationConfig.pickupModelIfSpaceHasDrivingLicence +
				isFemale * EvacuationConfig.pickupModelIfSpaceIsFemale;
		
		double V2 = EvacuationConfig.pickupModelAlwaysConst + 
				is31To60 * EvacuationConfig.pickupModelAlwaysAge31to60 +
				is61To70 * EvacuationConfig.pickupModelAlwaysAge61to70 +
				is71plus * EvacuationConfig.pickupModelAlwaysAge71plus +
				children * EvacuationConfig.pickupModelAlwaysHasChildren +
				license * EvacuationConfig.pickupModelAlwaysHasDrivingLicence +
				isFemale * EvacuationConfig.pickupModelAlwaysIsFemale;
		
		double V3 = 0;
		
		double ExpV = Math.exp(V1) + Math.exp(V2) + Math.exp(V3);
		
		double ifSpace = Math.exp(V1) / ExpV;
		double always = Math.exp(V2) / ExpV;
//		double never = Math.exp(V3) / ExpV;
		
		double rand = random.nextDouble();
		if (rand < ifSpace) return PickupDecision.IFSPACE;
		else if (rand < ifSpace + always) return PickupDecision.ALWAYS;
		else return PickupDecision.NEVER;
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
		modelWriter.write("personId");
		modelWriter.write(delimiter);
		modelWriter.write("pickup behavior");
		modelWriter.write(newLine);
	}
	
	private void writeRows(BufferedWriter modelWriter) throws IOException {
		for (PersonDecisionData pdd : this.decisionDataProvider.getPersonDecisionData()) {
			modelWriter.write(pdd.getPersonId().toString());
			modelWriter.write(delimiter);
			modelWriter.write(pdd.getPickupDecision().toString());
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
			Id<Person> personId = Id.create(columns[0], Person.class);
			if (columns[1].equals(PickupDecision.ALWAYS.toString())) {
				this.decisionDataProvider.getPersonDecisionData(personId).setPickupDecision(PickupDecision.ALWAYS);
				this.always++;
			} else if (columns[1].equals(PickupDecision.IFSPACE.toString())) {
				this.decisionDataProvider.getPersonDecisionData(personId).setPickupDecision(PickupDecision.IFSPACE);
				this.ifSpace++;
			} else if (columns[1].equals(PickupDecision.NEVER.toString())) {
				this.decisionDataProvider.getPersonDecisionData(personId).setPickupDecision(PickupDecision.NEVER);
				this.never++;
			} else if (columns[1].equals(PickupDecision.UNDEFINED.toString())) {
				this.decisionDataProvider.getPersonDecisionData(personId).setPickupDecision(PickupDecision.UNDEFINED);
				this.undefined++;
			} else throw new RuntimeException("Could not parse person's pickup decision: " + line);
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
//		decisionDataGrabber.notifyMobsimInitialized(null);
//		
//		PickupModel pickupModel = new PickupModel(decisionDataProvider);
//		pickupModel.runModel(scenario.getPopulation());
//		pickupModel.printStatistics();
//	}

}