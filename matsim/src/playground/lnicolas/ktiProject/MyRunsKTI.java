/* *********************************************************************** *
 * project: org.matsim.*
 * MyRunsKTI.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.algorithms.XY2Links;
import org.matsim.plans.algorithms.XY2LinksMultithreaded;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import playground.lnicolas.MyRuns;

/**
 * Generates a plans file out of the given input data.
 * @author lnicolas
 *
 */
public class MyRunsKTI extends MyRuns {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		runPlansAlgorithms(); System.exit(0);
//		fixPlans(); System.exit(0);

//		balmiRouting(); System.exit(0);

		/**
		 * All input data must be in the given folder
		 */
		final String inputFolder = "/data/matsim-t/lnicolas/data/";
		/**
		 * Path to the folder where the output data is written to
		 */
		final String outputFolder =  "/data/matsim-t/lnicolas/";
		ArrayList<Zone> referenceZones = null;
		TreeMap<IdI, MunicipalityInformation> municipalityInfo = null;
		ArrayList<Zone> homeZones = null;
		ArrayList<Zone> primaryActZones = null;
		if (args[2].equals("2000r") == false && args[2].equals("2006r") == false) {
			/**
			 * Get the world
			 */
			readWorld();

			/**
			 * Get the ZoneLayer containing the municipalities
			 */
			referenceZones = new ArrayList((Gbl.getWorld().getLayer(
					new Id("municipality"))).getLocations().values());

			/**
			 * Read additional information per municipality from the given input file
			 */
			municipalityInfo = MunicipalityInformation.readTabbedMunicipalityInfo(
					inputFolder + "world_ch_gem.xml.txt", referenceZones);
			homeZones = new ArrayList<Zone>();
			primaryActZones = new ArrayList<Zone>();

			/**
			 * Add only those zones to the set of initial home zones (i.e. zones where
			 * people live) for which there exist additional information
			 */
			for (Zone zone : referenceZones) {
				if (municipalityInfo.containsKey(zone.getId())) {
					homeZones.add(zone);
				}
			}
		}

		Plans plans = null;
		ArrayList<Person> population = null;
		ArrayList<HouseholdI> households = null;
		readNetwork();
		if (args[2].equals("2000")) {

			/**
			 * Generate an initial population out of the census information
			 */
			CensusPopulationGenerator popGen = new CensusPopulationGenerator(inputFolder);
			plans = popGen.run();
			population = popGen.getPersons();
			households = popGen.getHouseholds();

			/**
			 * Map each person to a home zone (ignoring zones for which there exists no
			 * income information) and add income information (based on its home zone) to it
			 */
			homeZones = Income2000Generator.mapPersonsToZones(population,
					homeZones, municipalityInfo);
			addCensusIncomeInformationToPopulation(population, popGen.getHouseholds(),
					homeZones, Gbl.getWorld(), municipalityInfo);
		} else if (args[2].equals("2006")) {
			/**
			 * Generate an initial population out of the datapuls information
			 */
			DatapulsPopulationGenerator popGen = new DatapulsPopulationGenerator(inputFolder);
			plans = popGen.run();
			population = popGen.getPersons();
			households = popGen.getHouseholds();

			/**
			 * Map each person to a home zone
			 */
			homeZones = MobilityResourceGenerator.mapPersonsToZones(population, homeZones);
		} else if (args[2].equals("2006_wu")) {
			/**
			 * Generate an initial population out of the datapuls information, without
			 * upsampling the population, i.e. only keep those persons for which
			 * there exist valid data in the input dataset
			 */
			DatapulsPopulationGenerator popGen = new DatapulsPopulationGenerator(inputFolder);
			plans = popGen.runWithoutUpsampling();
			population = popGen.getPersons();
			households = popGen.getHouseholds();
			homeZones = MobilityResourceGenerator.mapPersonsToZones(population,
					homeZones);
		} else if (args[2].equals("2000r") || args[2].equals("2006r")) {
			/**
			 * Read initial input plans
			 */
			plans = readPlans();
		} else {
			return;
		}
		if (args[2].equals("2000r") == false && args[2].equals("2006r") == false) {

			writePlans(plans, outputFolder + "population" + args[2] + "_noMobilityTools.xml.gz");

			/**
			 * Do some analysis...
			 */
			writePersonCountPerZoneDistribution(population, homeZones, args[2]);
			/**
			 * Add mobility information to each person (whether he/she owns a GA, a halbtax, has
			 * a license etc.)
			 */
			addMobilityInformationToPopulation(population, households,
					homeZones, Gbl.getWorld(), municipalityInfo);

			/**
			 *
			 */
			for (int i = 0; i < population.size(); i++) {
				population.get(i).createKnowledge(
						homeZones.get(i).getId().toString());
				// Assign household information to persons
				population.get(i).setNOfKids(households.get(i).getKidCount());
				population.get(i).setHouseholdSize(households.get(i).getPersonCount());
				population.get(i).setHouseholdIncome(households.get(i).getIncome());
			}
			/**
			 * It seems that too moch people own a GA after that, so scale down the fraction
			 * of people owning a GA to the given fraction (usually 0.09)
			 */
			System.out.println("Scaling down GA ownership to a fraction of " + 0.09);
			MobilityResourceGenerator.scaleDownGAOwnershipFraction(plans, 0.09);

			writePlans(plans, outputFolder + "population" + args[2] + "_noPlans.xml.gz");

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
			PlansGenerator plansGen = new PlansGenerator(plans, referencePlans);
			plansGen.run(plans);
			System.out.println();

			writePlans(plans, outputFolder + "population" + args[2] + "_noHomes.xml.gz");

			/**
			 * Again some analysis...
			 */
			analyzePopulation(population, homeZones, args[2],
					((ZoneLayer) Gbl.getWorld().getLayer(new Id("municipality"))).getLocations().values());
			analyzePopulation(plans, args[2]);
			MicroCensus2005ActChainGenerator.writeActChainDistributionWorkNoWork(
					plans, outputFolder + "actChainDistr" + args[2] + ".csv");

			/**
			 * Get the facilities
			 */
			Facilities facilities = readFacilities();
			/**
			 * Group facilities per zone for faster lookup of facilities per zone
			 */
			GroupFacilitiesPerZone facsPerZone = new GroupFacilitiesPerZone();
			System.out.println("Building QuadTree of facility coords for "
					+ "each zone...");
			facsPerZone.run(referenceZones, facilities);
			System.out.println("done");

			/**
			 * Get the layer that connects the each facility to the zones it lies in.
			 */
			ZoneLayer facilityLocations =
				(ZoneLayer) Gbl.getWorld().getLayer(new Id("facility"));
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

			/**
			 * Map persons to a home facility
			 */
			System.out.println("Mapping persons to home facilities...");
			PersonToHomeFacilityMapper homeFacMapper =
				new PersonToHomeFacilityMapper(population, homeZones,
						facsPerZone);
			homeFacMapper.run();
			System.out.println("done");

			writePlans(plans, outputFolder + "population" + args[2] + "_noPrimaryActs.xml.gz");

			/**
			 * Get the commuter matrices in order to assign commuter information to the population
			 */
			Matrices commuterMatrices = readMatrices();
			Matrix workCommuterMatrix =
				commuterMatrices.getMatrix("work");
			Matrix educationCommuterMatrix =
				commuterMatrices.getMatrix("education");
			/**
			 * "Clean up" the commuter matrices, i.e. remove entries pointing to zones
			 * that have no facilities that allow performing the respective activity
			 * and add missing entries starting at zones that contain facilities that
			 * allow the "home" activity (people living there must work somewhere for example,
			 * so we have to add those missing entries).
			 */
			CommuterInformationRevisor commuterRev =
				new CommuterInformationRevisor(referenceZones,
					facilities);
			commuterRev.run(workCommuterMatrix, educationCommuterMatrix);

			/**
			 * Map persons to facilities where they perform their primary activity (i.e.
			 * work or education).
			 */
			CommuterInformationGenerator commuterGen =
				new CommuterInformationGenerator(population, homeZones,
						facsPerZone);
			System.out.println("Generating commuter information...");
			commuterGen.run(workCommuterMatrix, educationCommuterMatrix);
			System.out.println("done");

			TreeMap<IdI, Zone> tmpZones = new TreeMap<IdI, Zone>();
			for (Zone zone : referenceZones) {
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
					population.get(i).getKnowledge().setDesc(
						population.get(i).getKnowledge().getDesc() +
							";" + primActLocs.get(i).getId());
					primaryActZones.add(tmpZones.get(primActLocs.get(i).getId()));
				} else {
					primaryActZones.add(null);
				}
			}

