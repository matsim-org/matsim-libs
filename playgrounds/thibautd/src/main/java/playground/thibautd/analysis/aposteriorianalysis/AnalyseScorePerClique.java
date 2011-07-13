/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseScorePerClique.java
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
package playground.thibautd.analysis.aposteriorianalysis;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * runs an analysis of the score per clique size
 * @author thibautd
 */
public class AnalyseScorePerClique {
	private static final Log log =
		LogFactory.getLog(AnalyseScorePerClique.class);


	private static final String DIRECTORY_MODULE_REGEXP = "outputForAnalysis.*";
	private static final String LABEL_FIELD = "label";
	private static final String DIRECTORY_FIELD = "directory";
	private static final String ANALYSIS_MODULE = "analysis";
	private static final String ITERATION_FIELD = "iteration";
	private static final String OUTPUT_FIELD = "outputDirectory";
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;


	/**
	 * AnalyseScorePerClique config
	 * <br><br>
	 * the config must define:
	 * <ul>
	 * <li> an arbitrary number of modules named "outputForAnalysis*", * being
	 * any string, with a parameter "label" and a parameter "directory"
	 * <li> a module named "analysis" defining a field "iteration" and a field
	 * "outputDirectory"
	 * </ul>
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);

		Map<String,String> outputDirectories = getOutputDirectories(config);
		int iteration = Integer.parseInt(
				config.getModule(ANALYSIS_MODULE).getValue(ITERATION_FIELD));
		File outputPath = new File(getOutputPath(config));

		ExecutedScorePerCliqueSizeAnalyser analyser =
			new ExecutedScorePerCliqueSizeAnalyser(
					outputDirectories,
					iteration);

		ChartUtil chart = analyser.getChart();

		// do not check for existence of file
		outputPath.mkdirs();
		try {
			chart.saveAsPng(
					outputPath.getCanonicalPath() + "/executedScorePerCliqueSize.png",
					WIDTH, HEIGHT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String,String> getOutputDirectories(final Config config) {
		Map<String, String> output = new HashMap<String, String>();
		Map<String, Module> configGroups = config.getModules();
		Module module;
		String label;
		String dir;

		for (Map.Entry<String, Module> entry : configGroups.entrySet()) {
			if (entry.getKey().matches(DIRECTORY_MODULE_REGEXP)) {
				module = entry.getValue();
				label = module.getValue(LABEL_FIELD);
				dir = module.getValue(DIRECTORY_FIELD);

				log.debug("importing info from module "+entry.getKey()+
						": label="+label+
						", directory="+dir);

				output.put(label, dir);
			}
		}

		return output;
	}


	private static String getOutputPath(final Config config) {
		String path = config.getModule(ANALYSIS_MODULE).getValue(OUTPUT_FIELD);
		return (path.matches(".*/") ? path : path + "/");
	}
}

