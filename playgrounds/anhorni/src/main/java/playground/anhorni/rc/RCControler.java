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

package playground.anhorni.rc;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.LinkFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;

public class RCControler extends Controler {
					
	public RCControler(final String[] args) {
		super(args);	
	}

	public static void main (final String[] args) { 
		RCControler controler = new RCControler(args);
		controler.setOverwriteFiles(true);
		controler.setScoringFunctionFactory(new RCScoringFunctionFactory(
				controler.getConfig().planCalcScore(), controler.getScenario()));
		
		WithinDayControlerListener withinDayControlerListener = controler.initWithinDayReplanning(controler.getScenario());
		controler.addControlerListener(withinDayControlerListener);
		
    	controler.run();
    }
	
	
	public WithinDayControlerListener initWithinDayReplanning(Scenario scenario) {
		WithinDayControlerListener withinDayControlerListener = new WithinDayControlerListener();
		
		TravelDisutility travelDisutility = withinDayControlerListener.getTravelDisutilityFactory()
				.createTravelDisutility(withinDayControlerListener.getTravelTimeCollector(), scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, withinDayControlerListener.getTravelTimeCollector());
		
		LeaveLinkIdentifierFactory duringLegIdentifierFactory = new LeaveLinkIdentifierFactory(withinDayControlerListener.getLinkReplanningMap(),
				withinDayControlerListener.getMobsimDataProvider());
		
		Set<Id> links = new HashSet<Id>();
		
		LinkFilterFactory linkFilter = new LinkFilterFactory(links, withinDayControlerListener.getMobsimDataProvider());
		duringLegIdentifierFactory.addAgentFilterFactory(linkFilter);
		
		DuringLegIdentifier duringLegIdentifier = duringLegIdentifierFactory.createIdentifier();
		
		CurrentLegReplannerFactory duringLegReplannerFactory = new CurrentLegReplannerFactory(scenario, withinDayControlerListener.getWithinDayEngine(),
				withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		duringLegReplannerFactory.addIdentifier(duringLegIdentifier);
		
		withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
		
		return withinDayControlerListener;
	}
	
}
