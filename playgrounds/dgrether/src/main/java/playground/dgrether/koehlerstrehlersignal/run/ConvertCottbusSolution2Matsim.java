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

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.SignalsScenarioWriter;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010Solution2Matsim;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010SolutionTXTParser10;


/**
 * @author dgrether
 *
 */
public class ConvertCottbusSolution2Matsim {

	private void convert(String directory, String inputFile){
		KS2010SolutionTXTParser10  solutionParser = new KS2010SolutionTXTParser10();
		solutionParser.readFile(directory + inputFile);
		List<KS2010CrossingSolution> solutionCrossings = solutionParser.getSolutionCrossings();

		DgIdPool idPool = DgIdPool.readFromFile(directory + "id_conversions.txt");
		SignalsData signalsData = loadSignalsData(directory);
//
		KS2010Solution2Matsim converter = new KS2010Solution2Matsim(idPool);
		converter.setScale(3); // TODO check this parameter when tool is rerun
		converter.convertSolution(signalsData.getSignalControlData(), solutionCrossings);

		// Currently we get two offsets for signal system 13. This is due to different data modelling approaches
		// at tub / btu that might be resolved in further studies
		removeSignalSystems(signalsData, new IdImpl("13"));
		
		writeSignalControl(directory, inputFile, signalsData);
	}
	
	private void removeSignalSystems(SignalsData signalData, Id...ids) {
		for (Id id : ids) {
			signalData.getSignalSystemsData().getSignalSystemData().remove(id);
			signalData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().remove(id);
			signalData.getSignalControlData().getSignalSystemControllerDataBySystemId().remove(id);
		}
	}
	

	private SignalsData loadSignalsData(String directory) {
		Config config = ConfigUtils.createConfig(); 
		config.signalSystems().setSignalSystemFile(directory + "output_signal_systems_v2.0.xml.gz");
		config.signalSystems().setSignalGroupsFile(directory + "output_signal_groups_v2.0.xml.gz");
		config.signalSystems().setSignalControlFile(directory + "output_signal_control_v2.0.xml.gz");
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(config.signalSystems());
		SignalsData signals = signalsLoader.loadSignalsData();
		return signals;
	}



	private void writeSignalControl(String directoryPath, String inputFilename, SignalsData signalsData) {
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		String basefilename = inputFilename.substring(0, inputFilename.lastIndexOf("."));
		writer.setSignalSystemsOutputFilename(directoryPath + "optimized_signal_systems_" + basefilename + ".xml");
		writer.setSignalGroupsOutputFilename(directoryPath + "optimized_signal_groups_" + basefilename + ".xml");
		writer.setSignalControlOutputFilename(directoryPath + "optimized_signal_control_" + basefilename + ".xml");
		writer.writeSignalsData(signalsData);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Tuple<String, String>> input= new ArrayList<Tuple<String, String>>();
		input.add(new Tuple<String, String>(
						DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_50_evening_peak/",
						"ksm_50a_sol.txt"
						));
		input.add(new Tuple<String, String>(
				DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_50_morning_peak/",
				"ksm_50m_sol.txt"
				));
		input.add(new Tuple<String, String>(
				DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/",
				"ksm_10a_sol.txt"
				));
		input.add(new Tuple<String, String>(
				DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_morning_peak/",
				"ksm_10m_sol.txt"
		));

		
		for (Tuple<String, String> i : input){
			new ConvertCottbusSolution2Matsim().convert(i.getFirst(), i.getSecond());
		}
		
		
	}

}
