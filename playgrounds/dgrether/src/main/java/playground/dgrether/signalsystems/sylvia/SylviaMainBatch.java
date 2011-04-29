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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 * @deprecated use CottbusFootballBatch instead
 */
@Deprecated
public class SylviaMainBatch {

	
	private static final Logger log = Logger.getLogger(SylviaMainBatch.class);
	
	/**
	 * @param args: first: directory containing shared-svn root; second config serving as base config for football runs
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String reposBaseDirectory = args[0];
		String configFilename = args[1];
		if (args == null || args.length == 0){
			reposBaseDirectory = DgPaths.REPOS;
			configFilename = reposBaseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/cottbus_sylvia_config.xml"; 
			log.info("Running CottbusMainBatch with base directory: " + reposBaseDirectory + " and config: " + configFilename);
		}
		//configure the signals
		String fixedTimeSignals = reposBaseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/signalControlCottbusT90_v2.0_jb_ba_removed.xml";
		String sylviaSignals = reposBaseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/signal_control_sylvia.xml";
		String footballPlansBaseFilename = reposBaseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/planswithfb/output_plans_";
		//read the config
		Config baseConfig = ConfigUtils.createConfig();
		MatsimConfigReader confReader = new MatsimConfigReader(baseConfig);
		confReader.readFile(configFilename);

		String outputDirBase = baseConfig.controler().getOutputDirectory();
		
		//start the runs
		Map<Integer, Double> fixedtimeScale2AverageTTMap = new HashMap<Integer, Double>();
		Map<Integer, Double> sylviaScale2AverageTTMap = new HashMap<Integer, Double>();
		
		for (int scale = 0; scale <= 100; scale = scale + 5){
			//fixed time control
			DgCottbusSylviaAnalysisControlerListener analysis = new DgCottbusSylviaAnalysisControlerListener();
			baseConfig.controler().setOutputDirectory(outputDirBase + "fixed-time_scale_"+scale + "/");
			baseConfig.plans().setInputFile( footballPlansBaseFilename + scale + ".xml.gz");
			baseConfig.signalSystems().setSignalControlFile(fixedTimeSignals);
			baseConfig.controler().setRunId("fixed-time_scale_" + scale);
			Controler controler = new Controler(baseConfig);
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
			
			//sylvia control
			analysis = new DgCottbusSylviaAnalysisControlerListener();
			baseConfig.controler().setOutputDirectory(outputDirBase + "sylvia_scale_"+ scale + "/");
			baseConfig.plans().setInputFile( footballPlansBaseFilename + scale + ".xml.gz");
			baseConfig.signalSystems().setSignalControlFile(sylviaSignals);
			baseConfig.controler().setRunId("sylvia_scale" + scale);
			
			controler = new Controler(baseConfig);
			controler.setSignalsControllerListenerFactory(new DgSylviaControlerListenerFactory());
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		
		writeAverageTT(fixedtimeScale2AverageTTMap, outputDirBase + "fixed-time_avg_tt.txt");
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



