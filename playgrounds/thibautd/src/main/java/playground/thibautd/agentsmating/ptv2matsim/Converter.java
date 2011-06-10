/* *********************************************************************** *
 * project: org.matsim.*
 * Converter.java
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
package playground.thibautd.agentsmating.ptv2matsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.agentsmating.greedysavings.FacilitiesFactory;
import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * Takes PTV carpool data and a Matsim population, and create a population
 * with cliques and joint trips.
 *
 * @author thibautd
 */
public class Converter {
	private static final Logger log =
		Logger.getLogger(Converter.class);


	private static final String SPLIT_EXPR = "\t";
	// fields position
	private static final int DRIVER = 0;
	private static final int DRIVER_LEG = 1;
	private static final int PASSENGER = 2;
	private static final int PASSENGER_LEG = 3;

	private final Random random = new Random();
	private final IdFactory cliqueIdFactory = new IdFactory();
	private final PuNumberFactory puNumberFactory = new PuNumberFactory();

	private final BufferedReader ptvData;
	private final int popSize;
	private final Population population;
	private final ScenarioImpl scenario;
	private final Map<Id, List<Id>> cliques;
	private final Map<Id, List<Id>> associations;
	private final Map<Id, List<PlanElement>> plansToConstruct;
	private final List<Id> unaffectedAgents;
	private final FacilitiesFactory facilitiesFactory;
	private final boolean useFacilities;

	// ////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////
	/**
	 * Constructs the converter and processes mating data.
	 */
	public Converter(
			final BufferedReader ptvData,
			final ScenarioImpl scenario
			) throws IOException {
		this.ptvData = ptvData;
		// skip first line.
		// could also be used to detect interesting columns
		this.ptvData.readLine();
		this.scenario = scenario;
		this.population = scenario.getPopulation();
		this.popSize = population.getPersons().size();
		this.cliques = new HashMap<Id, List<Id>>(popSize);
		this.associations = new HashMap<Id, List<Id>>(popSize);
		this.unaffectedAgents = new ArrayList<Id>(popSize);
		this.plansToConstruct = new HashMap<Id, List<PlanElement>>(popSize);

		if (scenario.getConfig().facilities().getInputFile() == null) {
			log.debug("facility creation disabled");
			this.facilitiesFactory = null;
			this.useFacilities = false;
		}
		else {
			log.debug("facility creation enabled");
			this.facilitiesFactory =
				new FacilitiesFactory(
						scenario.getActivityFacilities(),
						scenario.getNetwork());
			this.useFacilities = true;
		}

		initInternalStructures();
		processData();
	}

