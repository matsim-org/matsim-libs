/* *********************************************************************** *
 * project: org.matsim.*
 * PlanVkmCalculatorUsingRunnable.java
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

package playground.southafrica.population.analysis.vkt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.openstreetmap.network.RoadTypeParser;

public class PlanVktCalculator {
	private final static Logger LOG = Logger.getLogger(PlanVktCalculator.class);
	private final int BLOCK_SIZE = 100;
	private static long lowerIdLimit = 0;
	private static long upperIdLimit = Long.MAX_VALUE;
	private Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(PlanVktCalculator.class.toString(), args);
		
		String plansfile = args[0];
		String networkFile = args[1];
		String outputFolder = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		String osmFile = args[4];
		lowerIdLimit = Long.parseLong(args[5]);
		upperIdLimit = Long.parseLong(args[6]);
		
		/* Parse the road types from OSM */
		RoadTypeParser rtp = new RoadTypeParser();
		Map<Long, String> roadTypeMap = null;
		try {
			roadTypeMap = rtp.parseRoadType(osmFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Couldn't find the OSM file " + osmFile);
		}
		
		/* Read and update the network. */
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader mnr = new MatsimNetworkReader(scenario.getNetwork());
		mnr.parse(networkFile);		
		LOG.info("Updating road types... (" + scenario.getNetwork().getLinks().size() + " links)");
		Counter linkCounter = new Counter("  links # ");
		for(Id linkId : scenario.getNetwork().getLinks().keySet()){
			LinkImpl link = (LinkImpl) scenario.getNetwork().getLinks().get(linkId);
			link.setType(roadTypeMap.get(Long.parseLong(link.getOrigId())));
			if(link.getType() == null){
				LOG.warn("Couldn't change the road type of link " + link.getOrigId());
			}
			linkCounter.incCounter();
		}
		linkCounter.printCounter();
		
		/* Read the population file. */
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
		mpr.parse(plansfile);

		/* The actual stuff to run the Vkt analysis. */
		DigicoreNetworkRouterFactory factory = new DigicoreNetworkRouterFactory(scenario);
		PlanVktCalculator pvc = new PlanVktCalculator(scenario);
		pvc.calculateVkt(outputFolder, numberOfThreads, factory);
		pvc.consolidateOutput(outputFolder);
		
		Header.printFooter();
	}
	
	
	public PlanVktCalculator(Scenario scenario) {
		this.sc = scenario;
	}
	
	
	/**
	 * Analyses the selected plans to calculate the vehicle kilometres 
	 * travelled (vkt). This method calls a multi-threaded class which, in turn,
	 * creates temporary files for each plan. It is necessary to call the 
	 * method {@link #consolidateOutput(String)} after this method.
	 * @param outputFolder the absolute path of the folder where temporary files
	 * 		  will be written to.
	 * @param numberOfThreads the number of threads to use.
	 * @param factory the router factory that will be used to generate routers
	 * 		  if the plan does not already have {@link Leg}s with {@link Route}s.
	 */
	public void calculateVkt(String outputFolder, int numberOfThreads, DigicoreNetworkRouterFactory factory){

		/* Create a list of all selected plans. */
		LOG.info("Creating list of plans...");
		List<Plan> plans = new ArrayList<Plan>(this.sc.getPopulation().getPersons().size());
		for(Id person : this.sc.getPopulation().getPersons().keySet()){
			long id = Long.parseLong(person.toString());
			if(id >= lowerIdLimit && id < upperIdLimit){
				Plan plan = sc.getPopulation().getPersons().get(person).getSelectedPlan();
				if(plan != null){
					plans.add(plan);
				} else{
					LOG.error("   Person " + person.toString() + " does not have a selected plan... person will be ignored!");
				}
			}
		}
		LOG.info("Performing multi-threaded Vkt analysis... (" + plans.size() + " plans)");
		
		/* Systematically execute plans in blocks. */
		Counter counter = new Counter("  plans # ");
		ExecutorService threadExecutor = null;
		while(plans.size() > 0){
			int blockCounter = 0;
			threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
			while(blockCounter < BLOCK_SIZE && plans.size() > 0){
				PlanVktCalculatorRunnable pvr = new PlanVktCalculatorRunnable(plans.get(0), factory, outputFolder, counter);
				threadExecutor.execute(pvr);
				plans.remove(0);
				blockCounter++;
			}
			threadExecutor.shutdown();
			while(!threadExecutor.isTerminated()){
			}			
		}
		counter.printCounter();
		LOG.info("Done with multi-threaded Vkt analysis.");
	}
	
	
	/**
	 * Consolidates the *.tmp files created by the {@link Runnable} class
	 * {@link PlanVktCalculatorRunnable#run()}.
	 * @param outputFolder absolute path where the *.tmp files were written to.
	 */
	public void consolidateOutput(String outputFolder){
		File folder = new File(outputFolder);
		List<File> files = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".tmp"));
		Counter counter = new Counter("  files # ");
		
		LOG.info("Consolidate output... (" + files.size() + " files)");
		LOG.info("Convert files to pathnames");
		List<String> list = new ArrayList<String>(files.size());
		for(File f : files){
			if(f.exists() && f.canRead()){
				list.add(f.getAbsolutePath());
			}
		}
		
		String outputFile = null;
		if(folder.getParent() == null){
			outputFile = "./" + folder.getName() + "_VKT.csv";
		} else{
			outputFile = folder.getParent() + "/" + folder.getName() + "_VKT.csv";			
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("Id,Vkt,numberOfActivities,freeway,arterial,street,other");
			bw.newLine();
			for(String s : list){
				File f = new File(s);
				BufferedReader br = IOUtils.getBufferedReader(s);
				try{
					String line = null;
					while((line = br.readLine()) != null && line.split(",").length == 7){
						bw.write(line);
						bw.newLine();
					}
				} finally{
					try {
						br.close();
					} catch (IOException e) {
						throw new RuntimeException("Could not close BufferedReader " + outputFile);
					}
					f.delete();
					counter.incCounter();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to " + outputFile);			
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + outputFile);
			}
		}
		counter.printCounter();
		LOG.info("Done consolidating output.");		
	}

}

