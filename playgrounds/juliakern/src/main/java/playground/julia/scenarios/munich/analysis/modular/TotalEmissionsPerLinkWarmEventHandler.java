/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkWarmEventHandler.java
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
package playground.julia.scenarios.munich.analysis.modular;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.scenarios.munich.analysis.EmissionUtils;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.WarmPollutant;

/**
 * @author benjamin
 * 
 */
public class TotalEmissionsPerLinkWarmEventHandler implements
		WarmEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(TotalEmissionsPerLinkWarmEventHandler.class);

	Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = new HashMap<Double, Map<Id, Map<WarmPollutant, Double>>>();

	final int noOfTimeBins;
	final double timeBinSize;
	EmissionUtils emissionUtils;

	public TotalEmissionsPerLinkWarmEventHandler(double simulationEndTime,
			int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.emissionUtils = new EmissionUtils();
	}

	@Override
	public void reset(int iteration) {
		this.time2warmEmissionsTotal.clear();
		logger.info("Resetting warm emission aggregation to "
				+ this.time2warmEmissionsTotal);
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		double endOfTimeInterval = 0.0;

		if(0.0<=time && time <=this.noOfTimeBins*this.timeBinSize){
		endOfTimeInterval = (int) Math.ceil(time / this.timeBinSize)
				* this.timeBinSize;
		// was passiert mit Zeiten ausserhalb des Zeitintervalls? bisher
		// wurden und werden die uebergangen TODO andere Klassen

		Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

		if (this.time2warmEmissionsTotal.get(endOfTimeInterval) != null) {
			warmEmissionsTotal = this.time2warmEmissionsTotal.get(endOfTimeInterval);

			if (warmEmissionsTotal.get(linkId) != null) {
				warmEmissionsTotal.put(linkId, addEmissions(warmEmissionsTotal.get(linkId),warmEmissionsOfEvent));

			} else {
				warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
			}
		} else {
			warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
		}
		this.time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);

		}
	}

	private Map<WarmPollutant, Double> addEmissions(Map<WarmPollutant, Double> warmEmissionsSoFar, Map<WarmPollutant, Double> warmEmissionsOfEvent) {
		Map<WarmPollutant, Double> tore = new HashMap<WarmPollutant, Double>(warmEmissionsSoFar);
	for (Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()) {//TODO was passiert, wenn sofar emissionen hat, die das event nicht hat oder
		//anders herum? ueberlegen, ob das mit put gut geht TODO auch die anderen klasse ansehen
			WarmPollutant awp=entry.getKey();
			Double aEmission=entry.getValue();
			if(warmEmissionsSoFar.containsKey(awp)){
				tore.put(awp,aEmission+warmEmissionsSoFar.get(awp));//addieren	
	}
			else{
				tore.put(awp,aEmission);//neu erzeugen
			}
	}
		return tore;
	}
	

	public Map<Double, Map<Id, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return time2warmEmissionsTotal;
	}
}