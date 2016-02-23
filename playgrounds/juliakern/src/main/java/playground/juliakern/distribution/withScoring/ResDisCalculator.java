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

package playground.juliakern.distribution.withScoring;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import playground.benjamin.internalization.EmissionCostModule;
import playground.juliakern.distribution.EmActivity;

public class ResDisCalculator implements TravelDisutility{
	
	private static final Logger logger = Logger.getLogger(EmissionControlerListener.class);
	
	private TravelDisutility delegate;
	private EmissionControlerListener ecl;
	private double timeBinSize;
	private Double dist0factor = 0.216;
	private Double dist1factor = 0.132;
	private Double dist2factor = 0.029;
	private Double dist3factor = 0.002;	
	private double marginalUtilityOfMoney;
	private EmissionModule emissionModule;
	private EmissionCostModule emissionCostModule;

	private double sumOfEmissionValues = 0.0;

	private double sumOfDelegateValues = 0.0;

	public ResDisCalculator(TravelDisutility travelDisutility, EmissionControlerListener ecl, double marginalutilityOfMoney, EmissionModule emissionModule, EmissionCostModule emissionCostModule){
		this.delegate = travelDisutility;
		this.ecl = ecl;
		this.timeBinSize = ecl.timeBinSize;
		this.marginalUtilityOfMoney = marginalutilityOfMoney;
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		Vehicle emissionVehicle = vehicle;
		if (vehicle == null){
			// the link travel disutility is asked without information about the vehicle
			if (person == null){
				// additionally, no person is given -> a default vehicle type is used
				Log.warn("No person and no vehicle is given to calculate the link travel disutility. The default vehicle type is used to estimate emission disutility.");
				emissionVehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("defaultVehicle"), VehicleUtils.getDefaultVehicleType());
			} else {
				// a person is given -> use the vehicle for that person given in emissionModule
				emissionVehicle = this.emissionModule.getEmissionVehicles().getVehicles().get(person.getId());
			}
		}
		
		double delegateValue = delegate.getLinkTravelDisutility(link, time, person, emissionVehicle);
		
		double emissionValue = 0.0;
		double personXactivityDuration = 0.0;
		ArrayList<EmActivity> allActivities = ecl.intervalHandler.getActivities();
		//TODO check: are activities still/yet there when needed?
		if(allActivities!=null){
			if(allActivities.size()>0){

				Double endOfTimeInterval = Math.ceil(time / timeBinSize) *timeBinSize;
				if(endOfTimeInterval==0.0)endOfTimeInterval=timeBinSize;
				
				for(EmActivity ema: allActivities){
					// if activity is nearby and in the same time interval : add count (persons x time x distribution factor)
					if(ema.getStartTime()<=endOfTimeInterval && ema.getEndTime()>endOfTimeInterval-timeBinSize){
						int cellDistanceBetweenLinks = getDistance(link, ema);
						if(cellDistanceBetweenLinks <4){
							// calculate duration of activity inside this time bin
							double relevantDuration = Math.min(endOfTimeInterval, ema.getEndTime()) - Math.max(endOfTimeInterval-timeBinSize, ema.getStartTime());
						
							switch(cellDistanceBetweenLinks){
								case 0: personXactivityDuration += dist0factor * relevantDuration; break;
								case 1: personXactivityDuration += dist1factor * relevantDuration; break;
								case 2: personXactivityDuration += dist2factor * relevantDuration; break;
								case 3: personXactivityDuration += dist3factor * relevantDuration; break;
							}
						}
					}
				}
			}
		}
		
		double expectedEmissionPrice = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), link.getLength()/link.getFreespeed()); //TODO get some value from vehicle type/emission vehicles as approx for generated emissions. might also depend on link length
		
		emissionValue = personXactivityDuration * expectedEmissionPrice * marginalUtilityOfMoney;
		// TODO scale? -> Benjamin
		if(personXactivityDuration>0.0){
		logger.info("expe em price" + expectedEmissionPrice + " mag ut of money" + marginalUtilityOfMoney);
		logger.info("calculated travel disut for link " + link.getId().toString() + " person " + person.getId().toString()
				+ " delegate value " + delegateValue + " emission value " + emissionValue);
		}
		
		if(emissionValue>0.0){
			sumOfEmissionValues += emissionValue;
			sumOfDelegateValues += delegateValue;
		}
		 logger.info("expected emission costs for person " + person.getId() + " on link " + link.getId() + " at time " + time + " are calculated to " + emissionValue);
		return delegateValue + emissionValue;
	}

	
		private double calculateExpectedEmissionDisutility(Vehicle vehicle, Link link, double distance, double linkTravelTime) {
		double linkExpectedEmissionDisutility;

		/* The following is an estimate of the warm emission costs that an agent (depending on her vehicle type and
		the average travel time on that link in the last iteration) would have to pay if chosing that link in the next
		iteration. Cold emission costs are assumed not to change routing; they might change mode choice or
		location choice (not implemented)! */
		
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = this.emissionModule.getWarmEmissionHandler().getWarmEmissionAnalysisModule();
		Map<WarmPollutant, Double> expectedWarmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
					vehicle,
					Integer.parseInt(((LinkImpl) link).getType()),
					link.getFreespeed(),
					distance,
					linkTravelTime
					);
		double expectedEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(expectedWarmEmissions);
		linkExpectedEmissionDisutility = this.marginalUtilityOfMoney * expectedEmissionCosts ;
	//	 logger.info("expected emission costs for person " + person.getId() + " on link " + link.getId() + " at time " + time + " are calculated to " + expectedEmissionCosts);

		return linkExpectedEmissionDisutility;
	}
	
	
	
	private int getDistance(Link link, EmActivity ema) {
		try {
			int xCellOfLink = ecl.links2xcells.get(link.getId());
			int yCellOfLink = ecl.links2ycells.get(link.getId());
			int xCellOfEma = ema.getXBin();
			int yCellOfEma = ema.getYBin();
			int distance = Math.abs(xCellOfLink-xCellOfEma)+Math.abs(yCellOfLink+yCellOfEma);
			return distance;
		} catch (NullPointerException e) {
			return 5;
		}
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return delegate.getLinkMinimumTravelDisutility(link);
	}
	
	public void printInfo(){
		logger.info("sum of em values: " + sumOfEmissionValues);
		logger.info("sum of deleg values: " + sumOfDelegateValues);
		logger.info("average em/dele: " + sumOfEmissionValues/sumOfDelegateValues);
	}

}
