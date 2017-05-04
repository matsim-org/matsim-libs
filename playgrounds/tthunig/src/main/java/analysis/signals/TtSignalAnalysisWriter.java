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
package analysis.signals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;

import com.google.inject.Inject;

/**
 * Class to write some signal analysis of the analyze tool that is given in the constructor.
 *
 * @author tthunig
 */
public class TtSignalAnalysisWriter {

	private static final Logger log = Logger.getLogger(TtSignalAnalysisWriter.class);
	
	private TtSignalAnalysisTool handler;
	private String outputDirBase;
	private PrintStream totalGreenOverItWritingStream;
	private PrintStream avgGreenPerCycOverItWritingStream;
	private int lastIteration;
	private SignalsData signalsData;
	
	@Inject
	public TtSignalAnalysisWriter(Scenario scenario, TtSignalAnalysisTool handler) {
		this.handler = handler;
		this.outputDirBase = scenario.getConfig().controler().getOutputDirectory();
		this.lastIteration = scenario.getConfig().controler().getLastIteration();
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		// prepare files for the results of all iterations
		prepareOverallItWriting();
	}

	private void prepareOverallItWriting() {
		// create output dir for overall iteration analysis
		String lastItDir = this.outputDirBase + "ITERS/it." + this.lastIteration + "/";
		new File(lastItDir).mkdir();
		String lastItOutputDir = lastItDir + "analysis/";
		new File(lastItOutputDir).mkdir();
		
		// create writing stream
		try {	
			this.totalGreenOverItWritingStream = new PrintStream(
					new File(lastItOutputDir + "totalGreenOverIt.txt"));
			this.avgGreenPerCycOverItWritingStream = new PrintStream(
					new File(lastItOutputDir + "avgGreenPerCycOverIt.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// write header of both streams
		String headerTotalGreen = "it";
		String headerAvgGreen = "it";
		for (Id<SignalSystem> signalSystemId : signalsData.getSignalSystemsData().getSignalSystemData().keySet()){
			for (Id<SignalGroup> signalGroupId : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId).keySet()){
				headerTotalGreen += "\t" + signalGroupId;
				headerAvgGreen += "\t" + signalGroupId;
			}
		}
		this.totalGreenOverItWritingStream.println(headerTotalGreen);
		this.avgGreenPerCycOverItWritingStream.println(headerAvgGreen);
	}

	public void writeIterationResults(int iteration) {	
		log.info("Starting to write analysis of iteration " + iteration + "...");
		
		// collect results of this iteration
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = handler.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimes = handler.calculateAvgSignalGreenTimePerFlexibleCycle();
		
		// add line with results to overallItWritingStreams
		StringBuffer lineTotalTime = new StringBuffer();
		StringBuffer lineAvgTime = new StringBuffer();
		lineTotalTime.append(iteration);
		lineAvgTime.append(iteration);
		for (Id<SignalSystem> signalSystemId : signalsData.getSignalSystemsData().getSignalSystemData().keySet()){
			for (Id<SignalGroup> signalGroupId : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId).keySet()){
				lineTotalTime.append("\t" + totalSignalGreenTimes.get(signalGroupId));
				lineAvgTime.append("\t" + avgSignalGreenTimes.get(signalGroupId));
			}
		}
		this.totalGreenOverItWritingStream.println(lineTotalTime.toString());
		this.avgGreenPerCycOverItWritingStream.println(lineAvgTime.toString());
		
		// create output dir for this iteration analysis
		String outputDir = this.outputDirBase + "ITERS/it." + iteration + "/analysis/";
		new File(outputDir).mkdir();

		// write iteration specific analysis
		log.info("Results of iteration " + iteration + ":");
		writeSumOfBygoneSignalTimes(outputDir, handler.getSumOfBygoneSignalGreenTime());
	}

	private void writeSumOfBygoneSignalTimes(String outputDir, Map<Double, Map<Id<SignalGroup>, Double>> sumOfBygoneSignalGreenTime) {
		PrintStream stream;
		String filename = outputDir + "summedBygoneSignalTimes.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time";
		for (Id<SignalSystem> signalSystemId : signalsData.getSignalSystemsData().getSignalSystemData().keySet()){
			for (Id<SignalGroup> signalGroupId : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId).keySet()){
				header += "\t" + signalGroupId;
			}
		}
		stream.println(header);
		for (Double time : sumOfBygoneSignalGreenTime.keySet()) {
			StringBuffer line = new StringBuffer();
			Map<Id<SignalGroup>, Double> bygoneSignalTimes = sumOfBygoneSignalGreenTime.get(time);
			
			line.append(time);
			for (Id<SignalSystem> signalSystemId : signalsData.getSignalSystemsData().getSignalSystemData().keySet()){
				for (Id<SignalGroup> signalGroupId : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId).keySet()){
					line.append("\t" + bygoneSignalTimes.get(signalGroupId));
				}
			}
			stream.println(line.toString());
		}
		stream.close();
		log.info("output written to " + filename);
	}

	public void closeAllStreams() {
		this.totalGreenOverItWritingStream.close();
		this.avgGreenPerCycOverItWritingStream.close();
	}
	
}
