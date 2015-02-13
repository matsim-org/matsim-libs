/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.PLOC.analysis.postprocessing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CompareScenarios {	
	public String outpath;
	private ScenarioImpl baseScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private int numberOfAnalyses;
	private String path;
	private String networkFilePath;
	private String facilitiesFilePath;
	private String plansFileName;
	
	private final static Logger log = Logger.getLogger(CompareScenarios.class);

	public static void main(String[] args) {
		CompareScenarios comparator = new CompareScenarios();
		comparator.run(args[0]);
		log.info("Comparisons finished ...");
	}
	
	private void readConfig(String configFile) {
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
 
	          this.networkFilePath = bufferedReader.readLine();
	          this.facilitiesFilePath = bufferedReader.readLine();
	          this.numberOfAnalyses = Integer.parseInt(bufferedReader.readLine());
	          this.path = bufferedReader.readLine();
	          this.plansFileName = bufferedReader.readLine();
	          this.outpath = bufferedReader.readLine();
	        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
	}
	
	private void init(String configFile) {
		this.readConfig(configFile);
		new File(outpath).mkdirs();
		new MatsimNetworkReader(baseScenario).readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(baseScenario).readFile(facilitiesFilePath);
	}
		
	public void run(String pathsFile) {
		this.init(pathsFile);
		
		CompareScores scoreComparator = new CompareScores(this.outpath);
		scoreComparator.openScoresFile(this.outpath + "/scores.txt");
		
		for (int i = 0; i < this.numberOfAnalyses; i++) {
			String p = this.path + "/" + i + "/" + this.plansFileName;
			log.info("reading: " + p);
			this.readPopulation(p);
			scoreComparator.handleScenario(this.baseScenario);
		}
		scoreComparator.closeScoresFile();
		scoreComparator.printScores();
		
		scoreComparator.compareScores(this.outpath + "/scores.txt", 
				this.outpath + "/scoresStandardDeviationsInPercent.txt");
		
//		CompareDestinations destinationComparator = new CompareDestinations();
//		for (int i = 0; i < this.numberOfAnalyses; i++) {
//			String p = this.path + "/interrun/" + i + "/" + this.plansFileName;
//			log.info("reading: " + p);
//			this.readPopulation(p);
//			destinationComparator.handleScenario(this.baseScenario);
//		}
//		DecimalFormat formatter = new DecimalFormat("0.0000000");
//		log.info("Distances from center point: " + formatter.format(destinationComparator.evaluateScenarios()) + "[m]");
		
		LinkVolumesComparator volumesAnalyzer = new LinkVolumesComparator(numberOfAnalyses, this.path, this.outpath, 
				this.baseScenario.getNetwork());
		volumesAnalyzer.run();		
	}
	
	private void readPopulation(String populationFilePath) {
        this.baseScenario.setPopulation(PopulationUtils.createPopulation(this.baseScenario.getConfig(), this.baseScenario.getNetwork()));
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.baseScenario);
		populationReader.readFile(populationFilePath);
	}
	
	public static Plan getBestPlan(Person person) {
		double highestScore = Double.MIN_VALUE;
		int bestPlanIndex = 0;
		
		int cnt = 0;
		for (Plan plan : person.getPlans()) {
			if (plan.getScore() > highestScore) {
				highestScore = plan.getScore();
				bestPlanIndex = cnt;
			}
			cnt++;
		}
		return person.getPlans().get(bestPlanIndex);
 	}
}
