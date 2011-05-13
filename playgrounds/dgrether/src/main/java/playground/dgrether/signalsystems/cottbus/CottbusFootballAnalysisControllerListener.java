/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFootballControllerListener
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
package playground.dgrether.signalsystems.cottbus;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFootballStrings;


/**
 * @author dgrether
 *
 */
public class CottbusFootballAnalysisControllerListener implements StartupListener, IterationStartsListener, IterationEndsListener {

	
	private CottbusFootballTraveltimeHandler traveltimeHandler;

	private Double averageTravelTime = null;

	@Override
	public void notifyStartup(StartupEvent e) {
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent e) {
		Controler controler = e.getControler();
		if ((controler.getWriteEventsInterval() > 0) && (e.getIteration() % controler.getWriteEventsInterval() == 0)){
			this.traveltimeHandler = new CottbusFootballTraveltimeHandler();
			e.getControler().getEvents().addHandler(this.traveltimeHandler);
		}
	}

	
	@Override
	public void notifyIterationEnds(IterationEndsEvent e) {
		Controler controler = e.getControler();
 		if ((controler.getWriteEventsInterval() > 0) && (e.getIteration() % controler.getWriteEventsInterval() == 0)){
 			CottbusFootballTraveltimeWriter traveltimeWriter = new CottbusFootballTraveltimeWriter();
 			
 			String filename = e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "arrival_times_" + CottbusFootballStrings.CB2FB +  ".csv");
 			traveltimeWriter.writeMapToCsv(traveltimeHandler.getArrivalTimesCB2FB(), filename);
 			
 			filename = e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "arrival_times_" + CottbusFootballStrings.FB2CB +  ".csv");
 			traveltimeWriter.writeMapToCsv(traveltimeHandler.getArrivalTimesFB2CB(), filename);
 			
 			filename = e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "arrival_times_" + CottbusFootballStrings.SPN2FB +  ".csv");
 			traveltimeWriter.writeMapToCsv(traveltimeHandler.getArrivalTimesSPN2FB(), filename);
 			
 			filename = e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "arrival_times_" + CottbusFootballStrings.FB2SPN + ".csv");
 			traveltimeWriter.writeMapToCsv(traveltimeHandler.getArrivalTimesFB2SPN(), filename);
 			
 			filename = e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "latest_arrival_times.csv");
 			traveltimeWriter.exportLatestArrivals(traveltimeHandler, filename);
 			
 			filename = e.getControler().getControlerIO().getOutputFilename("average_travel_time.csv");
 			try {
 				BufferedWriter writer = IOUtils.getAppendingBufferedWriter(filename);
 				writer.append(e.getIteration() + CottbusFootballStrings.SEPARATOR + this.traveltimeHandler.getAverageTravelTime());
 				writer.newLine();
 				writer.close();
 			} catch (FileNotFoundException e1) {
 				e1.printStackTrace();
 			} catch (IOException e1) {
 				e1.printStackTrace();
 			}
 			this.averageTravelTime = this.traveltimeHandler.getAverageTravelTime();
 			controler.getEvents().removeHandler(this.traveltimeHandler);
 			this.traveltimeHandler = null;
 		}
	}

	public Double getAverageTraveltime() {
		return this.averageTravelTime;
	}

	
}
