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

import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.events.Events;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.socialnetworks.algorithms.EventsMapStartEndTimes;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

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

//	private static final String output_directory="D:/eclipse_workspace/matsim/output/EventsInt5_10/timecorr/";
//	private static final String input_directory="D:/eclipse_workspace/matsim/output/EventsInt5_10/";
	private static final String thisrun = "3_HC_4";
//	private static final String output_directory="D:/SocialNetsFolder/SNController4/"+thisrun+"/timecorr/";//AnalyzeTimeCorrelation
//	private static final String input_directory="D:/SocialNetsFolder/SNController4/"+thisrun+"/";//AnalyzeTimeCorrelation
	private static final String output_directory="/data/matsim/jhackney/results/matsim/SNController4/"+thisrun+"/timecorr/";//AnalyzeTimeCorrelation
	private static final String input_directory="/data/matsim/jhackney/results/matsim/SNController4/"+thisrun+"/";//AnalyzeTimeCorrelation
	private static final String out2 = thisrun+".out";
	private static final String out1 = "AgentsAtActivities"+thisrun+".out";

	private static final Config config= Gbl.createConfig(null);
	private static final World world= Gbl.createWorld();
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

	public static final Config setUpScenarioConfig() {
//		config = Gbl.createConfig(null);

		config.config().setOutputFile(output_directory + "output_config.xml");

		config.world().setInputFile(input_directory + "output_world.xml.gz");
		config.world().setOutputFile(output_directory + "output_world.xml");

		config.network().setInputFile(input_directory + "output_network.xml.gz");
		config.network().setOutputFile(output_directory + "output_network.xml");

		config.facilities().setInputFile(input_directory + "output_facilities.xml.gz");
		config.facilities().setOutputFile(output_directory + "output_facilities.xml");

		config.matrices().setInputFile(input_directory + "matrices.xml");
		config.matrices().setOutputFile(output_directory + "output_matrices.xml");

//		config.plans().setInputFile(input_directory + "output_plans.xml.gz");
		config.plans().setInputFile("output_plans.xml.gz");
//		config.plans().setInputFile("plans.xml.gz");//AnalyzeScores
		config.plans().setOutputFile(output_directory + "output_plans.xml.gz");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);

		config.counts().setCountsFileName(input_directory + "counts.xml");
		config.counts().setOutputFile(output_directory + "output_counts.xml.gz");

		config.events().setInputFile("events.txt");

		config.socnetmodule().setInDirName(input_directory);
		config.socnetmodule().setOutDir(output_directory);
//		config.socnetmodule().setSocNetGraphAlgo("none");
		config.socnetmodule().setSocNetGraphAlgo("read");//AnalyzeScores
		config.socnetmodule().setSocNetLinkRemovalP("0");
		config.socnetmodule().setSocNetLinkRemovalAge("0");
		config.socnetmodule().setDegSat("0");
		config.socnetmodule().setEdgeType("UNDIRECTED");
//		config.socnetmodule().setInitIter("0");
		config.socnetmodule().setInitIter("0");
		config.socnetmodule().setReadMentalMap("true");
//		config.socnetmodule().setBeta1("0");
//		config.socnetmodule().setBeta2("0");
//		config.socnetmodule().setBeta3("0");
//		config.socnetmodule().setBeta4("0");

		config.createModule("kml21");
		config.getModule("kml21").addParam("outputDirectory", output_directory);
		config.getModule("kml21").addParam("outputEgoNetPlansKMLMainFile","egoNetKML" );
		config.getModule("kml21").addParam("outputKMLDemoColoredLinkFile", "egoNetLinkColorFile");
		config.getModule("kml21").addParam("useCompression", "true");

		return config;
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
		new MatsimWorldReader(world).readFile(config.world().getInputFile());
		System.out.println("  done.");
		return world;
	}

	public static final ActivityFacilities readFacilities() {
		System.out.println("  reading facilities xml file... ");
		ActivityFacilities facilities = (ActivityFacilities)world.createLayer(ActivityFacilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");
		return facilities;
	}

	public static final NetworkLayer readNetwork() {
		System.out.println("  reading the network xml file...");
		System.out.println(Gbl.getConfig().network().getInputFile());
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
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

	public static final Population readPlans(final Network network) {
		System.out.println("  reading plans xml file... ");
		Population plans = new PopulationImpl();
		System.out.println(config.plans().getInputFile());
		new MatsimPopulationReader(plans, network).readFile(config.plans().getInputFile());

		System.out.println("  done.");
		return plans;
	}
	public static final Population readPlans(final Network network, final int i) {
		System.out.println("  reading plans xml file... ");
		Population plans = new PopulationImpl();
//		String filename=input_directory +"ITERS/it."+i+"/"+i+"."+Gbl.getConfig().plans().getInputFile();
		String filename=input_directory +config.plans().getInputFile();
		System.out.println(filename);
		new MatsimPopulationReader(plans, network).readFile(filename);

		System.out.println("  done.");
		return plans;
	}

	public static final Events readEvents(final int i, final EventsMapStartEndTimes epp) {
		System.out.println("  reading plans xml file... ");
		String filename=input_directory +"ITERS/it."+i+"/"+i+"."+config.events().getInputFile();
//		String filename=input_directory +"ITERS/it."+i+"/"+i+".events.txt";
		Events events = new Events();
		events.addHandler(epp);
		System.out.println(filename);
		new MatsimEventsReader(events).readFile(filename);

		System.out.println("  done.");
		return events;
	}

	public static final Events readEvents(final int i, final EventsMapStartEndTimes epp, final EventsToScore scoring) {
		System.out.println("  reading plans xml file... ");
//		String filename=input_directory +"ITERS/it."+i+"/"+i+"."+Gbl.getConfig().events().getInputFile();
		String filename=input_directory +"ITERS/it."+i+"/"+i+".events.txt";
		Events events = new Events();
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

	public static final void writePlans(final Population plans) {
		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans).write();
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

	public static final void writeNetwork(final NetworkLayer network) {
		System.out.println("  writing network xml file... ");
		new NetworkWriter(network).write();
		System.out.println("  done.");
	}

	public static final void writeFacilities(final ActivityFacilities facilities) {
		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write();
		System.out.println("  done.");
	}

	public static final void writeWorld(final World world) {
		System.out.println("  writing world xml file... ");
		new WorldWriter(world).write();
		System.out.println("  done.");
	}

	public static final void writeConfig() {
		System.out.println("  writing config xml file... ");
		new ConfigWriter(config).write();
		System.out.println("  done.");
	}
	public static Config getConfig(){
		return config;
	}
	public static World getWorld() {
		return world;
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

