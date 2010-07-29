/* *********************************************************************** *
 * project: org.matsim.*
 * MyRuns.java
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

package playground.meisterk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonAnalyseTimesByActivityType;
import org.matsim.population.algorithms.PersonAnalyseTimesByActivityType.Activities;
import org.matsim.population.algorithms.PersonRemoveLinkAndRoute;
import org.matsim.run.XY2Links;
import org.xml.sax.SAXException;

import playground.meisterk.eaptus2010.MyControler;
import playground.meisterk.org.matsim.config.groups.MeisterkConfigGroup;
import playground.meisterk.org.matsim.population.algorithms.PersonSetFirstActEndTime;
import playground.meisterk.org.matsim.population.algorithms.PlanAnalyzeTourModeChoiceSet;
import playground.meisterk.phd.controler.PhDControler;

public class MyRuns {

	public static final int TIME_BIN_SIZE = 300;

	private static Logger logger = Logger.getLogger(MyRuns.class);

	//////////////////////////////////////////////////////////////////////
	// What is available in this file?
	//////////////////////////////////////////////////////////////////////

	private enum Run {
		/**
		 * Re-generate equilibration results of semester project of Elias Aptus.
		 */
		EAPTUS_2010("eaptus2010"),
		/**
		 * Generate initial ivtch demand for ivtch-changed-wu-flama. Used for semester project of Elias Aptus.
		 */
		MOVE_DEMAND_TO_NETWORK("moveInitDemandToDifferentNetwork"),
		SUE_STUDY("sueStudy");

		private final String name;

		private Run(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	};

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {

		MyRuns myRuns = new MyRuns();

		boolean validRun = false;

		String desiredRunName = args[0];
		for (Run run : Run.values()) {
			if (desiredRunName.equals(run.getName())) {
				validRun = true;
			}
		}
		if (!validRun) {
			logger.error("A run with the name \"" + desiredRunName + "\"is not available. Available runs are:");
			for (Run run : Run.values()) {
				System.out.println(run.getName());
			}
			System.exit(-1);
		}

		logger.info("Running " + desiredRunName + "...");
		String[] methodArgs = Arrays.copyOfRange(args, 1, args.length);
		if (desiredRunName.equals(Run.EAPTUS_2010.getName())) {
			myRuns.eaptus2010ForPhDThesis(methodArgs);
		} else if (desiredRunName.equals(Run.MOVE_DEMAND_TO_NETWORK.getName())) {
			myRuns.moveInitDemandToDifferentNetwork(methodArgs);
		}
		logger.info("Running " + desiredRunName + "...done.");

	}

	void eaptus2010ForPhDThesis(final String[] args) {

		final double[] VARY_LEARNING_RATE = new double[]{1.0, 0.1};
		
		for (double learningRate : VARY_LEARNING_RATE) {
			MyControler myControler = new MyControler(args);
			myControler.setCreateGraphs(false);
			
			myControler.getConfig().charyparNagelScoring().setLearningRate(learningRate);
			myControler.getConfig().controler().setOutputDirectory("output/eaptus2010ForPhDThesis/equil/learningRate_" + Double.toString(learningRate));
			myControler.run();
		}

	}

	void moveInitDemandToDifferentNetwork(final String[] args) {

		// read ivtch demand
		ScenarioImpl scenario = new ScenarioImpl();
		Config config = scenario.getConfig();
		MeisterkConfigGroup meisterkConfigGroup = new MeisterkConfigGroup();
		config.addModule(MeisterkConfigGroup.GROUP_NAME, meisterkConfigGroup);
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(args[0]);
		Gbl.setConfig(config);
		MatsimRandom.reset(config.global().getRandomSeed());

		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();

		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		// remove links and routes
		PersonRemoveLinkAndRoute personRemoveLinkAndRoute = new PersonRemoveLinkAndRoute();
		personRemoveLinkAndRoute.run(population);

		// switch to new network in scenario
		ScenarioImpl scenario2 = new ScenarioImpl();
		try {
			new MatsimNetworkReader(scenario2).parse(config.getParam(MeisterkConfigGroup.GROUP_NAME, "inputSecondNetworkFile"));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scenario.setNetwork(scenario2.getNetwork());
		// run XY2Links
		XY2Links xY2Links = new XY2Links();

		// write out new initial demand
		new PopulationWriter(population, network).write(scenario.getConfig().plans().getOutputFile());

	}

	/**
	 * Generates the results of the sensitivity analysis of the SUE study.
	 */
	void doSUEStudySensitivityAnalysis(final String[] args, String outputDirectory) {

		final double[] VARY_BETA = new double[]{2.0, 0.1, 1.0, 4.0, 10.0, Double.MAX_VALUE};
		final double[] VARY_LEARNING_RATE = new double[]{1.0, 0.1};
		final String[] VARY_TIME_MODULE = new String[]{"TimeAllocationMutator", "Planomat"};

		for (double beta : VARY_BETA) {
			for (double learningRate : VARY_LEARNING_RATE) {

				for (String timingModule : VARY_TIME_MODULE) {
					Controler testee = new PhDControler(args);

					StrategySettings timingStrategy = new StrategySettings(new IdImpl(3));
					timingStrategy.setModuleName(timingModule);
					timingStrategy.setProbability(0.1);
					testee.getConfig().strategy().addStrategySettings(timingStrategy);

					if (timingModule.equals("Planomat")) {
						testee.getConfig().plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarOnly/expected_plans.xml.gz");
					} else if (timingModule.equals("TimeAllocationMutator")) {
						testee.getConfig().plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateAll6AM/expected_plans.xml.gz");
					}

					testee.getConfig().controler().setOutputDirectory(
							outputDirectory
							+ "/"
							+ this.getRunOutputDirectoryName(timingModule, beta, learningRate));
					testee.getConfig().charyparNagelScoring().setBrainExpBeta(beta);
					testee.getConfig().charyparNagelScoring().setLearningRate(learningRate);

					testee.setCreateGraphs(false);
					testee.setWriteEventsInterval(10);
					testee.run();
				}

			}
		}

	}

	String getRunOutputDirectoryName(final String timingModule, final double beta, final double learningRate) {
		return
		"timingModule_" + timingModule + "/" +
		"brainExpBeta_" + Double.toString(beta) + "/" +
		"learningRate_" + Double.toString(learningRate);

	}

	/**
	 * @param config
	 *
	 * TODO this analysis doesnt work for big kti scenario plans files
	 * it crashes with a "GC overhead limit exceeded" exception
	 * there is probably a memory leak when streaming populations:
	 * when person is removed, knowledge and desires are NOT removed.
	 */
	public void analyzeLegDistanceDistribution(Config config) {

//		double[] distanceClasses = new double[]{
//			0,
//			100, 200, 500,
//			1000, 2000, 5000,
//			10000, 20000, 50000,
//			100000, 200000, 500000,
//			1000000};
//
//		ScenarioImpl sc = new ScenarioImpl(config);
//		ScenarioLoader loader = new ScenarioLoader(sc);
//		sc.getPopulation().setIsStreaming(true);
//		AbstractClassifiedFrequencyAnalysis pa = new PopulationLegDistanceDistribution(System.out);
//		sc.getPopulation().addAlgorithm(pa);
//
//		loader.loadScenario();

//		// - network
//		logger.info("Reading network xml file...");
//		NetworkLayer network = new NetworkLayer();
//		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
//		logger.info("Reading network xml file...done.");
//
//		// - facilities
//		logger.info("Reading facilities xml file...");
//		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
//		try {
//			new MatsimFacilitiesReader(facilities).parse(config.facilities().getInputFile());
//		} catch (SAXException e) {
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		Gbl.getWorld().setFacilityLayer(facilities);
//		logger.info("Reading facilities xml file...");

		// - population
//		ArrayList<PersonAlgorithm> plansAlgos = new ArrayList<PersonAlgorithm>();
//		plansAlgos.add(pa);

//		PopulationImpl matsimAgentPopulation = new PopulationImpl();
//		pop.setIsStreaming(true);
//		pop.addAlgorithm(pa);
//		PopulationReader plansReader = new MatsimPopulationReader(pop, sc.getNetwork());
//		plansReader.readFile(config.plans().getInputFile());

//		for (boolean isCumulative : new boolean[]{false, true}) {
//			for (CrosstabFormat crosstabFormat : CrosstabFormat.values()) {
//				pa.printClasses(crosstabFormat, isCumulative, distanceClasses);
//			}
//		}
//
//		pa.printDeciles(true);

	}


	public void analyzeModeChainFeasibility(Config config) {

		ScenarioImpl scenario = new ScenarioImpl();

		// initialize scenario with events from a given events file
		// - network
		logger.info("Reading network xml file...");
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		logger.info("Reading network xml file...done.");

		// - facilities
		logger.info("Reading facilities xml file...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		try {
			new MatsimFacilitiesReader(scenario).parse(config.facilities().getInputFile());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Reading facilities xml file...");

		// - population
		PersonAnalyzeModeChainFeasibility pa = new PersonAnalyzeModeChainFeasibility(facilities, network);
		ArrayList<PersonAlgorithm> plansAlgos = new ArrayList<PersonAlgorithm>();
		plansAlgos.add(pa);

		PopulationImpl matsimAgentPopulation = (PopulationImpl) scenario.getPopulation();
		matsimAgentPopulation.setIsStreaming(true);
		matsimAgentPopulation.addAlgorithm(pa);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());

		logger.info("Number of selected plans which are infeasible: " + pa.getNumInfeasiblePlans());
	}

	private class PersonAnalyzeModeChainFeasibility implements PersonAlgorithm {

		private int numInfeasiblePlans = 0;
		private final ActivityFacilities facilities;
		private final Network network;

		public PersonAnalyzeModeChainFeasibility(ActivityFacilities facilities, Network network) {
			super();
			this.facilities = facilities;
			this.network = network;
		}

		@Override
		public void run(Person person) {

			Plan selectedPlan = person.getSelectedPlan();

			ArrayList<String> modeChain = new ArrayList<String>();
			for (PlanElement pe : selectedPlan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					modeChain.add(leg.getMode());
				}
			}
			String[] candidate = new String[modeChain.size()];
			candidate = modeChain.toArray(candidate);

			MeisterkConfigGroup meisterkConfigGroup = new MeisterkConfigGroup();

			boolean isFeasible = PlanAnalyzeTourModeChoiceSet.isModeChainFeasible(
					selectedPlan,
					candidate,
					meisterkConfigGroup.getChainBasedModes(),
					PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility,
					this.facilities,
					this.network);

			if (!isFeasible) {

				logger.info("Agent id: " + person.getId());

				for (PlanElement pe : selectedPlan.getPlanElements()) {

					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						logger.info("\t" + act.getFacilityId());
					}

					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						modeChain.add(leg.getMode());
						logger.info("\t" + leg.getMode());
					}

				}
				this.numInfeasiblePlans++;
			}

		}

		public int getNumInfeasiblePlans() {
			return numInfeasiblePlans;
		}

	}

	public static void setPlansToSameDepTime(Config config) {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();

		ScenarioImpl scenario = loader.getScenario();
		Population population = scenario.getPopulation();

		PersonSetFirstActEndTime psfaet = new PersonSetFirstActEndTime(24.0 * 3600);
		psfaet.run(population);

		logger.info("Writing plans file...");
		new PopulationWriter(population, scenario.getNetwork()).write(scenario.getConfig().plans().getOutputFile());
		logger.info("Writing plans file...DONE.");
	}

	public static Population initMatsimAgentPopulation(final String inputFilename, final boolean isStreaming, final ArrayList<PersonAlgorithm> algos, ScenarioImpl scenario) {

		PopulationImpl population = (PopulationImpl) scenario.getPopulation();

		System.out.println("  reading plans xml file... ");
		population.setIsStreaming(isStreaming);

		if (isStreaming) {
			// add plans algos for streaming
			if (algos != null) {
				for (PersonAlgorithm algo : algos) {
					population.addAlgorithm(algo);
				}
			}
		}
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(inputFilename);
		population.printPlansCount();
		System.out.println("  done.");

		return population;
	}

	public static void readEvents(final EventsManagerImpl events, final NetworkLayer network, final String eventsInputFile) {

		// load test events
		long startTime, endTime;

		System.out.println("  reading events file and (probably) running events algos");
		startTime = System.currentTimeMillis();
		new MatsimEventsReader(events).readFile(eventsInputFile);
		endTime = System.currentTimeMillis();
		System.out.println("  done.");
		System.out.println("  reading events from file and processing them took " + (endTime - startTime) + " ms.");
		System.out.flush();

	}

	/**
	 * Used this routine for MeisterEtAl_Heureka_2008 paper,
	 * plot of number of deps, arrs by activity type to visualize
	 * the time distribution from microcensus.
	 */
	public static void analyseInitialTimes(Config config) {

		ScenarioImpl scenario = new ScenarioImpl(config);
		// initialize scenario with events from a given events file
		// - network
		logger.info("Reading network xml file...");
		new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
		logger.info("Reading network xml file...done.");
		// - population
		PersonAlgorithm pa = new PersonAnalyseTimesByActivityType(TIME_BIN_SIZE);
		ArrayList<PersonAlgorithm> plansAlgos = new ArrayList<PersonAlgorithm>();
		plansAlgos.add(pa);

		PopulationImpl matsimAgentPopulation = (PopulationImpl) scenario.getPopulation();
		matsimAgentPopulation.setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(scenario.getConfig().plans().getInputFile());
		matsimAgentPopulation.printPlansCount();
		int[][] numDeps = ((PersonAnalyseTimesByActivityType) pa).getNumDeps();
		MyRuns.writeAnArray(numDeps, "output/deptimes.txt");
		int[][] numArrs = ((PersonAnalyseTimesByActivityType) pa).getNumArrs();
		MyRuns.writeAnArray(numArrs, "output/arrtimes.txt");
		int[][] numTraveling = ((PersonAnalyseTimesByActivityType) pa).getNumTraveling();
		MyRuns.writeAnArray(numTraveling, "output/traveling.txt");

	}

	private static void writeAnArray(final int[][] anArray, final String filename) {

		File outFile = null;
		BufferedWriter out = null;

		outFile = new File(filename);

		try {
			out = new BufferedWriter(new FileWriter(outFile));

			boolean timesAvailable = true;
			int timeIndex = 0;

			out.write("#");
			for (int ii=0; ii < Activities.values().length; ii++) {
				out.write(Activities.values()[ii] + "\t");
			}
			out.newLine();

			while (timesAvailable) {

				timesAvailable = false;

				out.write(Time.writeTime(timeIndex * TIME_BIN_SIZE) + "\t");
				for (int aa=0; aa < anArray.length; aa++) {

					//					if (numDeps[aa][timeIndex] != null) {
					if (timeIndex < anArray[aa].length) {
						out.write(Integer.toString(anArray[aa][timeIndex]));
						timesAvailable = true;
					} else {
						out.write("0");
					}
					out.write("\t");
				}
				out.newLine();
				timeIndex++;
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
