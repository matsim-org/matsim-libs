/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcPtRoute.java
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

package playground.mmoyo.ptRouterAdapted.precalculation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.collections.Tuple;
//import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouter;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;

/**
 *  Finds pt connection according to a priority : time or less transfers
 * @author manuel
 */
public class PrecalPlansCalcTransitRoute extends PlansCalcTransitRoute {
	private static final Logger log = Logger.getLogger(PrecalPlansCalcTransitRoute.class);
	private final AdaptedTransitRouter adaptedTransitRouter;
	final String PT = "pt";
	final String nullString = "";
	final String COMMA = ",";
	final String NO_PT_FOUND = "no pt connection found";
	int kBestRoute = 0;
	
	public PrecalPlansCalcTransitRoute(final PlansCalcRouteConfigGroup config, final Network network,
			final PersonalizableTravelCost costCalculator, final PersonalizableTravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule,
			final TransitConfigGroup transitConfig, MyTransitRouterConfig myTransitRouterConfig) {
		super(config, network, costCalculator, timeCalculator, factory, schedule, transitConfig);
		this.adaptedTransitRouter = new AdaptedTransitRouter( myTransitRouterConfig, schedule);
	}

	@Override
	protected double handlePtPlan(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		double travelTime = 0.0;
		double vdTime = depTime;
		Map <Id, StaticConnection> connectionMap = new HashMap <Id, StaticConnection>(); //a map that stores precalculated pt connections
		List<Leg> selectedConnection = null;
		
		//System.out.print("\npt connections from " + fromAct.getCoord().toString() + " to " + toAct.getCoord().toString());
		for (vdTime = depTime+600; vdTime>=depTime-300; vdTime-=180){   //find all possible connections in next 10 mins
			List<Leg> legs= this.adaptedTransitRouter.calcRoute(fromAct.getCoord(), toAct.getCoord(), vdTime);
			
			//calculate travelTime and distance
			travelTime = 0.0;
			double travelDistance = 0.0;
			int ptLegsNum = 0;
			if (legs != null) {
				StringBuilder routeId =  new StringBuilder (nullString);
				for (Leg leg2 : legs) {
					travelTime += leg2.getTravelTime(); //calculate travel time
					if (leg2.getMode().equals(PT)){     //calculate num of pt trips
						routeId.append(((ExperimentalTransitRoute)leg2.getRoute()).getRouteDescription());
						routeId.append(COMMA);
						ptLegsNum++;
					}
					//TODO: travelDistance += leg2.; //set distance
				}
				
				if (ptLegsNum >0){ 
					StaticConnection staticConnection = new StaticConnection(new IdImpl(routeId.toString()), legs, travelTime, travelDistance, ptLegsNum);	
					if (!connectionMap.containsKey(staticConnection.getId())){
						connectionMap.put(staticConnection.getId(), staticConnection);	
					}
				}else{
					selectedConnection = legs; //select transit-walk connection in case that there is not a pt leg at all
				}
			}
		}

		
		//select a connection
		if (connectionMap.size() >0){
			StaticConnection [] connectionArray = connectionMap.values().toArray(new StaticConnection[connectionMap.size()]);
		
			//*sort it in case we may choose the "best" ones
			Arrays.sort(connectionArray);
		
			// in console
			/*
			//System.out.println();
			int transfer=0;
			for (StaticConnection connection : connectionArray ){
				if (transfer!=connection.getPtTripNum()){
					System.out.println(connection.getPtTripNum()-1 + " transfers");
					transfer= connection.getPtTripNum();
				}
				System.out.println(connection.getId() + "    " + connection.getPtTripNum() + "    " + connection.getTravelTime());
			}
		   */
		
			
			Random randomGenerator = new Random();
			if(kBestRoute <  connectionArray.length){ 
				if (connectionArray.length>1){
					// select randomly among the connections
					//kBestRoute = randomGenerator.nextInt(connectionArray.length-1);	

					//add here Pareto Optimal calculation
					
					//or select the fastest connection  with min transfer!!
					kBestRoute =0;
				}	
				selectedConnection = connectionArray[kBestRoute].getLegs(); 
			}
			randomGenerator = null;
			connectionArray= null;
			
		}else{   //if not pt legs return a transit walk leg
			log.warn(NO_PT_FOUND);
		}

		super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, selectedConnection ));
		//super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, lessTransConnection.getLegs() ));
		//super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, fastestConnection.getLegs() ));
		
		//matsim router
		//super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, legs));  
		
		connectionMap=null;
		
		return travelTime;
	}
}
