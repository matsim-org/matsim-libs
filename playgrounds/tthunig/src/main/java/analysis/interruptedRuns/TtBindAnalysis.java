/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis.interruptedRuns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * @author tthunig
 *
 */
public class TtBindAnalysis implements IterationEndsListener{

	private static final Logger log = Logger.getLogger(TtBindAnalysis.class);
	
	private Scenario scenario;
	private TtWriteAnalysis writer;
	
	public TtBindAnalysis(Scenario scenario, TtAbstractAnalysisTool analyzer) {
		this.scenario = scenario;
		
		// create writer
		writer = new TtWriteAnalysis(scenario, analyzer);
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// write analyzed data
		writer.writeIterationResults(event.getIteration());

		// plot route distribution for every last iteration
		if (event.getIteration() == scenario.getConfig().controler().getLastIteration()) {
			String pathToInput = scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + event.getIteration() + "/analysis";
			String relPathToGnuplotScript = "../../../../../analysis/plot_routeDistribution.p";
			runGnuplotScript(pathToInput, relPathToGnuplotScript);
		}
	}
	
	public void runFinished() {
		// close overall writing stream
		writer.closeAllStreams();
		
		// plot results
		String pathToInput = scenario.getConfig().controler().getOutputDirectory() + "analysis";
		String relPathToGnuplotScript = "../../../analysis/plot_routesAndTTs.p";
		runGnuplotScript(pathToInput, relPathToGnuplotScript);
	}
	
	/**
	 * starts the gnuplot script from the specific input directory
	 */
	private void runGnuplotScript(String pathToInput, String relativePathToGnuplotScript){		
		log.info("execute command: cd " + pathToInput);
		log.info("and afterwards: gnuplot " + relativePathToGnuplotScript);
		
		try {
			// "&" splits different commands in one line in windows. Use ";" if you are a linux user.
			ProcessBuilder builder = new ProcessBuilder( "cmd", "/c", "cd", pathToInput, "&", "gnuplot", relativePathToGnuplotScript);
			Process p = builder.start();

			// print command line infos and errors:
			BufferedReader read = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String zeile;
			while ((zeile = read.readLine()) != null) {
				log.error("input stream: " + zeile);
			}			
			read = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((zeile = read.readLine()) != null) {
				log.error("error: " + zeile);
			}
		} catch (IOException e) {
			log.error("ERROR while executing gnuplot command.");
			e.printStackTrace();
		}
	}

}
