/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.copying;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class Main {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		Scenario sc = ScenarioUtils.createScenario(config) ;
		
		Population pop = sc.getPopulation() ;
		PopulationFactory pf = pop.getFactory() ;
		
		Person person = pf.createPerson( Id.createPersonId("test") )  ;
		pop.addPerson(person);
		
		Plan plan = pf.createPlan() ;
		person.addPlan( plan ) ;
		
		Coord coord = null ;
		Activity act = pf.createActivityFromCoord("home", coord ) ;
		plan.addActivity(act); 
		
		Leg leg = pf.createLeg( TransportMode.car ) ;
		plan.addLeg(leg); 
		
		Id<Link> startLinkId = Id.createLinkId("startLink") ;
		Id<Link> endLinkId = Id.createLinkId("endLink") ;
		NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, startLinkId, endLinkId) ;
		leg.setRoute(route);
		
		List<Id<Link>> linkIds = new ArrayList<>() ;
		linkIds.add( Id.createLinkId("1") ) ;
		linkIds.add( Id.createLinkId("2") ) ;
		route.setLinkIds(startLinkId, linkIds, endLinkId);
		
		System.out.println( " route: " + route ); 
		System.out.println( " clone: " + route.clone() );
		
		MyObject obj = new MyObject( 22, "Nagel") ;
		MyObject copy = obj.createCopy();
		
		System.out.println( " obj  :" + obj );
		System.out.println( " clone:" + copy );
		
		copy.getName().replace("Nagel","Meier") ;
		
		System.out.println( " obj  :" + obj );
		System.out.println( " clone:" + copy );
		
	}

}