			writePlans(plans, outputFolder + "population" + args[2] + "_no2ndaryActs.xml.gz");

			/**
			 * Map persons to facilities where they perform secondary activities
			 */
			System.out.println("Adding secondary locations to plans...");
			Barbell2ndaryLocationGenerator secondaryLocGen =
				new Barbell2ndaryLocationGenerator(facsPerZone,
						referenceZones);
			secondaryLocGen.run(population, homeZones, primaryActZones);
			System.out.println("done");

			/**
			 * Set all leg modes to "car"
			 */
			for (Person person : plans.getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					BasicPlan.LegIterator legIt = plan.getIteratorLeg();
					while (legIt.hasNext()) {
						legIt.next().setMode("car");
					}
				}
			}

			writePlans(plans, outputFolder + "population" + args[2] + "_noLinks.xml.gz");

		}

		int startIndex = 0;
		if (args.length > 3) {
			try {
				startIndex = Integer.parseInt(args[3]);
				System.out.println("Starting at chunk " + startIndex);
			} catch (Exception e) {}
		}

		/**
		 * Map the activities to links of the underlying network (yet we had only swiss coordinates
		 * associated to each activity)
		 */
		network.connect();
		System.out.println("Mapping acts to links...");
		XY2LinksMultithreaded xy2lAlgo = new XY2LinksMultithreaded(network);
		runMultiThreadedPlansAlgo(plans, xy2lAlgo);
		System.out.println("done");

		writePlans(plans, outputFolder + "population" + args[2] + "_noRoutes.xml.gz");

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
		printNote("R O U T I N G   M E A S U R E M E N T",
				"Average elapsed time for routing :\n" +
				(int) (now / (24 * 60 * 60 * 1000F))
				+ " days " + (int) ((now / (60 * 60 * 1000F)) % 24)
				+ " hours; " + (int) ((now / (60 * 1000F)) % 60)
				+ " mins; " + (int) ((now / 1000F) % 60) + " secs; "
				+ (int) (now % 1000) + " msecs; " + "(" + now
				+ " msecs in total)");

		/**
		 * Write out the population
		 */
		writePlans(plans, outputFolder + "population" + args[2] +
				"_complete.xml.gz");
	}

	private static void runPlansAlgorithms() {
		readNetwork();
		System.out.println("  setting up plans objects...");
		Plans plans = new Plans();
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		PersonActTypeExtension2 algo = new PersonActTypeExtension2();
		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(algo);
		System.out.println("  done.");

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		plansWriter.write();
		System.out.println("  done.");

		algo.printInformation();

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

	private static void runMultiThreadedPlansAlgo(final int startIndex, final Plans plans,
			final MultithreadedModuleA algo) {
		Iterator<Person> pIt = plans.getPersons().values().iterator();
		int chunkCount = 30;
		int chunkSize = (plans.getPersons().size()) / chunkCount;
		try {
			for (int i = 0; i < chunkCount; i++) {
				if (i+1 >= startIndex) {
					Plans tPlans = new Plans();

					algo.init();

					for (int j = 0; j < chunkSize; j++) {
						Person person = pIt.next();
						tPlans.addPerson(person);

						for (Plan plan : person.getPlans()) {
							algo.handlePlan(plan);
						}
					}
					if (i == chunkCount - 1) {
						while (pIt.hasNext()) {
							Person person = pIt.next();
							tPlans.addPerson(person);

							for (Plan plan : person.getPlans()) {
								algo.handlePlan(plan);
							}
						}
					}

					algo.finish();
					System.out.println("done");

//					writePlans(tPlans, outputFolder + args[2] + "_complete" + (i+1) + ".xml.gz");
				} else {
					for (int j = 0; j < chunkSize; j++) {
						// Skip persons
						pIt.next();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void fixPlans() {
		System.out.println("RUN: fixPlans");

		readNetwork();

		/*****************************************/
		Facilities facilities = readFacilities();
		GroupFacilitiesPerZone facsPerZone = new GroupFacilitiesPerZone();
		System.out.println("Building QuadTree of facility coords for "
				+ "each zone...");
		readWorld();
		ArrayList<Zone> referenceZones =
			new ArrayList((Gbl.getWorld().getLayer(
		new Id("municipality"))).getLocations().values());
		facsPerZone.run(referenceZones, facilities);
		System.out.println("done");

		Barbell2ndaryLocationGenerator secondaryLocGen =
			new Barbell2ndaryLocationGenerator(facsPerZone,
					referenceZones);

		String refPlansFilename =
			"/data/matsim-t/lnicolas/data/"
			+ "microcensus2000Tue2ThuWeighted_plans_withCoords.xml";
		Plans referencePopulation = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader2 = new MatsimPlansReader(referencePopulation);
		plansReader2.readFile(refPlansFilename);
		System.out.println("refPop size: " +
				referencePopulation.getPersons().size());
		MicroCensus2005ActChainGenerator.revisePlans(referencePopulation);
		HomeDurationActFixer durFix = new HomeDurationActFixer(
				referencePopulation);
		/********************************************/
		System.out.println("  setting up plans objects...");
		Plans plans = new Plans();
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		plans.addAlgorithm(durFix);
		plans.addAlgorithm(secondaryLocGen);
//		PlansAlgorithm xy2l_algo = new XY2Links(network);
//		plans.addAlgorithm(xy2l_algo);
		System.out.println("  done.");

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		System.out.println("  done.");

		writePlans(plans, "/data/matsim-t/lnicolas/population" + 2000
				+ "_noLinksNew.xml.gz");

//		/**
//		 * Map the activities to links of the underlying network (yet we had only swiss coordinates
//		 * associated to each activity)
//		 */
//		System.out.println("Mapping acts to links...");
//		XY2LinksMultithreaded xy2lAlgo = new XY2LinksMultithreaded(network);
//		xy2lAlgo.init();
//		for (Person person : plans.getPersons().values()) {
//			for (Plan plan : person.getPlans()) {
//				xy2lAlgo.handlePlan(plan);
//			}
//		}
//		xy2lAlgo.finish();
//		System.out.println("done");

		plansWriter.write();

		System.out.println("RUN: fixPlans finished.");
		System.out.println();
	}

	/**
	 * Reads in the plans file and performs a routing over the persons in it.
	 */
	private static void balmiRouting() {

		System.out.println("RUN: balmiRouting");

		System.out.println("  reading the network...");
		readNetwork();

		System.out.println("Creating routes...");
		PreProcessLandmarks preProcessRoutingData =
			new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preProcessRoutingData.run(network);
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		Plans plans = new Plans();
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		System.out.println("  adding plans algorithm... ");
		PlansAlgorithm xy2l_algo = new XY2Links(network);
		plans.addAlgorithm(xy2l_algo);
		plans.addAlgorithm(new PlansCalcRouteLandmarks(network, preProcessRoutingData,
				new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost()));
		System.out.println("  done.");

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		plansWriter.write();
		System.out.println("  done.");

		System.out.println("RUN: balmiRouting finished.");
		System.out.println();
	}

	/**
	 * Merges several plans files into one
	 * @param prefix The prefix common to all input plan file chunks
	 * @param postfix The postfix common to all input plan file chunks
	 * @return The merged plans
	 */
	private static Plans mergePlans(final String prefix, final String postfix) {
		/*
		 *  Sample call:
		 * 		readNetwork();
		 *		Plans plans2 = mergePlans("/data/matsim-t/lnicolas/population2000r_complete", ".xml.gz");
		 *		writePlans(plans2, "/data/matsim-t/lnicolas/population2000_complete.xml.gz");
		 **/
		Plans plans = new Plans();

		PlansReaderI plansReader = new MatsimPlansReader(plans);
		for (int i = 1; new File(prefix + i + postfix).exists(); i++) {
			plansReader.readFile(prefix + i + postfix);
		}

		return plans;
	}

	/**
	 * Splits the given plans file into several ones and writes them
	 * @param prefix The prefix common to all output plan file chunks
	 * @param postfix The postfix common to all output plan file chunks
	 */
	private static void splitPlans(final Plans plans, final String prefix, final String postfix) {
		/*
		 *  Sample call:
		 * 		readNetwork();
		 * 		Plans plans = readPlans();
		 *		splitPlans(plans, "/data/matsim-t/lnicolas/population" + args[2] + "_chunk", ".xml.gz");
		 **/
		Iterator<Person> pIt = plans.getPersons().values().iterator();
		int threadCount = 16;
		int chunkSize = (plans.getPersons().size()) / threadCount;
		try {
			for (int i = 0; i < threadCount; i++) {
				Plans tPlans = new Plans();
				for (int j = 0; j < chunkSize; j++) {
					tPlans.addPerson(pIt.next());
				}
				if (i == threadCount - 1) {
					while (pIt.hasNext()) {
						tPlans.addPerson(pIt.next());
					}
				}
				writePlans(tPlans, prefix + (i+1) + postfix);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void readAndWriteData() {
		readWorld();
		readFacilities();
		Plans plans = readPlans();
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);

		plansWriter.write();
		System.out.println("Wrote plans to " + Gbl.getConfig().plans().getOutputFile());
	}

	/**
	 * Writes a tab-separated file containing for each person a line with the person's
	 * id, its home x-coord, its home y-coord and the number of persons that have an home act
	 * at the same coord as the current person.
	 * @param plans Input plans
	 * @param filename The output file
	 */
	private static void writeHomeFacilities(final Plans plans, final String filename) {
		TreeMap<String, Integer> facilities = new TreeMap<String, Integer>();
		CoordI homeCoord = null;
		int personCnt = 0;
		for (Person person : plans.getPersons().values()) {
			homeCoord = PersonToHomeFacilityMapper.getHomeCoord(person);
			String xyID = homeCoord.getX() + "-" + homeCoord.getY();
			personCnt = 0;
			if (facilities.containsKey(xyID)) {
				personCnt = facilities.get(xyID);
			}
			facilities.put(xyID, personCnt + 1);
		}

		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("personID\thomeXCoord\thomeYCoord\tpersonCount\n");
			for (Person person : plans.getPersons().values()) {
				homeCoord = PersonToHomeFacilityMapper.getHomeCoord(person);
				String xyID = homeCoord.getX() + "-" + homeCoord.getY();
				out.write(person.getId() + "\t" + homeCoord.getX() + "\t" + homeCoord.getY()
						+ "\t" + facilities.get(xyID) + "\n");
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
				+ e.getMessage());
		}
		System.out.println("Home facilities written to " + filename);
	}

	/**
	 * Writes a histogram (format: csv) of the micro census 2000 and micro census 2005 activity chains
	 * distributions.
	 */
	private static void writeMZComparison() {
		String plans2000Filename = "/home/lnicolas/data/kti-projekt/micro-census-ch-2000/"
				+ "Tue2Thu-weighted/microcensus2000Tue2ThuWeighted_plans_withCoords.xml";
		Plans population2000 = new Plans();
		PlansReaderI plansReader2000 = new MatsimPlansReader(population2000);
		plansReader2000.readFile(plans2000Filename);
		MicroCensus2005ActChainGenerator.revisePlans(population2000);
		Plan[] plans2000 = MicroCensus2005ActChainGenerator.getPlanArray(population2000);

		Plans population2005 =
			MicroCensus2005ActChainGenerator.run("/home/lnicolas/data/hettinger/Wege.txt");
		Plan[] plans2005 = MicroCensus2005ActChainGenerator.getPlanArray(population2005);

		TreeMap<String, int[]> actChainDistr = new TreeMap<String, int[]>();
		double cnt2000 = addPlansToMZDistr(plans2000, actChainDistr, 0);
		double cnt2005 = addPlansToMZDistr(plans2005, actChainDistr, 1);

		System.out.println(actChainDistr.size());

		String filename = "/home/lnicolas/data/hettinger/mzActChainDistr.csv";
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			Iterator<Map.Entry<String, int[]>> it = actChainDistr.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = it.next();
				int[] values = (int[]) entry.getValue();
				out.write(entry.getKey() + "\t" + values[0]/cnt2000 + "\t" + values[1]/cnt2005 + "\n");
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
				+ e.getMessage());
		}
		System.out.println("Activity chain comparison written to " + filename);
	}


	private static int addPlansToMZDistr(final Plan[] plans2000, final TreeMap<String, int[]> actChainDistr,
			final int index) {
		int cnt = 0;
		for (Plan p : plans2000) {
			String actChain = "";
			BasicPlan.ActIterator it = p.getIteratorAct();
			while (it.hasNext()) {
				actChain += it.next().getType();
			}
			int[] actChainCnt = null;
			if (actChainDistr.containsKey(actChain)) {
				actChainCnt = actChainDistr.get(actChain);
			} else {
				actChainCnt = new int[2];
				actChainCnt[0] = 0;
				actChainCnt[1] = 0;
			}
			actChainCnt[index] = actChainCnt[index] + 1;
			actChainDistr.put(actChain, actChainCnt);
			cnt++;
		}
		return cnt;
	}

	/**
	 * Sets a person's income to the average income of the zone (municipality) it lives in
	 * @param population Input persons
	 * @param households For each person, the household he/she is part of
	 * @param zones For each person the zone he/she lives in
	 * @param world
	 * @param incomeInfo Additional information for each zone
	 */
	private static void addCensusIncomeInformationToPopulation(final ArrayList<Person> population,
			final ArrayList<HouseholdI> households, final ArrayList<Zone> zones, final World world,
			final TreeMap<IdI, MunicipalityInformation> incomeInfo) {
		System.out.println("Adding municipality income information to population...");

		Income2000Generator incInfGen = new Income2000Generator(world, incomeInfo);
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		// calculate (add up) the household income from the personal incomes
		for (int i = 0; i < population.size(); i++) {
			if (population.get(i).getEmployed().equals("yes")) {
				double income = incInfGen.getIncome2000(population.get(i), zones.get(i));
				HouseholdI hInfo = households.get(i);
				hInfo.setIncome(income + hInfo.getIncome());
			}

			if (i % (population.size() / statusString.length()) == 0) {
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
	private static void addMobilityInformationToPopulation(final ArrayList<Person> population,
			final ArrayList<HouseholdI> households, final ArrayList<Zone> zones, final World world,
			final TreeMap<IdI, MunicipalityInformation> municipalityInfo) {
		System.out.println("Adding license and mobility information to population...");
		LicenseOwnershipGenerator licInfGen =
			new LicenseOwnershipGenerator(world, municipalityInfo);
		MobilityResourceGenerator mobInfGen =
			new MobilityResourceGenerator(world, municipalityInfo);

		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		for (int i = 0; i < population.size(); i++) {
			licInfGen.addLicenseInformation(population.get(i), households.get(i), zones.get(i));
			mobInfGen.addMobilityInformation(population.get(i), households.get(i), zones.get(i));

			if (i % (population.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		System.out.println("done");

//		licInfGen.printInformation();
	}

	/**
	 * Does some analysis over the given plans...
	 * @param plans
	 * @param suffix
	 */
	private static void analyzePopulation(final Plans plans, final String suffix) {
		System.out.println("analyzing population...");
		for (int i = 0; i <= 7; i++) {
			writePopulationAgeDistribution(plans, suffix, i);
		}
		writePopulationHHSizeDistribution(plans, suffix);
		writePopulationIncomeDistribution(plans, suffix);
		writePopulationHHKidCountDistribution(plans, suffix);
	}

	/**
	 * Does some analysis over the given plans...
	 * @param plans
	 * @param suffix
	 */
	private static void analyzePopulation(final ArrayList<Person> population,
			final ArrayList<Zone> zones, final String suffix, final Collection<Zone> referenceZones) {
		System.out.println("analyzing population with respect to municipalities...");
		for (int i = 0; i <= 7; i++) {
			writePopulationDistribution(population, zones, suffix, referenceZones, i);
		}
		writePopulationIncomeDistribution(population, zones, suffix + "_income", referenceZones);
		writePopulationAgeDistribution(population, zones, suffix + "_age", referenceZones);
	}

	private static void writePopulationAgeDistribution(final ArrayList<Person> population,
			final ArrayList<Zone> zones, final String suffix, final Collection<Zone> referenceZones) {
		TreeMap<IdI, ArrayList<Integer> > personsPerZone = new TreeMap<IdI, ArrayList<Integer>>();
		ArrayList<Integer> nullAges = new ArrayList<Integer>();
		for (int j = 0; j < 126; j++) {
			nullAges.add(0);
		}
		for (int i = 0; i < zones.size(); i++) {
			Person p = population.get(i);
			Zone z = zones.get(i);
			int age = p.getAge();
			ArrayList<Integer> ages = null;
			if (personsPerZone.containsKey(z.getId())) {
				ages = personsPerZone.get(z.getId());
			} else {
				ages = new ArrayList<Integer>(nullAges);
				personsPerZone.put(z.getId(), ages);
			}
			int personCnt = ages.get(age);
			ages.set(age, personCnt + 1);
		}

		BufferedWriter out;
		String filename = "/home/lnicolas/data/kti-projekt/zoneDistr" + suffix + ".txt";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("MunicipalityID\tMunicipalityName");
			for (int i = 0; i < nullAges.size(); i++) {
				out.write(i + "\t");
			}
			out.newLine();
			ArrayList<Integer> ages = null;
			for (Zone z : referenceZones) {
				if (personsPerZone.containsKey(z.getId())) {
					ages = personsPerZone.get(z.getId());
				} else {
					ages = nullAges;
				}
				out.write(z.getId() + "\t" + z.getName());
				for (int age : ages) {
					out.write("\t" + age);
				}
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Zone distribution written to " + filename);
	}

	private static void writePopulationIncomeDistribution(final ArrayList<Person> population,
			final ArrayList<Zone> zones, final String suffix, final Collection<Zone> referenceZones) {
		TreeMap<IdI, Integer> personsPerZone = new TreeMap<IdI, Integer>();
		TreeMap<IdI, Double> incomePerZone = new TreeMap<IdI, Double>();
		for (int i = 0; i < zones.size(); i++) {
			Person p = population.get(i);
			Zone z = zones.get(i);
			int personCnt = 0;
			double avgIncome = 0;
			if (personsPerZone.containsKey(z.getId())) {
				personCnt = personsPerZone.get(z.getId());
				avgIncome = incomePerZone.get(z.getId());
			}
			personsPerZone.put(z.getId(), personCnt + 1);
			incomePerZone.put(z.getId(),
					((avgIncome * personCnt) + p.getHouseholdIncome()) / (personCnt + 1));
		}

		BufferedWriter out;
		String filename = "/home/lnicolas/data/kti-projekt/zoneDistr" + suffix + ".txt";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("Municipality\tavg" + suffix + "\n");
			for (Zone z : referenceZones) {
				double avgInc = -1;
				if (incomePerZone.containsKey(z.getId())) {
					avgInc = incomePerZone.get(z.getId());
				}
				out.write(z.getName() + "\t" + avgInc + "\n");
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Zone distribution written to " + filename);
	}

	private static void writePopulationDistribution(final ArrayList<Person> population,
			final ArrayList<Zone> zones, String suffix, final Collection<Zone> referenceZones,
			final int differenceTypeIndex) {

		TreeMap<IdI, Integer> yesPersonsPerZone = new TreeMap<IdI, Integer>();
		TreeMap<IdI, Integer> noPersonsPerZone = new TreeMap<IdI, Integer>();
		TreeMap<IdI, Integer> tmpDistr = null;
		for (int i = 0; i < zones.size(); i++) {
			Person p = population.get(i);
			Zone z = zones.get(i);
			if (differenceTypeIndex == 0) {
				if (p.getEmployed().equals("yes")) {
					tmpDistr = yesPersonsPerZone;
				} else {
					tmpDistr = noPersonsPerZone;
				}
			} else if (differenceTypeIndex == 1) {
				if (p.getSex().equals("m")) {
					tmpDistr = yesPersonsPerZone;
				} else {
					tmpDistr = noPersonsPerZone;
				}
			} else if (differenceTypeIndex == 2) {
				if (p.getNationality().equals("swiss")) {
					tmpDistr = yesPersonsPerZone;
				} else if (p.getNationality().equals("other")) {
					tmpDistr = noPersonsPerZone;
				}
			} else if (differenceTypeIndex == 3) {
				if (p.getLicense().equals("yes")) {
					tmpDistr = yesPersonsPerZone;
				} else {
					tmpDistr = noPersonsPerZone;
				}
			} else if (differenceTypeIndex == 4) {
				if (p.getCarAvail().equals("always")) {
					tmpDistr = yesPersonsPerZone;
				} else if (p.getCarAvail().equals("never")) {
					tmpDistr = noPersonsPerZone;
				}
			} else if (differenceTypeIndex == 5) {
				if (p.getTravelcards().contains("ch-GA")) {
					tmpDistr = yesPersonsPerZone;
				} else {
					tmpDistr = noPersonsPerZone;
				}
			} else if (differenceTypeIndex == 6) {
				if (p.getTravelcards().contains("ch-HT")) {
					tmpDistr = yesPersonsPerZone;
				} else {
					tmpDistr = noPersonsPerZone;
				}
			} else {
				tmpDistr = yesPersonsPerZone;
			}
			int pCount = 0;
			if (tmpDistr.containsKey(z.getId())) {
				pCount = tmpDistr.get(z.getId());
			}
			tmpDistr.put(z.getId(), pCount + 1);
		}

		String[] typeSuffixes = { "employed", "sex", "nationality", "license", "car",
				"chGA", "chHT"};
		BufferedWriter out;
		if (differenceTypeIndex >= 0 && differenceTypeIndex < typeSuffixes.length) {
			suffix += "_" + typeSuffixes[differenceTypeIndex];
		}
		String filename = "/home/lnicolas/data/kti-projekt/zoneDistr" + suffix + ".txt";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("Municipality\tyes" + suffix + "\tno" + suffix + "\n");
			for (Zone z : referenceZones) {
				int yCnt = 0;
				if (yesPersonsPerZone.containsKey(z.getId())) {
					yCnt = yesPersonsPerZone.get(z.getId());
				}
				int nCnt = 0;
				if (noPersonsPerZone.containsKey(z.getId())) {
					nCnt = noPersonsPerZone.get(z.getId());
				}
				out.write(z.getName() + "\t" + yCnt + "\t" + nCnt + "\n");
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Zone distribution written to " + filename);
	}

	private static void writePersonCountPerZoneDistribution(final ArrayList<Person> population,
			final ArrayList<Zone> zones, final String suffix) {
		TreeMap<IdI, Integer> personsPerZone = new TreeMap<IdI, Integer>();
		TreeMap<IdI, Zone> zoneByID = new TreeMap<IdI, Zone>();
		for (Zone z : zones) {
			int pCount = 0;
			if (personsPerZone.containsKey(z.getId())) {
				pCount = personsPerZone.get(z.getId());
			}
			personsPerZone.put(z.getId(), pCount + 1);
			zoneByID.put(z.getId(), z);
		}

		BufferedWriter out;
		String filename = "/home/lnicolas/data/kti-projekt/zonePopulationDistr" + suffix + ".csv";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			for (Entry<IdI, Zone> entry : zoneByID.entrySet()) {
				out.write(entry.getValue().getName() + "," + personsPerZone.get(entry.getKey()));
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Zone person count distribution written to " + filename);
	}

	private static void writePopulationHHKidCountDistribution(final Plans plans, final String suffix) {
		double avgHHKidCount = 0;
		int maxHHKidCount = 0;
		int personCount = 0;

		Iterator<Person> it = plans.getPersons().values().iterator();

		while(it.hasNext()) {
			int kidCount = it.next().getNOfKids();
			if (kidCount >= 0) {
				if (kidCount > maxHHKidCount) {
					maxHHKidCount = kidCount;
				}
				avgHHKidCount = ((avgHHKidCount * personCount) + kidCount) / (personCount + 1);
				personCount++;
			}
		}
		System.out.println("Avg KidCount = " + avgHHKidCount + ", max. KidCount= " + maxHHKidCount);
		double binLength = 1;
		ArrayList<Integer> hhKidCountDistr = new ArrayList<Integer>();
		double binCount = 0;
		while (binCount <= maxHHKidCount) {
			hhKidCountDistr.add(0);
			binCount += binLength;
		}

		it = plans.getPersons().values().iterator();
		while(it.hasNext()) {
			int kidCount = it.next().getNOfKids();
			if (kidCount >= 0) {
				int c = hhKidCountDistr.get((int)(kidCount/binLength));
				c++;
				hhKidCountDistr.set((int)(kidCount/binLength), c);
			}
		}

		String valuesString = "";
		String countString = "";
		for (int i = 0; i < hhKidCountDistr.size(); i++) {
			countString += hhKidCountDistr.get(i) + ",";
			if (binLength == 1) {
				valuesString += i + ",";
			} else {
				valuesString += (i*binLength) + "-" + ((i+1)*binLength) + ",";
			}
		}
		valuesString = valuesString.substring(0, valuesString.length() - 1);
		countString = countString.substring(0, countString.length() - 1);
		BufferedWriter out;
		String filename = "/home/lnicolas/data/kti-projekt/hhKidCountDistr" + suffix + ".csv";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write(valuesString);
			out.newLine();
			out.write(countString);
			out.newLine();
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("KidCount distribution written to " + filename);
	}

	private static void writePopulationAgeDistribution(final Plans plans, String suffix,
			final int differenceTypeIndex) {
		double avgAge = 0;
		int maxAge = 0;
		int personCount = 0;

		Iterator<Person> it = plans.getPersons().values().iterator();

		while(it.hasNext()) {
			int age = it.next().getAge();
			if (age >= 0) {
				if (age > maxAge) {
					maxAge = age;
				}
			}
			avgAge = ((avgAge * personCount) + age) / (personCount + 1);
			personCount++;
		}
		System.out.println("Avg age = " + avgAge + ", max. age = " + maxAge);
		double binLength = 1;
		ArrayList<Integer> ageDistr = new ArrayList<Integer>();
		ArrayList<Integer> ageDistr2 = new ArrayList<Integer>();
		double binCount = 0;
		while (binCount <= maxAge) {
			ageDistr.add(0);
			ageDistr2.add(0);
			binCount += binLength;
		}

		String[] typeSuffixes = { "employed", "sex", "nationality", "license", "car",
				"ch-GA", "ch-HT"};
		ArrayList<Integer> tmpDistr = null;
		it = plans.getPersons().values().iterator();
		int yesCnt = 0;
		while(it.hasNext()) {
			Person p = it.next();
			double age = p.getAge();
			if (age >= 0) {
				if (differenceTypeIndex == 0) {
					if (p.getEmployed().equals("yes")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else {
						tmpDistr = ageDistr2;
					}
				} else if (differenceTypeIndex == 1) {
					if (p.getSex().equals("m")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else {
						tmpDistr = ageDistr2;
					}
				} else if (differenceTypeIndex == 2) {
					if (p.getNationality().equals("swiss")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else if (p.getNationality().equals("other")) {
						tmpDistr = ageDistr2;
					}
				} else if (differenceTypeIndex == 3) {
					if (p.getLicense().equals("yes")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else {
						tmpDistr = ageDistr2;
					}
				} else if (differenceTypeIndex == 4) {
					if (p.getCarAvail().equals("always")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else if (p.getCarAvail().equals("never")) {
						tmpDistr = ageDistr2;
					}
				} else if (differenceTypeIndex == 5) {
					if (p.getTravelcards().contains("ch-GA")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else {
						tmpDistr = ageDistr2;
					}
				} else if (differenceTypeIndex == 6) {
					if (p.getTravelcards().contains("ch-HT")) {
						tmpDistr = ageDistr;
						yesCnt++;
					} else {
						tmpDistr = ageDistr2;
					}
				} else {
					tmpDistr = ageDistr;
				}
				int c = tmpDistr.get((int)(age/binLength));
				c++;
				tmpDistr.set((int)(age/binLength), c);
			}
		}

		if (differenceTypeIndex == 1) {
			System.out.println(yesCnt + " of " + plans.getPersons().size()
					+ " are male (" + (double)yesCnt/plans.getPersons().size() + "%)");
		} else if (differenceTypeIndex == 2) {
			System.out.println(yesCnt + " of " + plans.getPersons().size()
					+ " are swiss (" + (double)yesCnt/plans.getPersons().size() + "%)");
		} else if (differenceTypeIndex >= 0 && differenceTypeIndex < typeSuffixes.length) {
			System.out.println(yesCnt + " of " + plans.getPersons().size()
				+ " (own a/are) " + typeSuffixes[differenceTypeIndex] + " (" +
				(double)yesCnt/plans.getPersons().size() + "%)");
		} else {
			System.out.println(yesCnt + " of " + plans.getPersons().size() + " (" +
					(double)yesCnt/plans.getPersons().size() + "%)");
		}

		String valuesString = "";
		String countString = "";
		String count2String = "";
		for (int i = 0; i < ageDistr.size(); i++) {
			countString += ageDistr.get(i) + ",";
			count2String += ageDistr2.get(i) + ",";
			if (binLength == 1) {
				valuesString += i + ",";
			} else {
				valuesString += (i*binLength) + "-" + ((i+1)*binLength) + ",";
			}
		}
		valuesString = valuesString.substring(0, valuesString.length() - 1);
		countString = countString.substring(0, countString.length() - 1);
		BufferedWriter out;
		if (differenceTypeIndex >= 0 && differenceTypeIndex < typeSuffixes.length) {
			suffix += "_" + typeSuffixes[differenceTypeIndex];
		}
		String filename = "/home/lnicolas/data/kti-projekt/ageDistr" + suffix + ".csv";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write(valuesString);
			out.newLine();
			out.write(countString);
			out.newLine();
			out.write(count2String);
			out.newLine();
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Age distribution written to " + filename);
	}

	private static void writePopulationIncomeDistribution(final Plans plans, final String suffix) {
		double avgIncome = 0;
		double maxIncome = 0;
		int personCount = 0;

		Iterator<Person> it = plans.getPersons().values().iterator();

		while(it.hasNext()) {
			double income = it.next().getHouseholdIncome();
			if (income >= 0) {
				if (income > maxIncome) {
					maxIncome = income;
				}
				avgIncome = ((avgIncome * personCount) + income) / (personCount + 1);
				personCount++;
			}
		}
		System.out.println("Avg income = " + avgIncome + ", max. income = " + maxIncome);
		double binLength = 5000;
		ArrayList<Integer> incomeDistr = new ArrayList<Integer>();
		double binCount = 0;
		while (binCount <= maxIncome) {
			incomeDistr.add(0);
			binCount += binLength;
		}

		it = plans.getPersons().values().iterator();
		while(it.hasNext()) {
			double income = it.next().getHouseholdIncome();
			if (income >= 0) {
				int c = incomeDistr.get((int)(income/binLength));
				c++;
				incomeDistr.set((int)(income/binLength), c);
			}
		}

		String valuesString = "";
		String countString = "";
		for (int i = 0; i < incomeDistr.size(); i++) {
			countString += incomeDistr.get(i) + ",";
			if (binLength == 1) {
				valuesString += i + ",";
			} else {
				valuesString += (i*binLength) + "-" + ((i+1)*binLength) + ",";
			}
		}
		valuesString = valuesString.substring(0, valuesString.length() - 1);
		countString = countString.substring(0, countString.length() - 1);
		BufferedWriter out;
		String filename = "/home/lnicolas/data/kti-projekt/incomeDistr" + suffix + ".csv";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write(valuesString);
			out.newLine();
			out.write(countString);
			out.newLine();
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Income distribution written to " + filename);
	}

	private static void writePopulationHHSizeDistribution(final Plans plans, final String suffix) {
		double avgHHSize = 0;
		int maxHHSize = 0;
		int personCount = 0;

		Iterator<Person> it = plans.getPersons().values().iterator();

		while(it.hasNext()) {
			int hhSize = it.next().getHouseholdSize();
			if (hhSize >= 0) {
				if (hhSize > maxHHSize) {
					maxHHSize = hhSize;
				}
				avgHHSize = ((avgHHSize * personCount) + hhSize) / (personCount + 1);
				personCount++;
			}
		}
		System.out.println("Avg household size = " + avgHHSize + ", max. household size = " + maxHHSize);
		double binLength = 1;
		ArrayList<Integer> hhSizeDistr = new ArrayList<Integer>();
		double binCount = 0;
		while (binCount <= maxHHSize) {
			hhSizeDistr.add(0);
			binCount += binLength;
		}

		it = plans.getPersons().values().iterator();
		while(it.hasNext()) {
			double hhSize = it.next().getHouseholdSize();
			if (hhSize >= 0) {
				int c = hhSizeDistr.get((int)(hhSize/binLength));
				c++;
				hhSizeDistr.set((int)(hhSize/binLength), c);
			}
		}

		String valuesString = "";
		String countString = "";
		for (int i = 0; i < hhSizeDistr.size(); i++) {
			countString += hhSizeDistr.get(i) + ",";
			if (binLength == 1) {
				valuesString += i + ",";
			} else {
				valuesString += (i*binLength) + "-" + ((i+1)*binLength) + ",";
			}
		}
		valuesString = valuesString.substring(0, valuesString.length() - 1);
		countString = countString.substring(0, countString.length() - 1);
		BufferedWriter out;
		String filename = "/home/lnicolas/data/kti-projekt/hhSizeDistr" + suffix + ".csv";
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write(valuesString);
			out.newLine();
			out.write(countString);
			out.newLine();
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("household size distribution written to " + filename);
	}

	/**
	 * Maps persons to the nearest link
	 */
	private static void mapPersonsToNetwork() {

		Plans plans = readPlans();
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansAlgorithm xy2l_algo = new XY2Links(network);
		plans.addAlgorithm(xy2l_algo);

		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			System.out.println("  running algorithms over all plans...");
			plans.runAlgorithms();
			System.out.println("  done.");
		}

		plansWriter.write();
		System.out.println("Wrote plans to " + Gbl.getConfig().plans().getOutputFile());
	}

	/**
	 * Removes link references in the persons acts such that the plans can be
	 * used in other networks than the one the persons were mapped to.
	 * @param plans
	 */
	private static void removeLinksFromActs(final Plans plans) {
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);

		for (Person p : plans.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				ArrayList actslegs = plan.getActsLegs();
				for (int j = 0; j < actslegs.size(); j = j + 2) {
					Act act = (Act) actslegs.get(j);
					act.setLink(null);
				}
				for (int j = 1; j < actslegs.size(); j = j + 2) {
					Leg leg = (Leg) actslegs.get(j);
					leg.setRoute(null);
				}
			}
		}

		plansWriter.write();
		System.out.println("Wrote plans to " + Gbl.getConfig().plans().getOutputFile());
	}

	/**
	 * Reduces the number of persons within the given Plans object to the given fromToCount
	 * number.
	 * @param fromToCount
	 * @return
	 */
	private static Plans reducePlans(final Plans plans, final int fromToCount) {
		Gbl.random.nextDouble(); // draw one because of strange "not-randomness" in the first draw...

		// Build array of persons
		ArrayList<Person> persons = new ArrayList<Person>(plans.getPersons().values());
		TreeSet<Integer> usedIndexes = new TreeSet<Integer>();

		Plans newPlans = new Plans();
		int i = 0;
		while (i < fromToCount) {
			// get a new person index
			int ind = (int)(Gbl.random.nextDouble() * plans.getPersons().size());
			while (usedIndexes.contains(ind)) {
				ind = (int)(Gbl.random.nextDouble() * plans.getPersons().size());
			}
			usedIndexes.add(ind);
			Person per = persons.get(ind);
			for (Plan pl : per.getPlans()) {
				for (int j = 0; j < pl.getActsLegs().size(); j++) {
					if (pl.getActsLegs().get(j) instanceof Leg) {
						i++;
					}
				}
			}
			try {
				newPlans.addPerson(per);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error adding person " + per.getId());
			}
		}

		return newPlans;
	}
}
