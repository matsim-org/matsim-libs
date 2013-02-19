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

package playground.mmoyo.utils.counts;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.ExpTransRouteUtils;
import playground.mmoyo.utils.Generic2ExpRouteConverter;

/**
 * Enumerates the transit stops that the whole routed population actually uses. 
 */

public class CountsInPopPtRoutes implements PersonAlgorithm {
	final Network net;
	final TransitSchedule schedule;
	final boolean onlySelPlans;
	private Generic2ExpRouteConverter converter;
	private final Set<Id> stopsIdSet = new HashSet<Id>();
	
	public CountsInPopPtRoutes (final Network net, final TransitSchedule schedule, final boolean onlySelPlans){
		this.net = net;
		this.schedule = schedule;
		this.onlySelPlans = onlySelPlans;
		converter = new Generic2ExpRouteConverter(this.schedule);
	}
	
	@Override
	public void run(Person person) {
		if (onlySelPlans){
			stopSearch(person.getSelectedPlan());
		}else{
			for(Plan plan : person.getPlans()){
				stopSearch(plan);
			}
		}
	}
	
	private void stopSearch(final Plan plan){
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof LegImpl) {
				Leg leg = (LegImpl)pe;
				if(leg.getMode().equals(TransportMode.pt) ){
					if (leg.getRoute()!= null && (leg.getRoute() instanceof org.matsim.api.core.v01.population.Route)){
						ExperimentalTransitRoute expRoute = converter.convert((GenericRouteImpl) leg.getRoute());
						ExpTransRouteUtils  expTransRouteUtils  = new ExpTransRouteUtils(net, schedule, expRoute);
						stopsIdSet.addAll(expTransRouteUtils.getStopsIds());
					}
				}
			}
		}
	}
	
	private Set<Id> getStopsInPopPtRoutes (){
		return stopsIdSet;
	}
	
	public static void main(String[] args) {
		String popFile = "../../";
		String netFile= "../../";
		String scheduleFile="../../";
		boolean onlySelPlan= false;
		
		//load data
		DataLoader dataLoader= new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFile);
		Network net = scn.getNetwork();
		TransitSchedule schedule = dataLoader.readTransitSchedule(scheduleFile);
		
		CountsInPopPtRoutes countsInPopPtRoutes = new CountsInPopPtRoutes(net, schedule, onlySelPlan);
		new PopSecReader(scn, countsInPopPtRoutes).readFile(popFile);
		
		//System.out.println(countsInPopPtRoutes.getStopsInPopPtRoutes());
		
		//convert to stop zones
		Set<String> stopzonesIdSet = new HashSet<String>();
		for(Id id :countsInPopPtRoutes.getStopsInPopPtRoutes()){
			stopzonesIdSet.add(	Real2PseudoId.convertRealIdtoPseudo(id.toString()));
		}
		for(String strId : stopzonesIdSet){
			System.out.println(strId);
		}
		
		
	}

	

}
