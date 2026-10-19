/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
/**
 * 
 */
package org.matsim.codeexamples.programming.multipleJvmMatsimRuns;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * This class is the workhorse. It will set up a MATSim run; start it; and
 * process the outputs. In this the outputs is a list of utilities (scores),
 * each entry being the score of the selected {@link Plan} of a {@link Person} 
 * in the standard <i>equil</i> example.  
 * 
 * @author jwjoubert
 */
public class MultipleJvmMatsimCallable implements Callable<List<Double>> {
	final private Logger log = LogManager.getLogger(MultipleJvmMatsimCallable.class);
	final File folder;
	
	public MultipleJvmMatsimCallable(String foldername) {
		this.folder = new File(foldername);
		boolean success = this.folder.mkdirs();
		if(!success && !this.folder.exists()) {
			throw new RuntimeException("Could not create the run's folder in '" + foldername + "'");
		}
	}

	@Override
	public List<Double> call() throws Exception {
		List<Double> result = new ArrayList<>();
		
		/* Set up the folder structure. This entails copying the package to the
		 * current folder and unzipping it. TODO Not sure if the unzipping 
		 * always works on a Windows machine. */
		File fileFrom = new File("./target/matsim-code-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
		File fileTo = new File(this.folder.getAbsoluteFile() + "/package.jar");
		Files.copy(fileFrom.toPath(), fileTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		/* For a more complex job you may have to copy other input files as well. */
		
		/* Run the job. */
		ProcessBuilder equilBuilder = new ProcessBuilder(
				"java",
				"-Xmx512m",
				"-cp",
				"package.jar",
				"org.matsim.codeexamples.programming.multipleJvmMatsimRuns.MultipleJvmBlackBox"
				);
		equilBuilder.directory(this.folder);
		log.info("Builder: " + equilBuilder.command().toString());
		equilBuilder.redirectErrorStream(true);
		final Process equilProcess = equilBuilder.start();
		log.info("Process started...");
		BufferedReader br = new BufferedReader(new InputStreamReader(equilProcess.getInputStream()));
		String line;
		while((line = br.readLine()) != null) {
			/* You can comment the following line out if you want less garbage. */
			log.info( folder.getName() + " ===> " + line);
		}
		int equilExitCode = equilProcess.waitFor();
		log.info("Process ended. Exit status '" + equilExitCode + "'");
		if(equilExitCode != 0) {
			log.error("Could not complete Equil run for '" + folder.getName() + "'");
		}
		
		/* Analyse the output. That is, read in the output population file and
		 * add each person's selected plan's score. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(folder.getAbsolutePath() + "/output/output_plans.xml.gz");
		for(Person person : sc.getPopulation().getPersons().values()) {
			result.add(person.getSelectedPlan().getScore());
		}
		
		/* Clean up. It's likely inefficient to keep these simulation output
		 * folders and files around. */
		
		return result;
	}
}
