/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


/**
 * Leg scoring that considers route distance for pt legs
 */

public class CharyparNagelLegScoringWRouteDistance implements LegScoring, ArbitraryEventScoring{
	private CharyparNagelLegScoring delegLegScoring;
	private IndividualPTvaluesFromEvents individualPTvaluesFromEvents;
	private final Id personId;
	private Leg tempLeg;
	 
	public CharyparNagelLegScoringWRouteDistance(final Id personId, final CharyparNagelScoringParameters params, Network network, IndividualPTvaluesFromEvents indptValues) {
		delegLegScoring = new CharyparNagelLegScoring(params, network);	
		this.personId = personId;
		individualPTvaluesFromEvents =indptValues;
		this.reset();
	}
	
	@Override
	public void finish() {
		delegLegScoring.finish();
	}

	@Override
	public double getScore() {
		return delegLegScoring.getScore();
	}

	@Override
	public void reset() {
		delegLegScoring.reset();
	}

	@Override
	public void handleEvent(Event event) {
		delegLegScoring.handleEvent(event);		
	}

	@Override
	public void startLeg(double time, Leg leg) {
		tempLeg = leg;
		delegLegScoring.startLeg(time, leg);
	}

	@Override
	public void endLeg(double time) {
		IndividualPTvalues personValues = individualPTvaluesFromEvents.getIndividualPTvalues(personId);
		
		if(tempLeg.getMode().equals(TransportMode.pt)){
			tempLeg.getRoute().setDistance(personValues.getTrDistance());
			//System.err.println("myscorer " + i.getTrDistance());
		}
		//this is not needed, CharyparNagel scoring calculates walk time in the same way 
		//if(tempLeg.getMode().equals(TransportMode.transit_walk)){
			//IndividualPTvalues i = individualPTvaluesFromEvents.getIndividualPTvalues(personId);
			//System.err.println("myscorer walkTime" + i.getTrWalkTime());	
			//individualPTvaluesFromEvents.getIndividualPTvalues(personId).setTrWalkTime(0.0); //set to zero to start leg distance calculation from zero
		//}
		
		//it is necessary to nullify values from eventHandler, otherwise they are accumulated. In this class they are needed only for leg-basis calculation 
		personValues.setTrDistance(0.0);
		personValues.setTrTime(0.0);
		personValues.setTrWalkTime(0.0);
		personValues.setChanges(0);
		individualPTvaluesFromEvents.resetAgentEvents(personId);
				
		delegLegScoring.endLeg(time);
	}
	
}