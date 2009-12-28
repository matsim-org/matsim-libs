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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class MyCommercialActivityDensityListener implements IterationStartsListener, IterationEndsListener{
	
	private BufferedWriter outputCommercialActivityDensity = null;
	private static final String DELIMITER = ",";
	private MyCommercialActivityDensityWriter cs = null;

	public MyCommercialActivityDensityListener() {

	}

	public void notifyIterationStarts(IterationStartsEvent event) {

		event.getControler();
		String outputCommercialActivityDensityFilename = event.getControler().getControlerIO().getIterationPath(event.getControler().getIteration()) + "/" + event.getControler().getIteration() + ".eventsTruckMinor.txt";
		try {
			this.outputCommercialActivityDensity = new BufferedWriter(new FileWriter(new File( outputCommercialActivityDensityFilename )));
			this.outputCommercialActivityDensity.write("Long");
			this.outputCommercialActivityDensity.write(DELIMITER);
			this.outputCommercialActivityDensity.write("Lat");
			this.outputCommercialActivityDensity.write(DELIMITER);
			this.outputCommercialActivityDensity.write("Start_Hour");			
			this.outputCommercialActivityDensity.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Network nw = event.getControler().getNetwork();
		this.cs = new MyCommercialActivityDensityWriter(this.outputCommercialActivityDensity, nw);
		event.getControler().getEvents().addHandler(this.cs);
	}	
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		event.getControler().getEvents().removeHandler(this.cs);
		try {
			this.outputCommercialActivityDensity.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}