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

import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;

public class MyCommercialSplitter implements BasicActivityStartEventHandler{
	
	private BufferedWriter output;
	private Network network;
	
	public MyCommercialSplitter(BufferedWriter output, Network nw) {
		this.output = output;
		this.network = nw;
	}

	public void reset(int iteration) {

	}

	public void handleEvent(BasicActivityStartEvent event) {

		if(event.getActType().equalsIgnoreCase("minor")){
			double timeSeconds = event.getTime();
			int hour = (int) Math.floor((timeSeconds) / 3600);

			Link link = this.network.getLink( event.getLinkId() );
			String outputString = link.getCoord().getX() + "," + 
			link.getCoord().getY() + "," +
			hour;
			try {
				this.output.write(outputString);
				this.output.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}