/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialLegHistogramListener.java
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

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class MyCommercialLegHistogramListener implements IterationStartsListener, IterationEndsListener{
	private MyCommercialLegHistogramBuilder hb = null;
	
	public MyCommercialLegHistogramListener(){
		
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Open two files, one for private cars, and one for commercial
		
		// TODO Write a header to each 
		
		// Create an instance of the eventsHandler to convert events to a histogram table
		MyCommercialLegHistogramBuilder clhb = new MyCommercialLegHistogramBuilder();
		// Add the eventsHandler to the controller
		event.getControler().getEvents().addHandler(clhb);
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		// Remove the eventsHandler
		event.getControler().getEvents().removeHandler(this.hb);
		
		// TODO Close all files
		
	}

}
