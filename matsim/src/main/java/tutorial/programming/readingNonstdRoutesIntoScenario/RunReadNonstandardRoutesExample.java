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
package tutorial.programming.readingNonstdRoutesIntoScenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * This is is an example, taken from Thibaut, of how to set an additional route factory before the controler has started.
 * <p/>
 * This will generate the route, with start and end link id.  It will also set distance and travel time if they
 * are in the file.  This is sufficient information for teleportation.  Additional information needs to come from elsewhere, 
 * or the corresponding {@link PopulationReader} needs to be modified.
 * <p/>
 * Note, however, that for pure teleportation it is not necessary to set the route factory since the default {@link ModeRouteFactory} 
 * will already generate an instance of {@link GenericRoute} when a mode is not registered. 
 * 
 * @author nagel
 * @author thibautd
 *
 */
public class RunReadNonstandardRoutesExample {

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

