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

package playground.juliakern.responsibilityOffline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;

import playground.juliakern.distribution.Cell;

public class EmissionCostDensityHandler implements WarmEmissionEventHandler {

	private Double dist0factor = 0.216;
	private Double dist1factor = 0.132;
	private Double dist2factor = 0.029;
	private Double dist3factor = 0.002;
	
	/*Values taken from IMPACT (Maibach et al.(2008))*/
	private final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_SO2 = 11000. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_CO2 = 70. / (1000. * 1000.);
	
	private HashMap<Double, Double> timeBinsToAvgDensity;
	private Map<Id, Double> person2causedEmCosts;
	private Map<Id, Integer> link2xbins;
	private Map<Id, Integer> link2ybins;
	private double timeBinSize = 60*60.;
	private int noOfXCells = 160;
	private int noOfYCells = 120;
	private HashMap<Double, Double[][]> durations;

	public EmissionCostDensityHandler(HashMap<Double, Double[][]> durations,
			Map<Id, Integer> link2xbins, Map<Id, Integer> link2ybins) {
		
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		this.durations = durations;
		person2causedEmCosts = new HashMap<Id, Double>();
			timeBinsToAvgDensity = new HashMap<Double, Double>();
			
			for(Double timeBin : durations.keySet()){
				Double avgDen=0.0;
				for(int i=0; i< durations.get(timeBin).length; i++){
					for(int j=0; j< durations.get(timeBin)[i].length; j++){
						avgDen+= durations.get(timeBin)[i][j];
//						System.out.println(durations.get(timeBin)[i][j]);
					}
				}
				System.out.println("avg den" + avgDen);
				timeBinsToAvgDensity.put(timeBin, avgDen/noOfXCells/noOfYCells);
			}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		
		Map<WarmPollutant, Double> warmEmissions = event.getWarmEmissions();
		Id personId = event.getVehicleId();
		
		try {
			int xCell = link2xbins.get(event.getLinkId());
			int yCell = link2ybins.get(event.getLinkId());
			
			Double timeBin = Math.ceil(event.getTime()/timeBinSize )*timeBinSize;
			
			// calc pm costs
			Double pmCosts = warmEmissions.get(WarmPollutant.PM) * EURO_PER_GRAMM_PM2_5_EXHAUST;
//		Double pmCostsValue = warmEmissions.get("PM") * EURO_PER_GRAMM_PM2_5_EXHAUST;
			// distribute em costs

			Double noxCosts = warmEmissions.get(WarmPollutant.NOX) * EURO_PER_GRAMM_NOX;
			Double nmCosts = warmEmissions.get(WarmPollutant.NMHC) * EURO_PER_GRAMM_NMVOC;
			Double soCosts = warmEmissions.get(WarmPollutant.SO2) * EURO_PER_GRAMM_SO2;
			Double coCosts = warmEmissions.get(WarmPollutant.CO2_TOTAL) * EURO_PER_GRAMM_CO2;
			
			Double totalCosts = (pmCosts + noxCosts + nmCosts + soCosts + pmCosts + coCosts) * relativeFactor(timeBin, xCell, yCell);
//			System.out.println("NOX " + warmEmissions.get(WarmPollutant.NOX));
//			System.out.println("NMHC " + warmEmissions.get(WarmPollutant.NMHC));
//			System.out.println("SO2 " + warmEmissions.get(WarmPollutant.SO2));
			
			if(!person2causedEmCosts.containsKey(personId)){
				person2causedEmCosts.put(personId, totalCosts);
			}else{
				Double prevCosts = person2causedEmCosts.get(personId);
				person2causedEmCosts.put(personId, prevCosts +totalCosts);
			}
		} catch (NullPointerException e) {
			// not in research area
			
		}

	}

	private Double relativeFactor(Double timeBin, int xCell, int yCell) {
		
		Double relevantDuration = 0.0;
		
		for(int distance =0; distance <= 3; distance++){
			Cell cellOfLink = new Cell(xCell, yCell);
			List<Cell> distancedCells = cellOfLink .getCellsWithExactDistance(noOfXCells, noOfYCells, distance);
			for(Cell dc: distancedCells){
				// TODO better initialise with 0.0?
				try {
				if (durations.get(timeBin)[dc.getX()]!=null) {
					Double[][] durationsOfCurrentInterval = durations.get(timeBin);
					
						if (durationsOfCurrentInterval [dc.getX()][dc.getY()]!=null) {
							Double valueOfdc = durationsOfCurrentInterval[dc
									.getX()][dc.getY()];
							switch (distance) {
							case 0:
								relevantDuration += dist0factor * valueOfdc; 
								break;
							case 1:
								relevantDuration += dist1factor * valueOfdc;
								break;
							case 2:
								relevantDuration += dist2factor * valueOfdc;
								break;
							case 3:
								relevantDuration += dist3factor * valueOfdc;
								break;
							}
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					// nothing to do not in research area
				}
			}
		}
		return relevantDuration / timeBinsToAvgDensity.get(timeBin);		
	}

	public Map<Id, Double> getPerson2causedEmCosts() {
		return person2causedEmCosts;
	}

}
