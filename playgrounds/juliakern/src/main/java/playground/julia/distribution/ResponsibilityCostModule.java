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

package playground.julia.distribution;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class ResponsibilityCostModule {
	
	private Collection<EmActivity> activities;
	double timeBinSize;
	private Map<Id, Integer> link2xBins;
	private Map<Id, Integer> link2yBins;
	
	private final static Double dist0factor = 0.216;
	private final static Double dist1factor = 0.132;
	private final static Double dist2factor = 0.029;
	private final static Double dist3factor = 0.002;	

	public ResponsibilityCostModule(Collection<EmActivity> activities, Double timeBinSize, Map<Id, Integer> link2xBins, Map<Id, Integer> link2yBins){
		this.activities = activities;
		this.timeBinSize = timeBinSize;
		this.link2xBins = link2xBins;
		this.link2yBins = link2yBins;
	}

	public Double getDisutilityValue(Person person, Vehicle v, Link link,
			double time) {
		System.out.println("something !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		Double value=0.0;
		int xBin = link2xBins.get(link.getId());
		int yBin = link2yBins.get(link.getId());
		
		//time interval
		double endOfTimeInterval = Math.ceil(time/timeBinSize)*timeBinSize;
		if(endOfTimeInterval<timeBinSize) endOfTimeInterval = timeBinSize;
		double startOfTimeInterval = endOfTimeInterval - timeBinSize;
		
		for(EmActivity ema: activities){
			// x-bin
			int xDistance = Math.abs(xBin-ema.getXBin());
			if(xDistance>4) break;
			
			// y-bin
			int yDistance = Math.abs(yBin-ema.getYBin());
			if(xDistance + yDistance>= 4)break;
			
			Double distributionFactor =0.0;
			switch(xDistance+yDistance){
				case 0: distributionFactor = dist0factor; break;
				case 1: distributionFactor = dist1factor; break;
				case 2: distributionFactor = dist2factor; break;
				case 3: distributionFactor = dist3factor; break;
			}
			
			if(ema.getStartTime() >= startOfTimeInterval -MatsimTestUtils.EPSILON){
				if(ema.getEndTime()<= endOfTimeInterval + MatsimTestUtils.EPSILON){
					//TODO woher??
					Double emissionConcentration = 100.;
					value += ema.getDuration() * distributionFactor * emissionConcentration;
				}
			}
		}
		return value;
	}
}
