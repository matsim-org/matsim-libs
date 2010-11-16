/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Solution2MatsimConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgKoehlerStrehler2010Solution2MatsimConverter {
	
	private static final Logger log = Logger
			.getLogger(DgKoehlerStrehler2010Solution2MatsimConverter.class);
	
	private static final String MATSIM_CONFIG = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals.xml";
//	private static final String SOLUTION_INPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/solution_population_100_agents.xml";
//	private static final String SIGNAL_CONTROL_OUTPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/signal_control_solution_population_100_agents.xml";
//	private static final String SOLUTION_INPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/solution_figure9_from_matsim_population_800.sol";
//	private static final String SIGNAL_CONTROL_OUTPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/signal_control_solution_figure9_from_matsim_population_800.xml";
	private static final String SOLUTION_INPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/solution_figure9_from_matsim_population_800_50_50.sol";
	private static final String SIGNAL_CONTROL_OUTPUT_FILENAME = DgPaths.STUDIESDG + "koehlerStrehler2010/signal_control_solution_figure9_from_matsim_population_800_50_50.xml";
	
	
	
	public DgKoehlerStrehler2010Solution2MatsimConverter(){}
	
	public void convert(String matsimConfig, String solutionInputFilename, String signalControlOutputFilename) throws IOException{
		//load solution
		DgSolutionParser solutionParser = new DgSolutionParser();
		solutionParser.readFile(solutionInputFilename);
		Map<Id, DgSolutionCrossing> solutionCrossingByIdMap = solutionParser.getSolutionCrossingByIdMap();
		log.info("Read " + solutionCrossingByIdMap.size() + " solutions");
		//load config and signals
		Config config = ConfigUtils.loadConfig(matsimConfig);
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(config.signalSystems());
		SignalsData signals = signalsLoader.loadSignalsData();
		
		//convert solution to signal plans
		SignalControlData signalControl = signals.getSignalControlData();
		//TODO modify matching in case of more complex scenarios
		log.warn("Matching of signal system to node to solution might not be correct!");
		for (SignalSystemControllerData controllerData : signalControl.getSignalSystemControllerDataBySystemId().values()){
			log.debug("Processing control for signal system id : " + controllerData.getSignalSystemId());
			DgSolutionCrossing solutionCrossing = solutionCrossingByIdMap.get(controllerData.getSignalSystemId());
			log.debug("  solution crossing : " + solutionCrossing);
			for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()){
				Integer offset = solutionCrossing.getProgramIdOffsetMap().get(signalPlan.getId());
				log.debug("  processing plan: " + signalPlan.getId() + " offset: " + offset);
				signalPlan.setOffset(offset);
			}
		}
		
		SignalControlWriter20 signalControlWriter = new SignalControlWriter20(signalControl);
		signalControlWriter.write(signalControlOutputFilename);
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		new DgKoehlerStrehler2010Solution2MatsimConverter().convert(MATSIM_CONFIG, SOLUTION_INPUT_FILENAME, SIGNAL_CONTROL_OUTPUT_FILENAME);
	}

}
