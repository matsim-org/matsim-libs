/* *********************************************************************** *
 * project: org.matsim.*
 * SylviaMainBatch
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.sylvia;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class SylviaMainBatch {

	
	private static final Logger log = Logger.getLogger(SylviaMainBatch.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String baseDirectory = DgPaths.REPOS;
		String configFilename = "";
		configFilename = baseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/cottbus_sylvia_config.xml";
		if (args != null && args.length == 2){
			baseDirectory = args[0];
			configFilename = args[1];
			log.info("Running CottbusMainBatch with base directory: " + baseDirectory + " and config: " + configFilename);
		}

		
		String fixedTimeSignals = baseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/signalControlCottbusT90_v2.0_jb_ba_removed.xml";
		String sylviaSignals = baseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/signal_control_sylvia.xml";
		
		String footballPlansBase = baseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/planswithfb/output_plans_";
		
		Config baseConfig = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getConfig();
		MatsimConfigReader confReader = new MatsimConfigReader(baseConfig);
		confReader.readFile(configFilename);

		String outputDirBase = baseConfig.controler().getOutputDirectory();
		
		Map<Integer, Double> fixedtimeScale2AverageTTMap = new HashMap<Integer, Double>();
		Map<Integer, Double> sylviaScale2AverageTTMap = new HashMap<Integer, Double>();
		
		for (int scale = 0; scale <= 100; scale = scale + 5){
			//fixed time control
			DgCottbusSylviaAnalysisControlerListener analysis = new DgCottbusSylviaAnalysisControlerListener();
			baseConfig.controler().setOutputDirectory(outputDirBase + "fixed-time-control_scale_"+scale + "/");
			baseConfig.plans().setInputFile( footballPlansBase + scale + ".xml.gz");
			baseConfig.signalSystems().setSignalControlFile(fixedTimeSignals);
			baseConfig.controler().setRunId("ft_" + scale);
			Controler controler = new Controler(baseConfig);
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
			fixedtimeScale2AverageTTMap.put(scale, analysis.getAverageTravelTime());
			
			//sylvia control
			analysis = new DgCottbusSylviaAnalysisControlerListener();
			baseConfig.controler().setOutputDirectory(outputDirBase + "sylvia-control_scale_"+ scale + "/");
			baseConfig.plans().setInputFile( footballPlansBase + scale + ".xml.gz");
			baseConfig.signalSystems().setSignalControlFile(sylviaSignals);
			baseConfig.controler().setRunId("sv_" + scale);
			
			controler = new Controler(baseConfig);
			controler.setSignalsControllerListenerFactory(new DgSylviaControlerListenerFactory());
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
			sylviaScale2AverageTTMap.put(scale, analysis.getAverageTravelTime());
		}
		
		writeAverageTT(fixedtimeScale2AverageTTMap, outputDirBase + "fixed_time_avg_tt.txt");
		writeAverageTT(sylviaScale2AverageTTMap, outputDirBase + "sylvia_avg_tt.txt");
	}

	private static void writeAverageTT(Map<Integer, Double> map, String filename) throws FileNotFoundException, IOException{
		SortedMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
		sorted.putAll(map);
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("Football fans %" + "\t" + "Average travel time");
		writer.newLine();
		for (Entry<Integer, Double> e : sorted.entrySet()){
			writer.write(e.getKey().toString() + "\t" + e.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}
	

}



