/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.randomizerPtRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility;

class PopulationRndPtRouter{ 
	
	
		public static void main(String[] args) {
			String configFile; 
			if (args.length==1){
				configFile = args[0];
			}else{
				configFile = "";
			}
			
			//load data
			DataLoader dataLoader = new DataLoader();
			Scenario scn =dataLoader.loadScenario(configFile);
			final TransitRouterConfig trConfig = new TransitRouterConfig( scn.getConfig() ) ; 
			final TransitSchedule schedule = scn.getTransitSchedule();
			final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);
			Population population = scn.getPopulation();
			
			//create randomizedPtRouter and PlansCalcTransitRoute
			final TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
			final Network network = scn.getNetwork();
			FreespeedTravelTimeAndDisutility freespeedTravelTimeCost = new FreespeedTravelTimeAndDisutility(scn.getConfig().planCalcScore());
			final TravelDisutility costCalculator = freespeedTravelTimeCost;
			final TravelTime timeCalculator = freespeedTravelTimeCost;
			final TripRouterFactoryBuilderWithDefaults builder =
				new TripRouterFactoryBuilderWithDefaults();
			builder.setTransitRouterFactory(
				new TransitRouterFactory() {
					@Override
					public TransitRouter get() {
						return new TransitRouterImpl(
							trConfig,
							new PreparedTransitSchedule(schedule),
							routerNetwork,
							ttCalculator,
							ttCalculator);
					}
				});
			final PlanRouter router =
				new PlanRouter(
						builder.build(
							scn ).instantiateAndConfigureTripRouter(
								new RoutingContextImpl(
									costCalculator,
									timeCalculator ) ) ); 
			
			//route population
			for ( Person p : population.getPersons().values() ) {
				router.run( p );
			}
		
			//write agents individual parameters in output directory  ??
			
			//write routed population
			String outDir = scn.getConfig().controler().getOutputDirectory();
			String routedPlansFile = outDir + "routedWithRandomizedPtRouter.xml.gz";
			System.out.println("writing output plan file..." + routedPlansFile);
			PopulationWriter popwriter = new PopulationWriter(population, network) ;
			popwriter.write(routedPlansFile) ;
		}
	}
