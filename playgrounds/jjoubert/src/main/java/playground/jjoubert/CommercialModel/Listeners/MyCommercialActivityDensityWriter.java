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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

public class MyCommercialActivityDensityWriter implements ActivityStartEventHandler{
	
	private BufferedWriter outputGapDensity;
	private Network network;
	
	public MyCommercialActivityDensityWriter(BufferedWriter output, Network nw) {
		this.outputGapDensity = output;
		this.network = nw;
	}

	public void reset(int iteration) {

	}

	public void handleEvent(ActivityStartEvent event) {

		if(event.getActType().equalsIgnoreCase("minor")){
			double timeSeconds = event.getTime();
			int hour = (int) Math.floor((timeSeconds) / 3600);

			Link link = this.network.getLinks().get( event.getLinkId() );
			try {
				this.outputGapDensity.write(String.valueOf(link.getCoord().getX()));
				this.outputGapDensity.write(",");
				this.outputGapDensity.write(String.valueOf(link.getCoord().getY()));
				this.outputGapDensity.write(",");
				this.outputGapDensity.write(Integer.toString(hour));
				this.outputGapDensity.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}