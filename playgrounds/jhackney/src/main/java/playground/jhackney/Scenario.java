/* *********************************************************************** *
 * project: org.matsim.*
 * Scenario.java
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

package playground.jhackney;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;

public abstract class Scenario {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	// For KMZ drawings of final iteration: needs ActivityActMap500.txt, edge and agent.txt (for iter 500)
//	private static final String output_directory = "D:/SocialNetsFolder/TRB/Analyses/TRB5/postprocessing/";
//	private static final String input_directory = "D:/SocialNetsFolder/TRB/TRB5/";

	//For TRB run analyses of 500 iterations
//	private static final String output_directory = "D:/SocialNetsFolder/TRB/Analyses/Config1/";
//	private static final String input_directory = "D:/SocialNetsFolder/TRB/Config1/";
//	private static final String output_directory="output/Analyses/TRB6/";//AnalyzeScores
//	private static final String input_directory="output/TRB6/";//AnalyzeScores

//	private static final String output_directory="D:/eclipse_workspace/matsim/output/TRB1_501_F2F/timecorr/";
//	private static final String input_directory="D:/eclipse_workspace/matsim/output/TRB1_501_F2F/";

	private static final String thisrun = "TRB1_HC";
	private static final String output_directory="D:/SocialNetsFolder/HC/"+thisrun+"/timecorr/";//AnalyzeTimeCorrelation
	private static final String input_directory="D:/SocialNetsFolder/HC/"+thisrun+"/";//AnalyzeTimeCorrelation

//	private static final String output_directory="/data/matsim/jhackney/results/matsim/SNController4/"+thisrun+"/timecorr/";//AnalyzeTimeCorrelation
//	private static final String input_directory="/data/matsim/jhackney/results/matsim/SNController4/"+thisrun+"/";//AnalyzeTimeCorrelation
	private static final String out2 = thisrun+".out";
	private static final String out1 = "AgentsAtActivities"+thisrun+".out";

	private static final ScenarioImpl scenario = new ScenarioImpl();
//	private static final Config config= Gbl.createConfig(null);
//	private static final World world= Gbl.createWorld();
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	private Scenario() {
	}

	//////////////////////////////////////////////////////////////////////
	// setup
	//////////////////////////////////////////////////////////////////////

	public static final SocNetConfigGroup setUpScenarioConfig() {
//		config = Gbl.createConfig(null);
		SocNetConfigGroup snConfig = new SocNetConfigGroup();
		Config config = scenario.getConfig();
		config.addModule(SocNetConfigGroup.GROUP_NAME, snConfig);

		config.network().setInputFile(input_directory + "output_network.xml.gz");

		config.facilities().setInputFile(input_directory + "output_facilities.xml.gz");
		config.facilities().setOutputFile(output_directory + "output_facilities.xml");

		config.plans().setInputFile(input_directory + "output_plans.xml.gz");
//		config.plans().setInputFile("output_plans.xml.gz");
//		config.plans().setInputFile("plans.xml.gz");//AnalyzeScores
		config.plans().setOutputFile(output_directory + "output_plans.xml.gz");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);

		config.counts().setCountsFileName(input_directory + "counts.xml");

		snConfig.setInDirName(input_directory);
		snConfig.setOutDir(output_directory);
//		config.socnetmodule().setSocNetGraphAlgo("none");
		snConfig.setSocNetGraphAlgo("read");//AnalyzeScores
		snConfig.setSocNetLinkRemovalP("0");
		snConfig.setSocNetLinkRemovalAge("0");
		snConfig.setDegSat("0");
		snConfig.setEdgeType("UNDIRECTED");
//		config.socnetmodule().setInitIter("0");
		snConfig.setInitIter("0");
		snConfig.setReadMentalMap("true");
//		config.socnetmodule().setBeta1("0");
//		config.socnetmodule().setBeta2("0");
//		config.socnetmodule().setBeta3("0");
//		config.socnetmodule().setBeta4("0");

		config.createModule("kml21");
		config.getModule("kml21").addParam("outputDirectory", output_directory);
		config.getModule("kml21").addParam("outputEgoNetPlansKMLMainFile","egoNetKML" );
		config.getModule("kml21").addParam("outputKMLDemoColoredLinkFile", "egoNetLinkColorFile");
		config.getModule("kml21").addParam("useCompression", "true");

		return snConfig;
	}

	//////////////////////////////////////////////////////////////////////
	// read input
	//////////////////////////////////////////////////////////////////////

//	public static final Config readConfig(){
//		System.out.println("  Reading Config xml file ... ");
//		try {
//			new MatsimConfigReader(config).readFile(configFileName, dtdFileName);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.out.println("  Done");
//		return Gbl.getConfig();
//	}
	public static final World readWorld() {
		System.out.println("  reading world xml file... ");
		new MatsimWorldReader(scenario).readFile(scenario.getConfig().world().getInputFile());
		System.out.println("  done.");
		return scenario.getWorld();
	}

	public static final ActivityFacilitiesImpl readFacilities() {
		System.out.println("  reading facilities xml file... ");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(scenario.getConfig().facilities().getInputFile());
		System.out.println("  done.");
		return facilities;
	}

	public static final NetworkLayer readNetwork() {
		System.out.println("  reading the network xml file...");
		System.out.println(scenario.getConfig().network().getInputFile());
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
		System.out.println("  done.");
		return network;
	}
//
//	public static final Counts readCounts() {
//		System.out.println("  reading the counts...");
//		final Counts counts = new Counts();
//		new MatsimCountsReader(counts).readFile(Gbl.getConfig().counts().getCountsFileName());
//		System.out.println("  done.");
//		return counts;
//	}
//
//	public static final Matrices readMatrices() {
//		System.out.println("  reading matrices xml file... ");
//		new MatsimMatricesReader(Matrices.getSingleton(), Gbl.getWorld()).readFile(Gbl.getConfig().matrices().getInputFile());
//		System.out.println("  done.");
//		return Matrices.getSingleton();
//	}

	public static final Population readPlans() {
		System.out.println("  reading plans xml file... ");
		Population plans = scenario.getPopulation();
		System.out.println(scenario.getConfig().plans().getInputFile());
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		System.out.println("  done.");
		return plans;
	}

//	public static final Population readPlans(final int i) {
//		System.out.println("  reading plans xml file... ");
//		Population plans = scenario.getPopulation();
////		String filename=input_directory +"ITERS/it."+i+"/"+i+"."+Gbl.getConfig().plans().getInputFile();
//		String filename=input_directory +scenario.getConfig().plans().getInputFile();
//		System.out.println(filename);
//		new MatsimPopulationReader(scenario).readFile(filename);
//
//		System.out.println("  done.");
//		return plans;
//	}

	public static final Population readPlansAndKnowledges() {
		System.out.println("  reading plans xml file... ");
		Population plans = scenario.getPopulation();
		System.out.println(scenario.getConfig().plans().getInputFile());
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		System.out.println("  done.");
		return plans;
	}

	public static final EventsManagerImpl readEvents(final int i, final EventsMapStartEndTimes epp) {
		System.out.println("  reading plans xml file... ");
		String filename=input_directory +"ITERS/it."+i+"/"+i+".events.txt";
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(epp);
		System.out.println(filename);
		new MatsimEventsReader(events).readFile(filename);

		System.out.println("  done.");
		return events;
	}

	public static final EventsManagerImpl readEvents(final int i, final EventsMapStartEndTimes epp, final EventsToScore scoring) {
		System.out.println("  reading plans xml file... ");
		String filename=input_directory +"ITERS/it."+i+"/"+i+".events.txt";
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(epp);
		events.addHandler(scoring);
		System.out.println(filename);
		new MatsimEventsReader(events).readFile(filename);

		System.out.println("  done.");
		return events;
	}

	//////////////////////////////////////////////////////////////////////
	// write output
	//////////////////////////////////////////////////////////////////////

	public static final void writePlans(final Population plans, final Network network) {
		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans, network).write(scenario.getConfig().plans().getOutputFile());
		System.out.println("  done.");
	}
//
//	public static final void writeMatrices(final Matrices matrices) {
//		System.out.println("  writing matrices xml file... ");
//		new MatricesWriter(matrices).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeCounts(final Counts counts) {
//		System.out.println("  writing counts xml file... ");
//		new CountsWriter(counts).write();
//		System.out.println("  done.");
//	}

	public static final void writeNetwork(final NetworkLayer network, String filename) {
		System.out.println("  writing network xml file... ");
		new NetworkWriter(network).write(filename);
		System.out.println("  done.");
	}

	public static final void writeFacilities(final ActivityFacilitiesImpl facilities) {
		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(scenario.getConfig().facilities().getOutputFile());
		System.out.println("  done.");
	}

	public static Config getConfig(){
		return scenario.getConfig();
	}
	public static World getWorld() {
		return scenario.getWorld();
	}
	public static ScenarioImpl getScenarioImpl() {
		return scenario;
	}
	public static Knowledges getKnowledges() {
		return scenario.getKnowledges();
	}
	public static String getSNOutDir(){
		return output_directory;
	}
	public static String getSNInDir(){
		return input_directory;
	}
	public static String getOut1(){
		return output_directory + out1;
	}
	public static String getOut2(){
		return output_directory + out2;
	}
}

