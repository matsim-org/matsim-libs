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

package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.ExpTransRouteUtils;
import playground.mmoyo.utils.Generic2ExpRouteConverter;

public class PtPlanAnalyzer {
	
	private final String STR_PTINTERACTION = "pt interaction";
	private final Network network;
	private final TransitSchedule schedule;
	private Generic2ExpRouteConverter converter;
	
	public PtPlanAnalyzer (final Network network, final TransitSchedule schedule){
		this.network = network;
		this.schedule = schedule;
		converter = new Generic2ExpRouteConverter(this.schedule);
	}
	
	public PtPlanAnalysisValues run(Plan plan) {
		Activity aAct = ((Activity)plan.getPlanElements().get(0));
		Activity bAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));

		int pe_idx = 0;
		Activity lastAct= null;
		Activity nextAct;
		int transfers_num = 0;
		int ptLegs_num=0;
		int ptTrips_num=0;
		double walkDistance = 0.0;
		double transitWalkTime_secs = 0.0;
		double inVehDist_mts = 0.0;
		double trTravelTime_secs = 0.0;
		boolean currentlyPtMode= false;
		
		for (PlanElement pe : plan.getPlanElements()){

			//pt interaction 
			if (pe instanceof Activity) {
				Activity act = (Activity)pe;
				if (act!= aAct  && act!=bAct ){  //it is not first or last activity
					nextAct= (Activity) plan.getPlanElements().get(pe_idx+2);
					Leg nextLeg= (Leg) plan.getPlanElements().get(pe_idx+1);
					if(act.getType().equals(STR_PTINTERACTION) && lastAct.getType().equals(STR_PTINTERACTION) && nextAct.getType().equals(STR_PTINTERACTION) && nextLeg.getMode().equals(TransportMode.transit_walk) /*&& nextLeg.getMode().equals(TransportMode.transit_walk) */){
						transfers_num++;
					}	
				}
				
				if (!act.getType().equals(STR_PTINTERACTION)){ //this is to find out if it is a normal activity, in order to help to count original pt connections
					currentlyPtMode = false;
				}
				lastAct= act;
			}else{

				//Leg
				LegImpl leg = (LegImpl)pe;
				nextAct= (Activity) plan.getPlanElements().get(pe_idx+1);
				
				//get transit walk distances
				if (leg.getMode().equals(TransportMode.transit_walk)){
					walkDistance +=  CoordUtils.calcDistance(lastAct.getCoord() , nextAct.getCoord());
					transitWalkTime_secs += leg.getTravelTime();
				} 
				
				//get in pt vehicle distance and time
				else if (leg.getMode().equals(TransportMode.pt)){	
					if (!currentlyPtMode){
						ptLegs_num++;
						currentlyPtMode=true;
					}
					if (leg.getRoute()!= null) {
						//if (leg.getRoute() instanceof ExperimentalTransitRoute){
							//ExperimentalTransitRoute expTrRoute = ((ExperimentalTransitRoute)leg.getRoute());
						if (leg.getRoute()!= null && (leg.getRoute() instanceof org.matsim.api.core.v01.population.Route)){
							ExperimentalTransitRoute expRoute = converter.convert((GenericRouteImpl) leg.getRoute());
							
							ExpTransRouteUtils ptRouteUtill = new ExpTransRouteUtils(network, schedule, expRoute);
							inVehDist_mts += ptRouteUtill.getExpRouteDistance();
							trTravelTime_secs +=  leg.getTravelTime();
							ptTrips_num++;
						}
					}	
				}
			}
			pe_idx++;

		}//for planelement

		//store values
		PtPlanAnalysisValues values= new PtPlanAnalysisValues();
		values.setAgentId(plan.getPerson().getId());
		values.setTransitWalkTime_secs(transitWalkTime_secs);
		values.setTrTravelTime_secs(trTravelTime_secs);
		values.setInVehDist_mts(inVehDist_mts);
		values.setPtLegs_num(ptLegs_num);
		values.setPtTrips_num(ptTrips_num);
		values.setTransfers_num(transfers_num);
		return values;
	}//for plan
	
	public class PtPlanAnalysisValues{
		private Id agentId;
		private double transitWalkTime_secs;
		private double trTravelTime_secs;
		private double inVehDist_mts;
		private int ptLegs_num;
		private int ptTrips_num;
		private int transfers_num;
		
		public Id getAgentId() {
			return agentId;
		}
		
		private void setAgentId(Id agentId) {
			this.agentId = agentId;
		}
		public double getTransitWalkTime_secs() {
			return transitWalkTime_secs;
		}
		private void setTransitWalkTime_secs(double transitWalkTime_secs) {
			this.transitWalkTime_secs = transitWalkTime_secs;
		}
		public double trTravelTime_secs() {
			return trTravelTime_secs;
		}
		protected void setTrTravelTime_secs(double trTravelTime_secs) {
			this.trTravelTime_secs = trTravelTime_secs;
		}
		public double getInVehDist_mts() {
			return inVehDist_mts;
		}
		private void setInVehDist_mts(double inVehDist_mts) {
			this.inVehDist_mts = inVehDist_mts;
		}
		public int getPtLegs_num() {
			return ptLegs_num;
		}
		private void setPtLegs_num(int ptLegs_num) {
			this.ptLegs_num = ptLegs_num;
		}
		public int getPtTrips_num() {
			return ptTrips_num;
		}
		private void setPtTrips_num(int ptTrips_num) {
			this.ptTrips_num = ptTrips_num;
		}
		public int getTransfers_num() {
			return transfers_num;
		}
		private void setTransfers_num(int transfers_num) {
			this.transfers_num = transfers_num;
		}
	}//class PtPlanAnalysisValues
	
}
