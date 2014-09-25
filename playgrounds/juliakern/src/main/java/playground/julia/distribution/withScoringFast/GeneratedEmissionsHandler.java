/* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionEvent.java
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

package playground.julia.distribution.withScoringFast;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import playground.julia.distribution.EmPerCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * class to handle warm and cold emission events
 * 
 * for each emission event: distribute the emission value onto nearby cells and/or links
 * store information as emission per link/ emission per cell
 * 
 * @author julia, benjamin
 */
public class GeneratedEmissionsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	Double simulationStartTime;
	Double timeBinSize;
	Map<Double, ArrayList<EmPerCell>> emissionPerCell;
	Map<Id<Link>,Integer> link2xbins; 
	Map<Id<Link>,Integer> link2ybins;
	
	private final static Double dist0factor = 0.216;
	private final static Double dist1factor = 0.132;
	private final static Double dist2factor = 0.029;
	private final static Double dist3factor = 0.002;	
	
	/*
	 * see playground.benjamin.internalization.EmissionCostModule
	 * 
	 * Values taken from IMPACT (Maibach et al.(2008)) 
	 * */
	private final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_SO2 = 11000. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_CO2 = 70. / (1000. * 1000.);
	
	
	public GeneratedEmissionsHandler(Double simulationStartTime, Double timeBinSize, Map<Id<Link>, Integer> link2xbins, Map<Id<Link>, Integer> link2ybins){
		this.simulationStartTime = simulationStartTime;
		this.timeBinSize= timeBinSize;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		this.emissionPerCell = new HashMap<Double, ArrayList<EmPerCell>>();
	}
	
	@Override
	public void reset(int iteration) {	
		this.emissionPerCell = new HashMap<Double, ArrayList<EmPerCell>>();
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		//event information
		Id linkId = event.getLinkId();
		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		Double eventStartTime = event.getTime();
		
		if (xBin != null && yBin != null) {
			// TODO person id statt vehicleid??? woher?
			Id personId = event.getVehicleId();
			// monetary value: Eur/(person exposure second)

			Double value = new Double(0.0);
			value += EURO_PER_GRAMM_NMVOC * event.getColdEmissions().get(ColdPollutant.NMHC);
			value += EURO_PER_GRAMM_NOX * event.getColdEmissions().get(ColdPollutant.NOX);
			value += EURO_PER_GRAMM_PM2_5_EXHAUST * event.getColdEmissions().get(ColdPollutant.PM);

			// distribute onto cells
			ArrayList<EmPerCell> arrayEpb = new ArrayList<EmPerCell>();
			arrayEpb = distributeOnCells(xBin, yBin, personId, value, eventStartTime);
			Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
			if (!emissionPerCell.containsKey(endOfTimeIntervall)) {
				emissionPerCell.put(endOfTimeIntervall, new ArrayList<EmPerCell>());
			}
			emissionPerCell.get(endOfTimeIntervall).addAll(arrayEpb);
		}
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		// event information 
		Id linkId = event.getLinkId();
		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		Double eventStartTime = event.getTime();
		
		if (xBin != null && yBin != null) {
			//TODO person id statt vehicleid??? woher?
			Id personId = event.getVehicleId();
			Double value = new Double(.0);
			value += EURO_PER_GRAMM_CO2 * event.getWarmEmissions().get(WarmPollutant.CO2_TOTAL);
			value += EURO_PER_GRAMM_NMVOC * event.getWarmEmissions().get(WarmPollutant.NMHC);
			value += EURO_PER_GRAMM_NOX * event.getWarmEmissions().get(WarmPollutant.NOX);
			value += EURO_PER_GRAMM_PM2_5_EXHAUST * event.getWarmEmissions().get(WarmPollutant.PM);
			value += EURO_PER_GRAMM_SO2 * event.getWarmEmissions().get(WarmPollutant.SO2);
			
			//distribute onto cells
			ArrayList<EmPerCell> arrayEpb = new ArrayList<EmPerCell>();
			arrayEpb= distributeOnCells(xBin, yBin, personId, value, eventStartTime);
			Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
			if (!emissionPerCell.containsKey(endOfTimeIntervall)) {
				emissionPerCell.put(endOfTimeIntervall,	new ArrayList<EmPerCell>());
			}
			emissionPerCell.get(endOfTimeIntervall).addAll(arrayEpb);
			System.out.println("link " + linkId.toString() +  "warm emission amounts " + event.getWarmEmissions().toString());
			System.out.println("toll (undistributed, not weighted) in euro " + value);
		}
	}

	

	private ArrayList<EmPerCell> distributeOnCells(Integer xBin, Integer yBin,
			Id personId, Double value, Double eventStartTime) {
		
		/*
		 * negative emission values are distributed as well
		 *
		 * distribute the emission value onto 25 cells:
		 * use the distance from the source cell as a measure for the distribution weights.
		 * origin cell factor: 0.216, distance = 1 -> 0.132, distance = 2 -> 0.029,
		 * distance = 3 -> 0.002 
		 * values are oriented at a normalized Gaussian distribution
		 * and therefore add up to 1.0
		 * 
		 */
		
		ArrayList<EmPerCell> distributedEmissions = new ArrayList<EmPerCell>();
		
		// distribute value onto cells: origin ... dist(origin)=3
		// factors depending on distance (measured by number of cells)
		for(int xIndex = xBin-3; xIndex<=xBin+3; xIndex++){
			for(int yIndex = yBin-3; yIndex <= yBin+3; yIndex++){
				Double distributionFactor = 0.0;
				int distance = Math.abs(xBin-xIndex)+Math.abs(yBin-yIndex);
				
				switch(distance){
				case 0: distributionFactor = dist0factor; break;
				case 1: distributionFactor = dist1factor; break;
				case 2: distributionFactor = dist2factor; break;
				case 3: distributionFactor = dist3factor; break;
				}
				
				if (distributionFactor>0.0) {
					EmPerCell epb = new EmPerCell(xIndex, yIndex, personId, value	* distributionFactor, eventStartTime);
					distributedEmissions.add(epb);
				}
			}			
		}
		return distributedEmissions;
	}

	private Double getEndOfTimeInterval(double time) {
		Double end = Math.ceil(time/timeBinSize)*timeBinSize;
		if(end>0.0) return end;
		return timeBinSize;
	}

	public Map<Double, ArrayList<EmPerCell>> getEmissionsPerCell() {
		return emissionPerCell;
	}
}
