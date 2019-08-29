/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
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

package org.matsim.contrib.emissions;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * minimalistic handler to compare calculate sums in some tests
 * @author julia
 *
 */
class HandlerToTestEmissionAnalysisModules implements EventsManager {
	private static Double sumOverAll=0.0;

	@Override
	public void processEvent(Event event) {	
		for(String attribute: event.getAttributes().keySet()){
			try {
				if (!(attribute.equals("time"))) {
					sumOverAll += Double.parseDouble(event.getAttributes().get(attribute));
				}
				
			} catch (NumberFormatException e) {
				String notANumber = event.getAttributes().get(attribute);
				if(notANumber.equals("coldEmissionEvent")||notANumber.equals("coldEmissionEventLinkId")||notANumber.equals("personId")){
					//everything ok
				}else{
					//Assert.fail("this is not an expected cold emission event attribute: "+notANumber);
				}
				
			}
		}

	}

	@Override
	public void addHandler(EventHandler handler) {
	}

	@Override
	public void removeHandler(EventHandler handler) {
	}

	@Override
	public void setIteration(int iteration) {
	}

	@Override
	public void resetHandlers(int iteration) {
	}

	@Override
	public void initProcessing() {

	}

	@Override
	public void afterSimStep(double time) {

	}

	@Override
	public void finishProcessing() {

	}

	public static Double getSum() {
		return sumOverAll;

	}

	public static void reset() {
		sumOverAll=.0;
		
	}

}
