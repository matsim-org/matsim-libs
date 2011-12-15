/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionInternalizationModule.java
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
package playground.benjamin.internalization;

import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.benjamin.emissions.events.ColdEmissionEvent;
import playground.benjamin.emissions.events.ColdEmissionEventHandler;
import playground.benjamin.emissions.events.WarmEmissionEvent;
import playground.benjamin.emissions.events.WarmEmissionEventHandler;


/**
 * @author benjamin
 *
 */
public class EmissionInternalizationHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	private Map<Id, Double> personId2score;

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	public double getScore(Id personId){
		return personId2score.get(personId);
	}

}
