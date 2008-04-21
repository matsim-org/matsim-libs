/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationGeneratorControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.ktiProject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.XY2LinksMultithreaded;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.world.Location;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

/**
 * Class that combines several method calls in order to generate a population
 * from the raw data of the census 2000 of Switzerland on the one hand and from the
 * data from Datapuls.
 * @author lnicolas
 */
public class PopulationGeneratorControler {

	/**
	 * All input data must be in the given folder
	 */
	private final static String inputFolder = "/data/matsim-t/lnicolas/data/";
	/**
	 * Path to the folder where the output data is written to
	 */
	private final static String outputFolder =  "/data/matsim-t/lnicolas/";

	private Plans plans;
	private ArrayList<Person> population;
	private ArrayList<HouseholdI> households;
	private ArrayList<Zone> homeZones;
	private final ArrayList<Zone> referenceZones;
	private final TreeMap<Id, MunicipalityInformation> municipalityInfo;
	private final ArrayList<Zone> primaryActZones;
	private final GroupFacilitiesPerZone facsPerZone;
	private final NetworkLayer network;

	/**
	 * The facilities (entreprise census plus home facilities of switzerland),
	 * the world (mapping the facilities to zones, i.e. municipalities) and the commuter
	 * matrices describing where people living within a specific municipality go to work
	 * must exist before calling this constructor!
	 *
	 */
	public PopulationGeneratorControler(final NetworkLayer network) {

		this.network = network;
		Facilities facilities = (Facilities) Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);

		/**
		 * Get the ZoneLayer containing the municipalities
		 */
		this.referenceZones = new ArrayList((Gbl.getWorld().getLayer(
				new IdImpl("municipality"))).getLocations().values());

		/**
		 * Read additional information per municipality from the given input file
		 */
		this.municipalityInfo = MunicipalityInformation.readTabbedMunicipalityInfo(
				inputFolder + "world_ch_gem.xml.txt", this.referenceZones);
		this.homeZones = new ArrayList<Zone>();
		this.primaryActZones = new ArrayList<Zone>();

		/**
		 * Add only those zones to the set of initial home zones (i.e. zones where
		 * people live) for which there exist additional information
		 */
		for (Zone zone : this.referenceZones) {
			if (this.municipalityInfo.containsKey(zone.getId())) {
				this.homeZones.add(zone);
			}
		}

		/**
		 * Group facilities per zone for faster lookup of facilities per zone
		 */
		this.facsPerZone = new GroupFacilitiesPerZone();
		System.out.println("Building QuadTree of facility coords for "
				+ "each zone...");
		this.facsPerZone.run(this.referenceZones, facilities);
		System.out.println("done");

