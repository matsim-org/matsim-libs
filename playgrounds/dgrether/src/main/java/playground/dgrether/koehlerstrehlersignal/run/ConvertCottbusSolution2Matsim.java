/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertCottbusSolution2Matsim
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.model.SignalSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010Solution2Matsim;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010SolutionTXTParser10;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2014RandomOffsetsXMLParser;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2014SolutionXMLParser;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public class ConvertCottbusSolution2Matsim {

	/*
	 * flag to determine which solution format is used. if false the solution is
	 * given as txt file (former version), if true as xml file (standard since
	 * 2014).
	 */
	private static boolean xmlFormat = true;

	private void convertOptimalSolution(String directory, String inputFile) {
		List<KS2010CrossingSolution> crossingSolutions;
		if (xmlFormat) { // standard format since 2014
			KS2014SolutionXMLParser solutionParser = new KS2014SolutionXMLParser();
			solutionParser.readFile(directory + inputFile);
			crossingSolutions = solutionParser.getCrossingSolutions();
		} else { // txt format - old format
			KS2010SolutionTXTParser10 solutionParser = new KS2010SolutionTXTParser10();
			solutionParser.readFile(directory + inputFile);
			crossingSolutions = solutionParser.getSolutionCrossings();
		}
		SignalsData signalsData = loadSignalsData(directory);
		DgIdPool idPool = DgIdPool.readFromFile(directory
				+ "id_conversions.txt");

		convertSignals(crossingSolutions, idPool, signalsData);
		writeOptimizedSignalControl(directory, inputFile, signalsData);
	}
	
	private void convertRandomSolution(String directory, String inputFile) {
		KS2014RandomOffsetsXMLParser solutionParser = new KS2014RandomOffsetsXMLParser();
		solutionParser.readFile(directory + inputFile);
		Map<Integer, List<KS2010CrossingSolution>> crossingSolutions = 
				solutionParser.getRandomOffsets();
		
		SignalsData signalsData = loadSignalsData(directory);
		DgIdPool idPool = DgIdPool.readFromFile(directory
				+ "id_conversions.txt");

		// convert and write offsets for min random (=0), max random (=1), avg random (=2), med random (=3)
		for (int i=0; i<4; i++){
			convertSignals(crossingSolutions.get(i), idPool, signalsData);
			writeRandomOffsetsSignalControl(directory, inputFile, signalsData, i);
		}
	}

	private void convertSignals(List<KS2010CrossingSolution> crossingSolutions,
			DgIdPool idPool, SignalsData signalsData) {
		KS2010Solution2Matsim converter = new KS2010Solution2Matsim(idPool);
		// converter.setScale(3); // TODO check this parameter when tool is
		// rerun
		converter.convertSolution(signalsData.getSignalControlData(),
				crossingSolutions);

		// Currently we get two offsets for signal system 13. This is due to
		// different data modelling approaches
		// at tub / btu that might be resolved in further studies
		removeSignalSystems(signalsData, Id.create("13", SignalSystem.class));
	}

	private void removeSignalSystems(SignalsData signalData, Id<SignalSystem>... ids) {
		for (Id<SignalSystem> id : ids) {
			signalData.getSignalSystemsData().getSignalSystemData().remove(id);
			signalData.getSignalGroupsData()
					.getSignalGroupDataBySignalSystemId().remove(id);
			signalData.getSignalControlData()
					.getSignalSystemControllerDataBySystemId().remove(id);
		}
	}

	private SignalsData loadSignalsData(String directory) {
		Config config = ConfigUtils.createConfig();
		config.signalSystems().setSignalSystemFile(
				directory + "output_signal_systems_v2.0.xml.gz");
		config.signalSystems().setSignalGroupsFile(
				directory + "output_signal_groups_v2.0.xml.gz");
		config.signalSystems().setSignalControlFile(
				directory + "output_signal_control_v2.0.xml.gz");
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(
				config.signalSystems());
		SignalsData signals = signalsLoader.loadSignalsData();
		return signals;
	}

	private void writeOptimizedSignalControl(String directoryPath, String inputFilename,
			SignalsData signalsData) {
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		String basefilename = inputFilename.substring(inputFilename.lastIndexOf("/")+1,
				inputFilename.lastIndexOf("."));
		String subdirectory = inputFilename.substring(0,inputFilename.lastIndexOf("/")+1);
		
		writer.setSignalSystemsOutputFilename(directoryPath + subdirectory
				+ "signal_systems_" + basefilename + ".xml");
		writer.setSignalGroupsOutputFilename(directoryPath + subdirectory
				+ "signal_groups_" + basefilename + ".xml");
		writer.setSignalControlOutputFilename(directoryPath + subdirectory
				+ "signal_control_" + basefilename + ".xml");
		writer.writeSignalsData(signalsData);
	}
	
	/**
	 * 
	 * @param currentCoord 0 means min random (i.e. best random), 
	 * 	1 means max random (i.e. worst random),
	 * 	2 means avg random, 
	 * 	3 means med random (i.e. median).
	 */
	private void writeRandomOffsetsSignalControl(String directoryPath, String inputFilename,
			SignalsData signalsData, Integer currentCoord) {
		
		String substring = "";
		switch (currentCoord){
		case 0: substring = "best"; break;
		case 1: substring = "worst"; break;
		case 2: substring = "avg"; break;
		case 3: substring = "med"; break;
		}
		
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		String basefilename = inputFilename.substring(inputFilename.lastIndexOf("/")+1,
				inputFilename.lastIndexOf("."));
		String subdirectory = inputFilename.substring(0,inputFilename.lastIndexOf("/")+1);
		
		writer.setSignalSystemsOutputFilename(directoryPath + subdirectory
				+ "signal_systems_" + substring + "_" + basefilename + ".xml");
		writer.setSignalGroupsOutputFilename(directoryPath + subdirectory
				+ "signal_groups_" + substring + "_" + basefilename + ".xml");
		writer.setSignalControlOutputFilename(directoryPath + subdirectory
				+ "signal_control_" + substring + "_" + basefilename + ".xml");
		writer.writeSignalsData(signalsData);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Tuple<String, String>> input = new ArrayList<Tuple<String, String>>();
		// input.add(new Tuple<String, String>(
		// DgPaths.REPOS +
		// "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_50_evening_peak/",
		// "ksm_50a_sol.txt"
		// ));
		// input.add(new Tuple<String, String>(
		// DgPaths.REPOS +
		// "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_50_morning_peak/",
		// "ksm_50m_sol.txt"
		// ));
		// input.add(new Tuple<String, String>(
		// DgPaths.REPOS +
		// "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/",
		// "ksm_10a_sol.txt"
		// ));
		// input.add(new Tuple<String, String>(
		// DgPaths.REPOS +
		// "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_morning_peak/",
		// "ksm_10m_sol.txt"
		// ));

//		 input.add(new Tuple<String, String>(
//		 DgPaths.REPOS +
//		 "shared-svn/projects/cottbus/cb2ks2010/2013-08-12_minflow_10_evening_peak/",
//		 "ksm_10a_sol.txt"
//		 ));
//		 input.add(new Tuple<String, String>(
//		 DgPaths.REPOS +
//		 "shared-svn/projects/cottbus/cb2ks2010/2013-08-12_minflow_10_morning_peak/",
//		 "ksm_10m_sol.txt"
//		 ));
//
//		for (Tuple<String, String> i : input) {
//			new ConvertCottbusSolution2Matsim().convertOptimalSolution(
//					i.getFirst(), i.getSecond());
//		}
		
		new ConvertCottbusSolution2Matsim().convertOptimalSolution( DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/2015-02-06_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/", 
				"btu/new2_optimum.xml");
		new ConvertCottbusSolution2Matsim().convertRandomSolution(DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/2015-02-06_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/", 
				"btu/random_coordinations.xml");
		
	}
	
}
