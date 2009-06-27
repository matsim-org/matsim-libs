/* *********************************************************************** *
 * project: org.matsim.*
 * MySimulationStartListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialModel.Listeners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.api.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

public class MyCommercialListener implements StartupListener, IterationStartsListener, IterationEndsListener{
	
	private BufferedWriter OUTPUT = null;
	private final String DELIMITER = ",";
	private MyCommercialSplitter cs = null;

	public MyCommercialListener() {

	}

	public void notifyStartup(StartupEvent event) {
	}

	public void notifyIterationStarts(IterationStartsEvent event) {

		event.getControler();
		String outputTruck = Controler.getIterationPath() + "/" + Controler.getIteration() + ".eventsTruckMinor.txt";
		String header = "Long" + DELIMITER +
						"Lat" + DELIMITER +
						"Start_Hour";
		try {
			this.OUTPUT = new BufferedWriter(new FileWriter(new File( outputTruck )));
			this.OUTPUT.write(header);
			this.OUTPUT.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Network nw = event.getControler().getNetwork();
		this.cs = new MyCommercialSplitter(this.OUTPUT, nw);
		event.getControler().getEvents().addHandler(this.cs);
	}	
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		event.getControler().getEvents().removeHandler(this.cs);
		try {
			this.OUTPUT.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}