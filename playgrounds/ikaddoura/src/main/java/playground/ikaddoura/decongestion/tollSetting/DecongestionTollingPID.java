/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.decongestion.tollSetting;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.DecongestionConfigGroup.IntegralApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;

/**
 * 
 * PDI-based toll adjustment, where e(t) = average delay
 * 
 * @author ikaddoura
 */

public class DecongestionTollingPID implements DecongestionTollSetting, LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(DecongestionTollingPID.class);
	
	@Inject
	private DecongestionInfo congestionInfo;
	
	private Map<Id<Link>, LinkInfo> linkId2infoPreviousTollComputation = new HashMap<>();	
	private int tollUpdateCounter = 0;
	private final Map<Id<Link>, Map<Integer, Double>> linkId2time2totalDelayAllIterations = new HashMap<>();	
	
	private final Map<Id<Link>, Map<Integer, Double>> linkId2time2avgDelayAllIterations = new HashMap<>();	
	private final Map<Id<Link>, Map<Integer, Integer>> linkId2time2leavingAgents = new HashMap<>();

	@Override
	public void updateTolls() {
		
		final double K_p = congestionInfo.getDecongestionConfigGroup().getKp();
		final double K_i = congestionInfo.getDecongestionConfigGroup().getKi();
		final double K_d = congestionInfo.getDecongestionConfigGroup().getKd();
		
		final String integralApproach = congestionInfo.getDecongestionConfigGroup().getIntegralApproach().toString();
		
		final double timeBinSize = (double) this.congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
		final double capacityPeriod = this.congestionInfo.getScenario().getNetwork().getCapacityPeriod();
		final double flowCapacityFactor = this.congestionInfo.getScenario().getConfig().qsim().getFlowCapFactor();
		final double toleratedAvgDelay = this.congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC();
		final boolean msa = this.congestionInfo.getDecongestionConfigGroup().isMsa();
		final double blendFactorFromConfig = this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR();
		
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
			
			double flowCapacityHeadwaySec = Double.NEGATIVE_INFINITY;
			if (K_i != 0. && integralApproach.equals(IntegralApproach.UnusedHeadway.toString())) {
				flowCapacityHeadwaySec = capacityPeriod / ( this.congestionInfo.getScenario().getNetwork().getLinks().get(linkId).getCapacity() * flowCapacityFactor);
			}

			LinkInfo linkInfo = this.congestionInfo.getlinkInfos().get(linkId);
			for (Integer intervalNr : linkInfo.getTime2avgDelay().keySet()) {
				
				// 0) average delay
				
				double averageDelay = linkInfo.getTime2avgDelay().get(intervalNr);	
				if (averageDelay <= toleratedAvgDelay) {
					averageDelay = 0.0;
				}
								
				double toll = 0.;
				
				// 1) proportional term
				
				if (K_p != 0.) {
					toll += K_p * averageDelay;
				}
		
				// 2) integral term
				
//				// Ideen:
				// --> letzten positiven averageDelay merken, und dann totalDelay -= lastAverageDelay ; --> DONE (alpha = 1.0)
				// --> Mittel über die letzten positven averageDelays (exponential smoothing probably ok: lastAverageDelay = (1-alpha)*lastAvDelay + alpha*averageDelay ; ) --> DONE
				// --> \propto * (1/flow - 1/cap, so etwas wie die "headway reserve" oder "unused time headway") --> DONE

				if (K_i != 0.) {
					
					// 2a) average approach
					
					double avgDelayAllIterations = 0.;
					if (integralApproach.equals(IntegralApproach.Average.toString())) {
						if (averageDelay > congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {
							if (this.linkId2time2avgDelayAllIterations.get(linkId) == null) {
								avgDelayAllIterations = averageDelay;
								this.linkId2time2avgDelayAllIterations.put(linkId, new HashMap<>());
							} else {
								if (this.linkId2time2avgDelayAllIterations.get(linkId).get(intervalNr) == null) {
									avgDelayAllIterations = averageDelay;
								} else {
									avgDelayAllIterations =  (1 - congestionInfo.getDecongestionConfigGroup().getIntegralApproachAverageAlpha()) * this.linkId2time2avgDelayAllIterations.get(linkId).get(intervalNr)
											+ congestionInfo.getDecongestionConfigGroup().getIntegralApproachAverageAlpha() * averageDelay;
								}
							}
							this.linkId2time2avgDelayAllIterations.get(linkId).put(intervalNr, avgDelayAllIterations);
						}	
					}
					
					// 2b) unused headway approach
					
					double unusedHeadway = 0.;
					if (integralApproach.equals(IntegralApproach.UnusedHeadway.toString())) {
						double flowHeadwaySec = timeBinSize;
						if (this.linkId2time2leavingAgents.get(linkId) != null && this.linkId2time2leavingAgents.get(linkId).get(intervalNr) != null) {
							flowHeadwaySec = timeBinSize / this.linkId2time2leavingAgents.get(linkId).get(intervalNr);
						}
						unusedHeadway = flowHeadwaySec - flowCapacityHeadwaySec;
						if (unusedHeadway < 0.) unusedHeadway = 0.; // there is no unused Headway
					}

					// update the total delay over all iterations
					double totalDelayAllIterations = 0.;
					if (linkId2time2totalDelayAllIterations.get(linkId) == null) {	
						totalDelayAllIterations = averageDelay;
						this.linkId2time2totalDelayAllIterations.put(linkId, new HashMap<>());
					
					} else {
						
						if (this.linkId2time2totalDelayAllIterations.get(linkId).get(intervalNr) == null) {
							totalDelayAllIterations = averageDelay;
						
						} else {	
													
							if (averageDelay <= congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {
								
								if (integralApproach.equals(IntegralApproach.Average.toString())) {
									totalDelayAllIterations = this.linkId2time2totalDelayAllIterations.get(linkId).get(intervalNr)
											- avgDelayAllIterations;
								} else if (integralApproach.equals(IntegralApproach.UnusedHeadway.toString())) {
									totalDelayAllIterations = this.linkId2time2totalDelayAllIterations.get(linkId).get(intervalNr)
											- (congestionInfo.getDecongestionConfigGroup().getIntegralApproachUnusedHeadwayFactor() * unusedHeadway);
								} else if (integralApproach.equals(IntegralApproach.Zero.toString())) {
									totalDelayAllIterations = 0.;
								} else {
									throw new RuntimeException("Unknown integral approach. Aborting...");
								}
									
							} else {
								totalDelayAllIterations = this.linkId2time2totalDelayAllIterations.get(linkId).get(intervalNr) 
										+ averageDelay;
							}
						}
					}
					this.linkId2time2totalDelayAllIterations.get(linkId).put(intervalNr, totalDelayAllIterations);				
					toll += K_i * totalDelayAllIterations;
				}
				
				// 3) derivative term
				
				if (K_d != 0.) {
					double previousDelay = 0.;
					if (this.linkId2infoPreviousTollComputation.get(linkId) != null 
							&& this.linkId2infoPreviousTollComputation.get(linkId).getTime2avgDelay().get(intervalNr) != null) {
						previousDelay = this.linkId2infoPreviousTollComputation.get(linkId).getTime2avgDelay().get(intervalNr);
					}
				
					double deltaDelay = averageDelay - previousDelay;
					toll += K_d * deltaDelay;
				}
				
				// 4) prevent negative tolls
				
				if (toll < 0) {
					toll = 0;
				}
				
				// 5) smoothen the tolls
				
				Double previousToll = linkInfo.getTime2toll().get(intervalNr);

				double blendFactor;
				if (msa) {
					if (this.tollUpdateCounter > 0) {
						blendFactor = 1.0 / (double) this.tollUpdateCounter;
					} else {
						blendFactor = 1.0;
					}
				} else {
					blendFactor = blendFactorFromConfig;
				}
				
				double smoothedToll;
				if (previousToll != null && previousToll >= 0.) {
					smoothedToll = toll * blendFactor + previousToll * (1 - blendFactor);
				} else {
					smoothedToll = toll;
				}
								
				// 6) store the updated toll
				
				linkInfo.getTime2toll().put(intervalNr, smoothedToll);

			}	
		}
		
		log.info("Updating tolls completed.");
		this.tollUpdateCounter++;
		
		// store the current link information for the next toll computation
		
		linkId2infoPreviousTollComputation = new HashMap<>();
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {

			Map<Integer, Double> time2previousDelay = new HashMap<>();
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {
				time2previousDelay.put(intervalNr, this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr));
			}
			
			LinkInfo linkInfoPreviousTollComputation = new LinkInfo(linkId);
			linkInfoPreviousTollComputation.setTime2avgDelay(time2previousDelay);
			linkId2infoPreviousTollComputation.put(linkId, linkInfoPreviousTollComputation);
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2time2leavingAgents.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int timeBinNr = getIntervalNr(event.getTime());

		Id<Link> linkId = event.getLinkId();
		
		if (linkId2time2leavingAgents.get(linkId) != null) {
			
			if (linkId2time2leavingAgents.get(linkId).get(timeBinNr) != null) {
				int leavingAgents = linkId2time2leavingAgents.get(linkId).get(timeBinNr) + 1;
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, leavingAgents);
				
			} else {
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, 1);
			}
			
		} else {
			Map<Integer, Integer> time2leavingAgents = new HashMap<>();
			time2leavingAgents.put(timeBinNr, 1);
			linkId2time2leavingAgents.put(linkId, time2leavingAgents);
			
		}
	}
	
	private int getIntervalNr(double time) {
		double timeBinSize = congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();		
		return (int) (time / timeBinSize);
	}

}

