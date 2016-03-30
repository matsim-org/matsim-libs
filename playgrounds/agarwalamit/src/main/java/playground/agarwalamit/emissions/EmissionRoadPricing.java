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
package playground.agarwalamit.emissions;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import playground.benjamin.internalization.EmissionCostModule;

/**
 * @author amit
 */

public class EmissionRoadPricing {

	/**
	 * @param timeBinSize timeBinSize for which toll is set
	 * @param scenario minimally have network
	 */
	public EmissionRoadPricing(double timeBinSize, Scenario scenario, EmissionModule emissionModule, EmissionCostModule emissionCostModule) {
//		ZZ_TODO : yet to complete this.
		this.scheme = new RoadPricingSchemeImpl();
		this.timeBinSize = timeBinSize;
		this.sc = scenario;
		this.emissionModule = emissionModule;
		this.ecm = emissionCostModule;
		this.weam = this.emissionModule.getWarmEmissionHandler().getWarmEmissionAnalysisModule();
	}

	private RoadPricingSchemeImpl scheme;
	private final double dayStartTime = 0;
	private final double dayEndTime = 30*3600;
	private double timeBinSize;
	private Scenario sc;
	private EmissionModule emissionModule;
	private WarmEmissionAnalysisModule weam;
	private final Integer roadType =28;
	private EmissionCostModule ecm ;

	/**
	 * Setting initial cost as zero for all links.
	 */
	private void setTollCost(){
		for(double time = dayStartTime;time <= dayEndTime; time = time+timeBinSize){
			for(Link link:sc.getNetwork().getLinks().values()){
				double amount = getEmissionCost(link);
				scheme.addLinkCost(link.getId(), time, time+timeBinSize, amount);
			}
		}
	}

	private double getEmissionCost(Link link) {
		
		double freeTravelTime = Math.floor(link.getLength()/link.getFreespeed())+1;
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Id<Vehicle> vehicleId = Id.createVehicleId("555524.1#11613");
		Id<VehicleType> vehicleTypeId = Id.create(HbefaVehicleCategory.PASSENGER_CAR.toString(), VehicleType.class);
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));
		
		Map<WarmPollutant, Double> warmEmissions = this.weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, roadType, link.getFreespeed(), 
				link.getLength(), freeTravelTime);
		double amount = ecm.calculateWarmEmissionCosts(warmEmissions);
		return amount;
	}

	public void run(){
		scheme.setType(RoadPricingScheme.TOLL_TYPE_LINK);
		setTollCost();
	}

	public RoadPricingScheme getScheme(){
		run();
		return this.scheme;
	}
	
	public void writeEmissionTollFile(String outputFile){
		RoadPricingWriterXMLv1 writer = new RoadPricingWriterXMLv1(scheme);
		writer.writeFile(outputFile);
	}
}
