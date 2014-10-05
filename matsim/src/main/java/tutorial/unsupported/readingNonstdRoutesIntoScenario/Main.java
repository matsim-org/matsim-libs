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

/**
 * 
 */
package tutorial.unsupported.readingNonstdRoutesIntoScenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * This is is an example, taken from Thibaut, of how to set an additional route factory before the controler has started.
 * 
 * @author nagel
 * @author thibautd
 *
 */
public class Main {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		Scenario scenario = ScenarioUtils.createScenario(config);
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		modeRouteFactory.setRouteFactory( "mySpecialMode" , new RouteFactory(){
			@Override
			public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
				// TODO Auto-generated method stub
				return null;
			}} );
		ScenarioUtils.loadScenario( scenario );
		Controler controler = new Controler( scenario ) ;
		controler.run() ;

	}

}

