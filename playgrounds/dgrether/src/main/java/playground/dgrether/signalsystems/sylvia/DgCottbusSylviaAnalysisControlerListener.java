/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusSylviaAnalysisControlerListener
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.model.SignalGroupState;

import playground.dgrether.signalsystems.analysis.DgSignalGreenSplitHandler;
import playground.dgrether.signalsystems.analysis.DgSignalGroupAnalysisData;


public class DgCottbusSylviaAnalysisControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

	private static final Logger log = Logger.getLogger(DgCottbusSylviaAnalysisControlerListener.class);

	private final static boolean collectGreenSplitInformation = true;
	
	private DgTimeCalcHandler timeCalcHandler;

	private DgSignalGreenSplitHandler signalGreenSplitHandler;

	@Override
	public void notifyStartup(StartupEvent e) {
		this.timeCalcHandler = new DgTimeCalcHandler();
		e.getControler().getEvents().addHandler(this.timeCalcHandler);
		if (collectGreenSplitInformation){
			this.initGreenSplitHandler();
			e.getControler().getEvents().addHandler(this.signalGreenSplitHandler);
		}
	}

	private void initGreenSplitHandler(){
		this.signalGreenSplitHandler = new DgSignalGreenSplitHandler();
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("18"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("17"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("1"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("28"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("27"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("12"));
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent e) {

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent e) {
		log.info("Agents that passed an adaptive signal system (1,17 or 18) at least once: "
				+ this.timeCalcHandler.getPassedAgents());
		log.info("Average TravelTime for Agents that passed an adaptive signal at least once: "+this.timeCalcHandler.getAverageAdaptiveTravelTime());
		log.info("Average TT of all Agents" +this.timeCalcHandler.getAverageTravelTime() ); 
		log.info("Latest arrival time at stadium for Agents coming from Cottbus: "+this.timeCalcHandler.getLatestArrivalCBSDF());
		log.info("Latest arrival time at stadium for Agents coming from SPN: "+this.timeCalcHandler.getLatestArrivalSPNSDF());
		log.info("Latest home time for agents going from stadium to Cottbus"+this.timeCalcHandler.getLatestArrivalSDFCB());
		log.info("Latest home time for agents going from stadium to SPN"+this.timeCalcHandler.getLatestArrivalSDFSPN());

		this.timeCalcHandler.exportArrivalTime(e.getIteration(), e.getControler().getConfig().controler().getOutputDirectory());
	}

	@Override
	public void notifyShutdown(ShutdownEvent e) {
		if (collectGreenSplitInformation){
			this.writeSignalStats(e.getControler().getConfig().controler().getOutputDirectory());
		}
	}


	private void writeSignalStats(String outputDirectory){
		try {
			FileWriter fw = new FileWriter(outputDirectory +"signal_statistic.csv");
			for (Id ssid : signalGreenSplitHandler.getSystemIdAnalysisDataMap().keySet()) {
				for (Entry<Id, DgSignalGroupAnalysisData> entry : signalGreenSplitHandler
						.getSystemIdAnalysisDataMap().get(ssid)
						.getSystemGroupAnalysisDataMap().entrySet()) {
					// logg.info("for signalgroup: "+entry.getKey());
					for (Entry<SignalGroupState, Double> ee : entry
							.getValue().getStateTimeMap().entrySet()) {
						// logg.info(ee.getKey()+": "+ee.getValue());
						fw.append(ssid + ";" + entry.getKey() + ";"
								+ ee.getKey() + ";" + ee.getValue()+";\n");

					}
				}
			}				
			fw.flush();
			fw.close();
			log.info("Wrote signalsystemstats.");
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
