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

package playground.benjamin.scenarios.munich.analysis.exposure;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.vehicles.Vehicle;

import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * 
 * @author julia
 *
 */

public class EmissionCostsByGroupsAndTimeBinsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler{

	private PersonFilter personFilter;
	private HashMap<Integer, GroupLinkFlatEmissions> timeBin2group2link2flatEmissionCost;
	private EmissionCostModule ecm;
	private double timeBinSize;
	private int numberOfTimeBins;

	public EmissionCostsByGroupsAndTimeBinsHandler(double timeBinSize, int numberOfTimeBins){
		this.timeBinSize = timeBinSize;
		this.numberOfTimeBins = numberOfTimeBins;
		this.personFilter = new PersonFilter();
		this.timeBin2group2link2flatEmissionCost = new HashMap<Integer, GroupLinkFlatEmissions>();
		for(int i=0; i<numberOfTimeBins; i++){
			timeBin2group2link2flatEmissionCost.put(i, new GroupLinkFlatEmissions());
		}
		EmissionsConfigGroup emissionsConfigGroup = new EmissionsConfigGroup();
		emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);
		this.ecm = new EmissionCostModule(emissionsConfigGroup);
	}
	@Override
	public void reset(int iteration) {	
	}


	
	@Override
	public void handleEvent(ColdEmissionEvent event) {
		// calc emission costs of event
		double emissionCost = ecm.calculateColdEmissionCosts(event.getColdEmissions());
		
		// calc time bin of event
		int timeBin = getTimeBin(event.getTime());
		
		// user group
		UserGroup userGroup = getUserGroup(event.getVehicleId());
		
		Id<Link> linkId = event.getLinkId();
		
		// store cost for (timebin x usergroup x link) combination
		timeBin2group2link2flatEmissionCost.get(timeBin).addEmissionCosts(userGroup, linkId, emissionCost);
		
	}
	
	private int getTimeBin(double time) {
		int timeBin = (int) Math.floor(time/timeBinSize);
		if(timeBin<numberOfTimeBins)return timeBin;
		return (numberOfTimeBins-1);
	}

	private UserGroup getUserGroup(Id<Vehicle> vehicleId) {
		if(personFilter.isPersonFromMID(vehicleId)) return UserGroup.URBAN;
		if(personFilter.isPersonFreight(vehicleId)) return UserGroup.FREIGHT;
		if(personFilter.isPersonInnCommuter(vehicleId)) return UserGroup.COMMUTER;
		if(personFilter.isPersonOutCommuter(vehicleId)) return UserGroup.REV_COMMUTER;
		return null;
	}

	public HashMap<Integer, GroupLinkFlatEmissions> getAllFlatCosts() {
		return timeBin2group2link2flatEmissionCost;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event){
		// calc emission costs of event
		double emissionCost = ecm.calculateWarmEmissionCosts(event.getWarmEmissions());
		
		// calc time bin of event
		int timeBin = getTimeBin(event.getTime());
		
		// user group
		UserGroup userGroup = getUserGroup(event.getVehicleId());
		
		Id<Link> linkId = event.getLinkId();
		
		// store cost for (timebin x usergroup x link) combination
		timeBin2group2link2flatEmissionCost.get(timeBin).addEmissionCosts(userGroup, linkId, emissionCost);
	}

}