	private void initInternalStructures() {
		Id id;
		Plan plan;
		for (Map.Entry<Id, ? extends Person> person :
				this.population.getPersons().entrySet()) {
			id = person.getKey();
			plan = person.getValue().getSelectedPlan();
			this.unaffectedAgents.add(id);
			this.plansToConstruct.put(
					id,
					new ArrayList<PlanElement>(plan.getPlanElements()));
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// mating
	// /////////////////////////////////////////////////////////////////////////

	private void processData() {
		String[] data;
		Id driver;
		int driverLeg;
		Id passenger;
		int passengerLeg;

		for (String currentLine = getNextDataLine();
				currentLine != null;
				currentLine =  getNextDataLine()) {
			data = currentLine.split(SPLIT_EXPR);
			driver = new IdImpl(data[DRIVER]);
			driverLeg = getLegIndex(data[DRIVER_LEG]);
			passenger = new IdImpl(data[PASSENGER]);
			passengerLeg = getLegIndex(data[PASSENGER_LEG]);

			markSharedRide(driver, driverLeg, passenger, passengerLeg);

			// update cliques
			updateCliques(driver, passenger);
		}

		constructCliques();
	}

	private int getLegIndex(String tripId) {
		int i = Integer.valueOf(tripId);
		return i*2 - 1;
	}

	private void markSharedRide(
			final Id driver,
			final int driverLeg,
			final Id passenger,
			final int passengerLeg) {
		String currentPu = this.puNumberFactory.createNumber();
		List<PlanElement> planPassenger = this.plansToConstruct.get(passenger);
		Activity origin = (Activity) planPassenger.get(passengerLeg - 1);
		Activity destination = (Activity) planPassenger.get(passengerLeg + 1);

		// mark the driver's plan
		this.plansToConstruct.get(driver).set(
				driverLeg,
				new SharedRide(true, passenger, currentPu, origin, destination));

		// mark the passenger's plan
		planPassenger.set(
				passengerLeg,
				new SharedRide(false, driver, currentPu, origin, destination));

	}

	/**
	 * Update the links between agent (do not construct clique yet).
	 */
	private void updateCliques(final Id driver, final Id passenger) {
		getMates(driver).add(passenger);
		getMates(passenger).add(driver);
	}

	/**
	 * get the agents the agent is currently linked to.
	 */
	private List<Id> getMates(final Id agent) {
		List<Id> mating = this.associations.get(agent);

		if (mating == null) {
			mating = new ArrayList<Id>();
			this.associations.put(agent, mating);
		}

		return mating;
	}

	/**
	 * Actually construct cliques from link info.
	 */
	private void constructCliques() {
		List<Id> clique;
		Id root;
		int count = 0;
		int next = 1;

		while (true) {
			count++;
			if (count == next) {
				log.debug("constructing clique # "+count);
				next *= 2;
			}
	
			try {
				root = this.unaffectedAgents.remove(0);
			} catch (IndexOutOfBoundsException e) {
				// there is nobody more: exit
				break;
			}

			clique = new ArrayList<Id>();
			inDepthSearch(root, clique);
			addClique(clique);
		}
		log.debug(count+" cliques constructed");
	}

	/**
	 * Adds the clique to the clique datastruct
	 */
	private void addClique(final List<Id> clique) {
		this.unaffectedAgents.removeAll(clique);
		this.cliques.put(this.cliqueIdFactory.createId(), clique);
	}

	/**
	 * Performs an in depth search to construct cliques.
	 */
	private void inDepthSearch(final Id node, final List<Id> marks) {
		//log.debug("marking node");
		marks.add(node);
		
		for (Id neighboor : getMates(node)) {
			if (!marks.contains(neighboor)) {
				inDepthSearch(neighboor, marks);
			}
		}
	}

	private String getNextDataLine() {
		try {
			return this.ptvData.readLine();
		} catch (IOException e) {
			throw new RuntimeException("error while proccessing data", e);
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// Plans construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Constructs the joint plans for the individuals in the sample population.
	 */
	private void constructPlans(
			//final Map<Id, List<Id>> sampleCliques,
			final Population samplePopulation) {
		Id id;
		Person person;
		Plan plan;
		int count = 0;
		int next = 1;

		for (Map.Entry<Id, ? extends Person> entry :
				samplePopulation.getPersons().entrySet()) {
			count++;
			if (count == next) {
				log.debug("constructing individual plan # "+count);
				next *= 2;
			}
			id = entry.getKey();
			person = entry.getValue();
			// clear person plans and modify the selected plan
			((PersonImpl) person).removeUnselectedPlans();
			plan = person.getSelectedPlan();
			constructPlan(id, plan);
		}
		log.debug(count+" individuals processed");
	}

	private void constructPlan(final Id id, final Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		List<PlanElement> planToConstruct = this.plansToConstruct.get(id);

		planElements.clear();

		for (PlanElement pe : planToConstruct) {
			//log.debug("adding plan element");
			if (pe instanceof SharedRide) {
				planSharedRide((SharedRide) pe, planElements);
			}
			else {
				planElements.add(pe);
			}
		}
	}

	private void planSharedRide(
			final SharedRide sharedRide,
			final List<PlanElement> plan) {
		String accessMode;
		String sharedMode;
		Activity origin = sharedRide.origin;
		Activity destination = sharedRide.destination;
		Activity pickUp, dropOff;

		// mode selection
		if (sharedRide.isDriver) {
			accessMode = TransportMode.car;
			sharedMode = TransportMode.car;
		}
		else {
			accessMode = TransportMode.walk;
			sharedMode = JointActingTypes.PASSENGER;
		}

		// activities construction
		pickUp = new ActivityImpl(
					sharedRide.puName,
					origin.getCoord(),
					origin.getLinkId());
		dropOff = new ActivityImpl(
					JointActingTypes.DROP_OFF,
					destination.getCoord(),
					destination.getLinkId());

		if (useFacilities) {
			((ActivityImpl) pickUp).setFacilityId(
					this.facilitiesFactory.getPickUpDropOffFacility(
						pickUp.getLinkId()));
			((ActivityImpl) dropOff).setFacilityId(
					this.facilitiesFactory.getPickUpDropOffFacility(
						dropOff.getLinkId()));
		}

		plan.add(new LegImpl(accessMode));
		plan.add(pickUp);
		plan.add(new LegImpl(sharedMode));
		plan.add(dropOff);
		plan.add(new LegImpl(accessMode));
	}


	// /////////////////////////////////////////////////////////////////////////
	// getter and population sampling
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Writes a sample population to files.
	 * The sample is stratified according to clique size.
	 * 
	 * @param filePath the path in which files will be written.
	 * @param sampleRate a double in [0,1], specifying the proportion of
	 * cliques to keep in the sample population.
	 */
	public void write(final String filePath, final double sampleRate) {
		Map<Id, List<Id>> sampleCliques = getSampleCliques(sampleRate);

		Population samplePopulation = getSamplePopulation(sampleCliques);
		//constructPlans(sampleCliques, samplePopulation);
		constructPlans(samplePopulation);

		write(filePath, sampleRate, sampleCliques, samplePopulation);
	}

	private void write(
			final String filePath,
			final double sampleRate,
			final Map<Id, List<Id>> sampleCliques,
			final Population samplePopulation) {
		String endName =  sampleRate +"-sample.xml.gz";
		String cliqueFile = filePath + "/cliques-" + endName;
		String populationFile = filePath + "/plans-" + endName;
		String facilitiesFile = filePath + "/facilities-" + endName;

		File outputDir = new File(filePath);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		(new CliquesWriter(sampleCliques)).writeFile(cliqueFile);
		log.debug("writting population of size "+samplePopulation.getPersons().size());
		(new PopulationWriter(samplePopulation, this.scenario.getNetwork()))
			.write(populationFile);

		if (useFacilities) {
			this.facilitiesFactory.write(facilitiesFile);
		}
	}

	/**
	 * Creates a population containing only the persons in the sample
	 */
	private Population getSamplePopulation(final Map<Id, List<Id>> sampleCliques) {
		Population output = new PopulationImpl(this.scenario);
		Map<Id, ? extends Person> persons = this.population.getPersons();

		for (List<Id> clique : sampleCliques.values()) {
			for (Id person : clique) {
				output.addPerson(persons.get(person));
			}
		}

		return output;
	}

	private Map<Id, List<Id>> getSampleCliques(final double sampleRate) {
		List<Id> ids = getSampleCliquesIds(sampleRate);
		Map<Id, List<Id>> newCliques = new HashMap<Id, List<Id>>(ids.size());

		for (Id id : ids) {
			newCliques.put(id, this.cliques.get(id));
		}

		return newCliques;
	}

	/**
	 * gets a sample of the ids of the cliques
	 */
	private List<Id> getSampleCliquesIds(final double sampleRate) {
		Map<Integer, List<Id>> stratas = getStratas();
		List<Id> sample = new ArrayList<Id>((int) Math.ceil(sampleRate * popSize));
		int strataSize;
		int toDraw;
		int index;
		int cliqueSize;
		List<Id> strata;
		int drawn = 0;

		for (Map.Entry<Integer, List<Id>> entry : stratas.entrySet()) {
			strata = entry.getValue();
			strataSize = strata.size();
			cliqueSize = entry.getKey();
			toDraw = (int) Math.ceil((strataSize * sampleRate)/cliqueSize);
			drawn += toDraw;

			log.debug("drawing "+toDraw+" from the "+strataSize+
					" cliques of size "+cliqueSize);

			// draw a number of elements proportionnal to the strata size,
			// without replacement.
			for (int i=0;
					i < toDraw;
					i++) {
				index = this.random.nextInt(strataSize - i);
				sample.add(strata.remove(index));
			}
		}

		log.debug(drawn+" cliques in the sample");

		return sample;
	}

	/**
	 * @return a list of the stratas, under the form of lists of clique's ids.
	 */
	private Map<Integer, List<Id>> getStratas() {
		Map<Integer, List<Id>> tempStratas = new HashMap<Integer, List<Id>>();
		int size;
		List<Id> strata;

		// TODO: count and print stats
		for (Map.Entry<Id, List<Id>> clique : this.cliques.entrySet()) {
			size = clique.getValue().size();
			strata = tempStratas.get(size);

			if (strata == null) {
				strata = new ArrayList<Id>();
				tempStratas.put(size, strata);
			}

			strata.add(clique.getKey());
		}

		logStrataInfo(tempStratas);

		return tempStratas;
	}

	private void logStrataInfo(final Map<Integer, List<Id>> stratas) {
		log.debug("Strata sizes:");

		for (Map.Entry<Integer, List<Id>> entry : stratas.entrySet()) {
			log.debug("size "+entry.getKey()+": "+entry.getValue().size()+
					" cliques in the whole population");
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// classes
	// //////////////////////////////////////////////////////////////////////////
	private class SharedRide implements PlanElement {
		public final boolean isDriver;
		public final Id mate;
		public final String puName;
		public final Activity origin;
		public final Activity destination;

		public SharedRide(
				final boolean isDriver,
				final Id mate,
				final String puName,
				final Activity origin,
				final Activity destination) {
			this.isDriver = isDriver;
			this.mate = mate;
			this.puName = puName;
			this.origin = origin;
			this.destination = destination;
		}
	}

	private class PuNumberFactory {
		private long current = 0;

		public String createNumber() {
			current++;
			return JointActingTypes.PICK_UP_BEGIN +
				JointActingTypes.PICK_UP_SPLIT_EXPR +
				current;
		}
	}

	private class IdFactory {
		private long current = 0;

		public Id createId() {
			current++;
			return new IdImpl(current);
		}
	}
}

