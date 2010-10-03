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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteTest;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouter;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import org.matsim.pt.routes.ExperimentalTransitRoute;

/**
 *  Finds pt connection according to a priority : time or less transfers
 * @author manuel
 */
public class PrecalPlansCalcTransitRoute extends PlansCalcTransitRoute {
	private static final Logger log = Logger.getLogger(PrecalPlansCalcTransitRoute.class);
	private final AdaptedTransitRouter adaptedTransitRouter;

	public PrecalPlansCalcTransitRoute(final PlansCalcRouteConfigGroup config, final Network network,
			final PersonalizableTravelCost costCalculator, final PersonalizableTravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule,
			final TransitConfigGroup transitConfig, MyTransitRouterConfig myTransitRouterConfig) {
		super(config, network, costCalculator, timeCalculator, factory, schedule, transitConfig);
		this.adaptedTransitRouter = new AdaptedTransitRouter( myTransitRouterConfig, schedule);
	}

	@Override
	protected double handlePtPlan(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		StaticConnection fastestConnection= null;
		StaticConnection lessTransConnection= null;
		
		double travelTime = 0.0;
		
		double vdTime = depTime;
		//Set<StaticConnection> conections = new HashSet();
		System.out.println("starting");
		//String space = " ";
		for (vdTime = depTime+1800; vdTime>depTime-1800 ; vdTime-=180){   //find all possible connections in the last 30 mins and in the next 30 mins
			//System.out.println("vdtime + conections size: " + vdTime + space + conections.size());
			List<Leg> legs= this.adaptedTransitRouter.calcRoute(fromAct.getCoord(), toAct.getCoord(), vdTime);
		
			//calculate travelTime and distance
			travelTime = 0.0;
			double travelDistance = 0.0;
			int ptLegsNum = 0;
			String PT = "pt";
			if (legs != null) {
				for (Leg leg2 : legs) {
					travelTime += leg2.getTravelTime(); //calculate travel time
					if (leg2.getMode().equals(PT)){     //calculate num of pt trips
						ptLegsNum++;
					}
					//TODO: travelDistance += leg2.; //set distance
				}
			}
			

			StaticConnection staticConnection = new StaticConnection(legs, travelTime, travelDistance, ptLegsNum); 

			//connection with less transfers
			if (lessTransConnection == null || (ptLegsNum < lessTransConnection.getPtTripNum() && !lessTransConnection.equals(fastestConnection)) ){
				lessTransConnection = staticConnection;	
			}				

			//fastest connection			
			if (fastestConnection==null || travelTime< fastestConnection.getTravelTime()){
				fastestConnection= staticConnection;
			}

			
			/*
			boolean contained = false;
			for (StaticConnection conection : conections){
				if (conection.compareTo(staticConnection)==0){
					contained= true;
					break;
				}
			}
			
			ExperimentalTransitRoute expRoute = (ExperimentalTransitRoute)staticConnection.getLegs().get(1).getRoute();
			if (contained==false){
				conections.add(staticConnection);
			}
			*/
		}
		
		/*
		for (StaticConnection connection : conections ){
			System.out.println(connection.toString());
		}
		*/
		
		//super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, legs));
		/*
		double travelTime = 0.0;
		if (legs != null) {
			for (Leg leg2 : legs) {
				travelTime += leg2.getTravelTime();
			}
		}
		*/
		
		super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, lessTransConnection.getLegs() ));
		//super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, fastestConnection.getLegs() ));
		return travelTime;
	}
}
