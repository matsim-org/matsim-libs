/* *********************************************************************** *
 * project: org.matsim.*
 * MyAllEventCounter.java
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
import java.io.IOException;

import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.handler.ActivityStartEventHandler;

public class MyCommercialSplitter implements ActivityStartEventHandler{
	
	private BufferedWriter output;
	
	public MyCommercialSplitter(BufferedWriter output) {
		this.output = output;
	}

	public void handleEvent(ActivityStartEvent event) {
		
		if(event.getAct().getType().equalsIgnoreCase("minor")){
			double timeSeconds = event.getTime();
			int hour = (int) Math.floor((timeSeconds) / 3600);
			
			String outputString = event.getLink().getCoord().getX() + "," + 
								  event.getLink().getCoord().getY() + "," +
								  hour;
			try {
				this.output.write(outputString);
				this.output.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void reset(int iteration) {

	}

}
