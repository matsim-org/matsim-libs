/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertFigure9Solution2Matsim
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import java.io.IOException;
import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;

import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010Solution2Matsim;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010SolutionXMLParser10;


/**
 * Script that was once used to convert the figure 9 test scenario results
 * Will not work anymore, as id conversion must be reimplemented. 
 * 
 * @author dgrether
 *
 */
@Deprecated
public class ConvertFigure9Solution2Matsim {
	private static final String MATSIM_CONFIG = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals.xml";
	private static final String SOLUTION_INPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/solution_figure9_from_matsim_population_800_50_50.sol";
	private static final String SIGNAL_CONTROL_OUTPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/signal_control_solution_figure9_from_matsim_population_800_50_50.xml";

	public void convert(String matsimConfig, String solutionInputFilename, String signalControlOutputFilename) throws IOException{
		//load solution
		KS2010SolutionXMLParser10 solutionParser = new KS2010SolutionXMLParser10();
		solutionParser.readFile(solutionInputFilename);
		List<KS2010CrossingSolution> solutionCrossings = solutionParser.getSolutionCrossingByIdMap();
		
		//load config and signals
		Config config = ConfigUtils.loadConfig(matsimConfig);
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class));
		SignalsData signals = signalsLoader.loadSignalsData();
		
		//convert solution to signal plans
		SignalControlData signalControl = signals.getSignalControlData();
		DgIdPool idPool = null;
		new KS2010Solution2Matsim(idPool).convertSolution(signalControl, solutionCrossings);
		
		SignalControlWriter20 signalControlWriter = new SignalControlWriter20(signalControl);
		signalControlWriter.write(signalControlOutputFilename);
	}

	
	public static void main(String[] args) throws IOException {
		
		
		new ConvertFigure9Solution2Matsim().convert(MATSIM_CONFIG, SOLUTION_INPUT_FILENAME, SIGNAL_CONTROL_OUTPUT_FILENAME);
	}

}
