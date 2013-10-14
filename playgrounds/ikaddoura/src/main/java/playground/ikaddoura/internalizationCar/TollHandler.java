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

/**
 * 
 */
package playground.ikaddoura.internalizationCar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

/**
 * @author ikaddoura
 *
 */
public class TollHandler implements MarginalCongestionEventHandler {
	private static final Logger log = Logger.getLogger(TollHandler.class);
	private double timeBinSize = 900.;
	
	private List<TollInfo> tollInfos = new ArrayList<TollInfo>();
	private List<Id> linkIds = new ArrayList<Id>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgToll = new HashMap<Id, Map<Double, Double>>();
	private double vtts_car;
	
	public TollHandler(Scenario scenario) {
		this.vtts_car = (scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS_car: " + vtts_car);
	}

	@Override
	public void reset(int iteration) {
		log.info("-----> Iteration (" + iteration + ") begins. Clear all informations of the previous iteration (" + (iteration-1) + ").");
		this.tollInfos.clear();
		this.linkId2timeBin2avgToll.clear();
		this.linkIds.clear();
	}

	@Override
	public void handleEvent(MarginalCongestionEvent event) {
				
		TollInfo tollInfo = new TollInfo();
		double amount = event.getDelay() / 3600 * this.vtts_car;
		tollInfo.setAmount(amount);
		tollInfo.setTime(event.getTime());
		tollInfo.setLinkId(event.getLinkId());
		this.tollInfos.add(tollInfo);
		
		if (!linkIds.contains(event.getLinkId())){
			linkIds.add(event.getLinkId());
		}
	}

	public void setLinkId2timeBin2avgToll() {
		for (Id linkId : this.linkIds) {
			Map<Double, Double> timeBin2avgToll = new HashMap<Double, Double>();
			
			for (double time = 0; time < (30 * 3600); ){
				time = time + this.timeBinSize;
				
				List<Double> amounts = new ArrayList<Double>();
				for (TollInfo tollInfo : this.tollInfos){
					
					if (tollInfo.getLinkId().toString().equals(linkId.toString())){

						if (tollInfo.getTime() < time && tollInfo.getTime() >= (time - this.timeBinSize)){
							amounts.add(tollInfo.getAmount());
						}
					}
				}
				
				double sum = 0;
				double n = 0;
				for (Double amount : amounts) {
					sum = sum + amount;
					n++;
				}
				double avgToll = sum / n;
				timeBin2avgToll.put(time, avgToll);
			}
			this.linkId2timeBin2avgToll.put(linkId, timeBin2avgToll);
		}
		log.info(this.linkId2timeBin2avgToll.toString());
		
	}

	/**
	 * Returns the avg toll (monetary amount) paid on that link during that time bin.
	 */
	public double getAvgToll(Link link, double time) {
		double avgToll = 0.;
		
		if (this.linkId2timeBin2avgToll.containsKey(link.getId())){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgToll.get(link.getId());
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgToll = timeBin2avgToll.get(timeBin);
				}
			}
		}
		
		return avgToll;
	}
}
