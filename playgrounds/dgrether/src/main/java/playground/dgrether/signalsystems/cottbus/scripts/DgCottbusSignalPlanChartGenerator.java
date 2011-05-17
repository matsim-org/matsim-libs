/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusSignalPlanChartGenerator
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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.SignalGroupStateChangedEvent;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.utils.DgSignalEventsCollector;
import playground.dgrether.signalsystems.utils.DgSignalPlanChart;


/**
 * @author dgrether
 *
 */
public class DgCottbusSignalPlanChartGenerator {

	private static final Logger log = Logger.getLogger(DgCottbusSignalPlanChartGenerator.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//parameters
		//cottbus
		IdImpl runId = new IdImpl("1224");
		String baseDir = DgPaths.REPOS + "runs-svn/";
		int iteration = 500;
		Id signalSystemId = new IdImpl(18);
		double startSecond = 23135.0;
		double endSecond = startSecond + 3600.0;
		startSecond = 8.0 * 3600.0;
		endSecond = startSecond + 3600.0;

		baseDir = baseDir + "run" + runId;
//		koehler strehler 2010
//		runId = null;
//		baseDir = "/media/data/work/matsimOutput/koehlerStrehler2010Scenario5SelectBest/";
//		iteration = 20;
//		signalSystemId = new IdImpl(5);
//		startSecond = 0.0;
//		endSecond = 900.0;
//		
		//skript
		ControlerIO io = null;
		if (runId != null) 
			io = new ControlerIO(baseDir, runId);
		else
			io = new ControlerIO(baseDir);
		
		String eventsFilename = io.getIterationFilename(iteration, "events.xml.gz");
		
		DgSignalEventsCollector eventsCollector = new DgSignalEventsCollector();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(eventsCollector);
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
		eventsReader.readFile(eventsFilename);
		
		SortedSet<SignalGroupStateChangedEvent> systemEvents = eventsCollector.getSignalGroupEventsBySystemIdMap().get(signalSystemId);
		log.info("Number of events for system " + signalSystemId + " is: " + systemEvents.size());
		for (SignalGroupStateChangedEvent e : systemEvents) {
			if (e.getTime() >= startSecond && e.getTime() <= endSecond)
				log.debug(e);
		}
//			
		DgSignalPlanChart chart = new DgSignalPlanChart(startSecond, endSecond);
		chart.addData(systemEvents);
		JFreeChart jfChart = chart.createSignalPlanChart("System plan", "group", "time");
		
		String chartFileName = io.getIterationFilename(iteration, "signal_plan_system_" + signalSystemId + "_from_" + startSecond + "_to_" + endSecond);
		
		int rowCount = jfChart.getCategoryPlot().getDataset().getRowCount();
		
		int width = (int) (endSecond - startSecond) * 3;
		writeToPng(chartFileName, jfChart, width, rowCount * 3 + 50);
		
	}
	
	public static void writeToPng(String filename, JFreeChart jchart, int width, int ySize) {
		filename += ".png";
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), jchart, width, ySize, null, true, 9);
			log.info("Chart written to : " +filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