		/**
		 * Get the layer that connects the each facility to the zones it lies in.
		 */
		ZoneLayer facilityLocations =
			(ZoneLayer) Gbl.getWorld().getLayer(new IdImpl("facility"));
		// A Facility and the corresponding Facility Location (enclosing the given Facility)
		// have the same Id
		Location location = null;
		for (Location facility : facilities.getLocations().values()) {
			location = facilityLocations.getLocation(facility.getId());
			if (location == null) {
				Gbl.errorMsg("There exists no facility location for facility " + facility.getId());
			}
			((Facility) facility).setLocation(location);
		}
	}

	/**
	 * Generates an population out of the datapuls 2006 information by
	 * upsampling the population to the same amount of people as in the census 2000 and
	 * adding several information to each person, like work and home locations,
	 * initial activity chains, mobility tools etc.
	 */
	public Plans generateCompleteUpsampledDatapuls2006Population() {
		generateInitialUpsampledDatapuls2006Population();
		enrichInitialPopulation("datapuls2006");
		return this.plans;
	}

	/**
	 * Generates an population out of the census 2000 information by adding several
	 * information to each person, like work and home locations, initial activity chains,
	 * mobility tools etc.
	 */
	public Plans generateCompleteCensus2000Population() {
		generateInitialCensus2000Population();
		enrichInitialPopulation("census2000");
		return this.plans;
	}

	private void enrichInitialPopulation(final String popId) {
		writePlans(this.plans, outputFolder + "population" + popId + "_noMobilityInfo.xml.gz");
		addMobilityInformationToPopulation();
		scaleDownGAOwnershipFraction(0.09);
		writePlans(this.plans, outputFolder + "population" + popId + "_noActChains.xml.gz");
		addActivityChainsToPopulation();
		setAllLegModesToCar(this.plans);
		writePlans(this.plans, outputFolder + "population" + popId + "_noHomeActCoords.xml.gz");
		mapHomeActivitiesToFacilities();
		writePlans(this.plans, outputFolder + "population" + popId + "_noPrimaryActCoords.xml.gz");
		mapPrimaryActivitiesToFacilities();
		writePlans(this.plans, outputFolder + "population" + popId + "_no2ndaryActCoords.xml.gz");
		mapSecondaryActivitiesToFacilities();
		writePlans(this.plans, outputFolder + "population" + popId + "_noLinks.xml.gz");
		mapActCoordsToLinks(this.network, this.plans);
		writePlans(this.plans, outputFolder + "population" + popId + "_noRoutes.xml.gz");
		createInitialRoutes(this.network, this.plans);
		writePlans(this.plans, outputFolder + "population" + popId + "_complete.xml.gz");
	}

	/**
	 * Generates an initial population out of the datapuls information by
	 * upsampling the population to the same amount of people as in the census 2000.
	 */
	public Plans generateInitialUpsampledDatapuls2006Population() {
		/**
		 * Generate an initial population out of the datapuls information
		 */
		DatapulsPopulationGenerator popGen = new DatapulsPopulationGenerator(inputFolder);
		this.plans = popGen.run();
		this.population = popGen.getPersons();
		this.households = popGen.getHouseholds();

		/**
		 * Map each person to a home zone
		 */
		this.homeZones = MobilityResourceGenerator.mapPersonsToZones(this.population, this.homeZones);

		return this.plans;
	}

	/**
	 * Generates an initial population out of the datapuls information, without
	 * upsampling the population, i.e. only keep those persons for which
	 * there exist valid data in the input dataset
	 */
	public Plans generateInitialDatapuls2006Population() {
		DatapulsPopulationGenerator popGen = new DatapulsPopulationGenerator(inputFolder);
		this.plans = popGen.runWithoutUpsampling();
		this.population = popGen.getPersons();
		this.households = popGen.getHouseholds();
		this.homeZones = MobilityResourceGenerator.mapPersonsToZones(this.population,
				this.homeZones);

		return this.plans;
	}

	/**
	 * Generates an initial population out of the census 2000 information.
	 */
	public Plans generateInitialCensus2000Population() {
		/**
		 * Generate an initial population out of the census information
		 */
		CensusPopulationGenerator popGen = new CensusPopulationGenerator(inputFolder);
		this.plans = popGen.run();
		this.population = popGen.getPersons();
		this.households = popGen.getHouseholds();

		/**
		 * Map each person to a home zone (ignoring zones for which there exists no
		 * income information) and add income information (based on its home zone) to it
		 */
		this.homeZones = Income2000Generator.mapPersonsToZones(this.population,
				this.homeZones, this.municipalityInfo);
		addCensusIncomeInformationToPopulation();

		return this.plans;
	}

	/**
	 * Sets a person's income to the average income of the zone (municipality) it lives in
	 * @param population Input persons
	 * @param households For each person, the household he/she is part of
	 * @param zones For each person the zone he/she lives in
	 * @param world
	 * @param incomeInfo Additional information for each zone
	 */
	private void addCensusIncomeInformationToPopulation() {
		System.out.println("Adding municipality income information to population...");

		Income2000Generator incInfGen = new Income2000Generator(Gbl.getWorld(),
				this.municipalityInfo);
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		// calculate (add up) the household income from the personal incomes
		for (int i = 0; i < this.population.size(); i++) {
			if (this.population.get(i).getEmployed().equals("yes")) {
				double income = incInfGen.getIncome2000(this.population.get(i), this.homeZones.get(i));
				HouseholdI hInfo = this.households.get(i);
				hInfo.setIncome(income + hInfo.getIncome());
			}

			if (i % (this.population.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		System.out.println("done");
	}

	/**
	 * Adds mobility information to each person (whether he/she owns a GA, a halbtax, has
	 * a license etc.)
	 * @param population Input persons
	 * @param households For each person, the household he/she is part of
	 * @param zones For each person the zone he/she lives in
	 * @param world
	 * @param incomeInfo Additional information for each zone
	 */
	void addMobilityInformationToPopulation() {
		System.out.println("Adding license and mobility information to population...");
		LicenseOwnershipGenerator licInfGen =
			new LicenseOwnershipGenerator(Gbl.getWorld(), this.municipalityInfo);
		MobilityResourceGenerator mobInfGen =
			new MobilityResourceGenerator(Gbl.getWorld(), this.municipalityInfo);

		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		for (int i = 0; i < this.population.size(); i++) {
			licInfGen.addLicenseInformation(this.population.get(i), this.households.get(i),
					this.homeZones.get(i));
			mobInfGen.addMobilityInformation(this.population.get(i), this.households.get(i),
					this.homeZones.get(i));

			if (i % (this.population.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		System.out.println("done");
	}

	Plans scaleDownGAOwnershipFraction(final double newGAOwningFraction) {
		/**
		 * It seems that too many people own a GA after that, so scale down the fraction
		 * of people owning a GA to the given fraction (usually 0.09)
		 */
		System.out.println("Scaling down GA ownership to a fraction of " + newGAOwningFraction);
		MobilityResourceGenerator.scaleDownGAOwnershipFraction(this.plans, newGAOwningFraction);

		return this.plans;
	}

	Plans addActivityChainsToPopulation() {
		/**
		 * Get the reference plans of the micro census 2005. They are used to assign initial
		 * plans to the population.
		 */
		String refPlansFilename =
			inputFolder + "microcensus2000Tue2ThuWeighted_plans_withCoords.xml";
		Plans referencePopulation = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(referencePopulation);
		plansReader.readFile(refPlansFilename);
		/**
		 * "Clean up" the reference plans (i.e. removing plans with acts of unknown type,
		 * merge consecutive acts of same type, remove plans with only one act etc.)
		 */
		MicroCensus2005ActChainGenerator.revisePlans(referencePopulation);
		Plan[] referencePlans = MicroCensus2005ActChainGenerator
				.getPlanArray(referencePopulation);
		/**
		 * Distribute the reference plans over the population
		 */
		System.out.println("Adding plans to population...");
		PlansGenerator plansGen = new PlansGenerator(this.plans, referencePlans);
		plansGen.run(this.plans);
		System.out.println();

		return this.plans;
	}

	/**
	 * Assigns coordinates to the "h" (home) acts of a person's plan.
	 * @return The modified plans
	 */
	Plans mapHomeActivitiesToFacilities() {
		/**
		 * Map persons to a home facility
		 */
		System.out.println("Mapping persons to home facilities...");
		PersonToHomeFacilityMapper homeFacMapper =
			new PersonToHomeFacilityMapper(this.population, this.homeZones,
					this.facsPerZone);
		homeFacMapper.run();
		System.out.println("done");

		return this.plans;
	}

	/**
	 * Assigns coordinates to the primary activity acts (work or education)
	 * of a person's plan.
	 * @return The modified plans
	 */
	Plans mapPrimaryActivitiesToFacilities() {

		/**
		 * Get the commuter matrices in order to assign commuter information to the population
		 */
		Matrix workCommuterMatrix = Matrices.getSingleton().getMatrix("work");
		Matrix educationCommuterMatrix = Matrices.getSingleton().getMatrix("education");
//		/**
//		 * "Clean up" the commuter matrices, i.e. remove entries pointing to zones
//		 * that have no facilities that allow performing the respective activity
//		 * and add missing entries starting at zones that contain facitlities that
//		 * allow the "home" activity (people living there must work somewhere for example,
//		 * so we have to add those missing entries).
//		 */
//		CommuterInformationRevisor commuterRev =
//			new CommuterInformationRevisor(referenceZones, Facilities.getSingleton());
//		commuterRev.run(workCommuterMatrix, educationCommuterMatrix);

		/**
		 * Map persons to facilities where they perform their primary activitiy (i.e.
		 * work or education).
		 */
		CommuterInformationGenerator commuterGen =
			new CommuterInformationGenerator(this.population, this.homeZones,
					this.facsPerZone);
		System.out.println("Generating commuter information...");
		commuterGen.run(workCommuterMatrix, educationCommuterMatrix);
		System.out.println("done");

		TreeMap<Id, Zone> tmpZones = new TreeMap<Id, Zone>();
		for (Zone zone : this.referenceZones) {
			tmpZones.put(zone.getId(), zone);
		}
		/**
		 * Add the information in which a person lives and performs its primary activity
		 * to its knowledge such that it is written out and can be read in next time. This way
		 * we do not have to perform this costly computation again after having read in the
		 * population.
		 */
		ArrayList<Location> primActLocs =
			commuterGen.getPrimaryActLocations();
		for (int i = 0; i < primActLocs.size(); i++) {
			if (primActLocs.get(i) != null) {
//				population.get(i).getKnowledge().setDesc(
//					population.get(i).getKnowledge().getDesc() +
//						";" + primActLocs.get(i).getId());
				this.primaryActZones.add(tmpZones.get(primActLocs.get(i).getId()));
			} else {
				this.primaryActZones.add(null);
			}
		}

		return this.plans;
	}

	/**
	 * Assigns coordinates to those acts of a person's plan that have no coords yet.
	 * @return The modified plans
	 */
	Plans mapSecondaryActivitiesToFacilities() {
		/**
		 * Map persons to facilities where they perform secondary activities
		 */
		System.out.println("Adding secondary locations to plans...");
		Barbell2ndaryLocationGenerator secondaryLocGen =
			new Barbell2ndaryLocationGenerator(this.facsPerZone,
					this.referenceZones);
		secondaryLocGen.run(this.population, this.homeZones, this.primaryActZones);
		System.out.println("done");

		return this.plans;
	}

	static void setAllLegModesToCar(final Plans plans) {
		/**
		 * Set all leg modes to "car"
		 */
		for (Person person : plans.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				BasicPlanImpl.LegIterator legIt = plan.getIteratorLeg();
				while (legIt.hasNext()) {
					legIt.next().setMode("car");
				}
			}
		}
	}

	static void mapActCoordsToLinks(final NetworkLayer network, final Plans plans) {
		/**
		 * Map the activities to links of the underlying network (yet we had only swiss coordinates
		 * associated to each activity)
		 */
		network.connect();
		System.out.println("Mapping acts to links...");
		XY2LinksMultithreaded xy2lAlgo = new XY2LinksMultithreaded(network);
		runMultiThreadedPlansAlgo(plans, xy2lAlgo);
		System.out.println("done");
	}

	static void createInitialRoutes(final NetworkLayer network, final Plans plans) {
		/**
		 * Create initial routes such that a person can get from one act to the next act
		 */
		System.out.println("Creating routes...");
		PreProcessLandmarks preProcessRoutingData =
			new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preProcessRoutingData.run(network);
		ReRouteLandmarks router = new ReRouteLandmarks(
				network, new FreespeedTravelTimeCost(),
				new FreespeedTravelTimeCost(), preProcessRoutingData);

		long now = System.currentTimeMillis();

		runMultiThreadedPlansAlgo(plans, router);

		now = System.currentTimeMillis() - now;
		System.out.println("Average elapsed time for routing :\n" +
				(int) (now / (24 * 60 * 60 * 1000F))
				+ " days " + (int) ((now / (60 * 60 * 1000F)) % 24)
				+ " hours; " + (int) ((now / (60 * 1000F)) % 60)
				+ " mins; " + (int) ((now / 1000F) % 60) + " secs; "
				+ (int) (now % 1000) + " msecs; " + "(" + now
				+ " msecs in total)");
	}

	private static void runMultiThreadedPlansAlgo(final Plans plans, final MultithreadedModuleA algo) {
		Iterator<Person> pIt = plans.getPersons().values().iterator();
		algo.init();
		while (pIt.hasNext()) {
			Person person = pIt.next();
			for (Plan plan : person.getPlans()) {
				algo.handlePlan(plan);
			}
		}
		algo.finish();
	}

	private static void writePlans(final Plans plans, final String outFilename) {
		System.out.println("Writing plans to " + outFilename);
		PlansWriter plansWriter = new PlansWriter(plans, outFilename,
				Gbl.getConfig().plans().getOutputVersion());
		plansWriter.setUseCompression(true);
		plans.setPlansWriter(plansWriter);
		plansWriter.write();
		System.out.println("done");
	}
}
