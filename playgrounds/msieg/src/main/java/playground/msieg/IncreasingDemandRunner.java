/* *********************************************************************** *
 * project: org.matsim.*
 * IncreasingDemandRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.msieg;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;

import playground.msieg.cmcf.BestFitTimeRouter;
import playground.msieg.cmcf.CMCFDemandWriter;
import playground.msieg.cmcf.CMCFNetworkWriter;
import playground.msieg.cmcf.RandomPlansGenerator;

public class IncreasingDemandRunner {
	/***
	 * This is to perform the following test cases:
	 * In a top-Level-Directory, there must be a config file.
	 * 1) This config file is read and the network is converted to CMCF format.
	 * 2) A plans file is generated with a specific amount of agents, a specific config file is generated
	 * 3) A demand file for that plan is generated
	 * 4) CMCF is used for solving the resulting flow problem
	 * 5) This solution is used to adopt the generated Plansfile
	 * 6) A new config file based on the old one is created
	 *
	 * _____The last step is not executed, since it just mean to call the created config files:
	 * 7) CMCF is started with the old and new config file, output directories are mutual
	 */

	static String cmcfCommand = "/homes/combi/msieg/cmcf/cmcf";
	static String[] cmcfOptions = { "nopt", "-C", "-K", /*"-T",*/ "-i", "1000", "-G", "0.001"};

	//just to force the whole class deterministic behaviour for debugging
	static long seed = 31081983L;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println("At least a config file is needed, usage: java ... config.xml");
			System.exit(1);
		}
		File configFile = new File(args[0]);
		if(!configFile.exists())
		{
			System.out.println("Config-File "+args[0]+" seems not to exist. Aborting.");
			System.exit(2);
		}
		//createAllConfigs(args[0]);
		String cfgDir = new File(args[0]).getAbsolutePath();
		cfgDir = cfgDir.substring(0, cfgDir.lastIndexOf(File.separatorChar));
		runAllConfigsInDirectory(new File(cfgDir));

	}

	private static void runAllConfigsInDirectory(File dir){
		String[] cfgFiles = dir.list(new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("config") && arg1.endsWith(".xml") && !arg1.equals("config.xml");
			}
		});
		Arrays.sort(cfgFiles);
		for(String cfgFile: cfgFiles){
			final Controler controler = new Controler(new File(dir, cfgFile).getAbsolutePath());
			controler.setOverwriteFiles(true);
			try {
				controler.run();
				controler.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//String[] args = { new File(dir, cfgFile).getAbsolutePath() };
			//Controler.main(args);
		}
	}

	private static void createAllConfigs(String cfgFile) {
		String topDir = cfgFile.lastIndexOf(File.separatorChar) == -1 ?
						"." : cfgFile.substring(0, cfgFile.lastIndexOf(File.separatorChar));

		Gbl.createConfig(new String[] { cfgFile, "config_v1.dtd" });

		String  //cfgFile = args[0],
				netFile = new File(Gbl.getConfig().network().getInputFile()).getAbsolutePath(),
				popFile = null;

		String 	cmcfNetwork = new File(netFile).getAbsolutePath(),
				cmcfNetName = cmcfNetwork.substring(
						cmcfNetwork.lastIndexOf(File.separatorChar),
						cmcfNetwork.lastIndexOf('.')),
				cmcfDemand;

		//Step 1:
		cmcfNetwork = cmcfNetwork.substring(0, cmcfNetwork.lastIndexOf('.'))+"_cmcf.xml";
		try{
			CMCFNetworkWriter cnw = new CMCFNetworkWriter(netFile);
			cnw.read();
			Writer netOut = new FileWriter(cmcfNetwork);
			cnw.setNetName(cmcfNetName);
			cnw.convert(netOut);
		}catch (IOException ioe) {
			System.out.println("Could not read network file as specified in config file, aborting.");
			System.exit(3);
		}

		int minAgents = 1000, maxAgents = 10000, step = 1000, timeSteps = 6;
		//for each amount of agents perform steps 2-7:
		for(int agents = minAgents; agents <= maxAgents; agents += step)
		{
			//Step 2: create plans and new config
			RandomPlansGenerator rpg = new RandomPlansGenerator(netFile);
			rpg.setSeed(seed);
			PopulationImpl randPop = rpg.createPlans(agents);
			popFile = topDir+File.separatorChar+"plans"+agents+"random.xml";
			rpg.writePlans(randPop, popFile);

			Gbl.getConfig().controler().setOutputDirectory(topDir+File.separatorChar+"out"+agents+"random");
			Gbl.getConfig().plans().setInputFile(popFile);
			new ConfigWriter(Gbl.getConfig()).writeFile(topDir+File.separatorChar+"config"+agents+"rand.xml");

			//Step 3: create demand file for cmcf
			CMCFDemandWriter cdw = new CMCFDemandWriter(netFile, popFile);
			cmcfDemand = popFile.substring(0, popFile.lastIndexOf('.'))+"_cmcf.xml";
			cdw.setInputNetwork(cmcfNetName);
			try {
				cdw.convert(new FileWriter(cmcfDemand), timeSteps);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			//Step 4: run cmcf now with javas exec()-method
			String cmcfSolution = popFile.substring(0, popFile.lastIndexOf('.'))+".cmcf";
			String cmd = cmcfCommand;
			for(int i=0; i< cmcfOptions.length; i++)
				cmd += " " + cmcfOptions[i];
			cmd += " -w "+cmcfSolution;
			cmd += " "+cmcfNetwork+" "+cmcfDemand+" "+9999;
			cmd += " > "+cmcfSolution+".out";
			try {
				Runtime run = Runtime.getRuntime();
				Process pr = run.exec(cmd);
				pr.waitFor();
				/*
				//if output desired, leave that:
				BufferedReader buf = new BufferedReader( new InputStreamReader( pr.getInputStream() ) );
				String line;
				while ( (line = buf.readLine()) != null )
					System.out.println(line);
				*/
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not execute CMCF, skipping this iteration...");
				continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Process of CMCF run has been interrupted, skipping this iteration...");
				continue;
			}

			//Step 5: rerouting agents according to cmcf Solution;
			BestFitTimeRouter bft = new BestFitTimeRouter(netFile, popFile, cmcfSolution, timeSteps);
			try{
				bft.read();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.out.println("CMCF Solution not found, skipping iteration.");
				continue;
			}
			bft.route();
			popFile = topDir+File.separatorChar+"plans"+agents+"routed.xml";
			bft.writePlans(popFile);

			//Step 6: create new config File
			Gbl.getConfig().controler().setOutputDirectory(topDir+File.separatorChar+"out"+agents+"routed");
			Gbl.getConfig().plans().setInputFile(popFile);
			new ConfigWriter(Gbl.getConfig()).writeFile(topDir+File.separatorChar+"config"+agents+"rout.xml");
		}
	}
}

