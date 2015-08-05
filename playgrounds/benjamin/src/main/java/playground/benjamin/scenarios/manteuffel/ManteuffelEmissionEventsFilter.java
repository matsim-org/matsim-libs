/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.benjamin.scenarios.manteuffel;

import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.events.algorithms.EventWriterXML;

public class ManteuffelEmissionEventsFilter implements WarmEmissionEventHandler, ColdEmissionEventHandler {
	
	// Morgenspitze (6:00-9:00)
	private double morningStart = 21600.;
	private double morningEnd = 32400.;
	//  Abendspitze (16:00-19:00)
	private double eveningStart = 57600.;
	private double eveningEnd = 68400.;
	private EventWriterXML morningEventWriter;
	private EventWriterXML eveningEventWriter;

	public ManteuffelEmissionEventsFilter(EventWriterXML mew, EventWriterXML eew) {
		this.morningEventWriter = mew;
		this.eveningEventWriter = eew;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		double eventTime = event.getTime();
		if(eventTime >= morningStart){
			if(eventTime < morningEnd){
				this.morningEventWriter.handleEvent(event);
			}
			if(eventTime >= eveningStart){
				if(eventTime < eveningEnd){
					this.eveningEventWriter.handleEvent(event);
				}
			}
			// do nothing
		}
		// do nothing
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		double eventTime = event.getTime();
		if(eventTime >= morningStart){
			if(eventTime < morningEnd){
				this.morningEventWriter.handleEvent(event);
			}
			if(eventTime >= eveningStart){
				if(eventTime < eveningEnd){
					this.eveningEventWriter.handleEvent(event);
				}
			}
			// do nothing
		}
		// do nothing
	}
}
