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
package playground.ikaddoura.intervalBasedCongestionPricing.handler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.intervalBasedCongestionPricing.CongestionInfoWriter;
import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo;
import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo.InternalizationApproach;

/**
 * The centerpiece of the interval-based congestion pricing approach.
 * Checks the events' time and processes each time interval. Computes tolls and throws agent money events based on each link's congestion level,
 * 
 * @author ikaddoura
 *
 */

public class TimeTracker implements LinkLeaveEventHandler {

	private static final Logger log = Logger.getLogger(TimeTracker.class);
	
	private final CongestionInfo congestionInfo;
	private final EventsManager eventsManager;
	private final Scenario scenario;
	
	private int warnCnt = 0;
	
	// iteration-specific information
	private int iteration;
	private double totalTollPaymentsPerDay_monetaryUnits = 0.;
	
	// time-specific information
	private double currentTime = 0.;
	private String outputDirectory;
	
	public TimeTracker(CongestionInfo congestionInfo, EventsManager events) {
		this.scenario = congestionInfo.getScenario();
		this.congestionInfo = congestionInfo;
		this.eventsManager = events;	
	}

	@Override
	public void reset(int iteration) {
		
		this.outputDirectory = this.scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + iteration + "/";
		log.info("Setting the output directory to " + outputDirectory);
		
		this.iteration = iteration;
		this.totalTollPaymentsPerDay_monetaryUnits = 0.;
		this.currentTime = 0.;
		
		this.congestionInfo.setCurrentTimeBinEndTime(this.congestionInfo.getTIME_BIN_SIZE());
		this.congestionInfo.getCongestionLinkInfos().clear();
		this.congestionInfo.getVehicleId2personId().clear();
	}
	
	public double getTotalTollPayments() {
		return totalTollPaymentsPerDay_monetaryUnits;
	}

	private void checkTime(double time) {
		// Check for every event that is thrown if the current interval has changed.
		
		if (time > this.congestionInfo.getCurrentTimeBinEndTime()) {
			// All events of the current time bin were processed.
			
			while (time > this.congestionInfo.getCurrentTimeBinEndTime()) {
				currentTime = time;
				processTimeBin();
			}			
		}
	}
	
	private void processTimeBin() {
		
		log.info("##################################################");
		log.info("# Computing congestion for time interval " + Time.writeTime(this.congestionInfo.getCurrentTimeBinEndTime(), Time.TIMEFORMAT_HHMMSS) + " #");
		log.info("##################################################");

		processDelaysForCurrentTimeInterval(); 
		updateCurrentTimeInterval(); // Set the current time bin to the next one ( current time bin = current time bin + time bin size ).
		resetCurrentTimeIntervalInfo(); // Reset all time-specific information from the previous time interval.
	}

	private void processDelaysForCurrentTimeInterval() {
		boolean writeOutputForThisInterval = false;
		
		for (Id<Link> linkId : this.congestionInfo.getCongestionLinkInfos().keySet()) {
			
			if (this.congestionInfo.getCongestionLinkInfos().get(linkId).getLeavingVehicles().size() > 0) {
				writeOutputForThisInterval = true;
				
				double freespeedTravelTime_sec = this.scenario.getNetwork().getLinks().get(linkId).getLength() / this.scenario.getNetwork().getLinks().get(linkId).getFreespeed();
				double vtts_hour = (this.scenario.getConfig().planCalcScore().getPerforming_utils_hr() - this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

				if (warnCnt == 0) {
					log.info("VTTS_hour: " + vtts_hour);
					log.info("Marginal utility of money " + this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
					log.info("Marginal utility of performing an activity: " + this.scenario.getConfig().planCalcScore().getPerforming_utils_hr());
					log.info("Marginal utility of travelling by car " + this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());

					warnCnt++;
				}
				
				double amount = 0.;
		
				if (this.congestionInfo.getINTERNALIZATION_APPROACH().equals(InternalizationApproach.AverageDelay)) {
					double averageTravelTimePerAgent_sec = this.congestionInfo.getCongestionLinkInfos().get(linkId).getTravelTimeSum_sec() / this.congestionInfo.getCongestionLinkInfos().get(linkId).getLeavingVehicles().size();
					double averageDelayPerAgent_sec = averageTravelTimePerAgent_sec - freespeedTravelTime_sec;
					amount = -1.0 * averageDelayPerAgent_sec * vtts_hour / 3600.;
				
				} else if (this.congestionInfo.getINTERNALIZATION_APPROACH().equals(InternalizationApproach.LastAgentsDelay)) {
					double delayLastAgent_sec = this.congestionInfo.getCongestionLinkInfos().get(linkId).getTravelTimeLastLeavingAgent_sec() - freespeedTravelTime_sec;
					amount = -1.0 * delayLastAgent_sec * vtts_hour / 3600.;
					
				} else if (this.congestionInfo.getINTERNALIZATION_APPROACH().equals(InternalizationApproach.MaximumDelay)) {
					double maximumDelay = this.congestionInfo.getCongestionLinkInfos().get(linkId).getTravelTimeMaximum() - freespeedTravelTime_sec;
					amount = -1.0 * maximumDelay * vtts_hour / 3600.;
				
				} else {
					throw new RuntimeException("Unknown internalization approach. Aborting...");
				}
				
				if (amount > 0) {
					log.warn(amount);
					throw new RuntimeException("The money amount should be negative to be interpreted as a penalty. Check if the scoring parameters are right. Aborting...");
				}
				
				if (amount < 0) {
					for (Id<Vehicle> vehicleId : this.congestionInfo.getCongestionLinkInfos().get(linkId).getLeavingVehicles()) {
						this.eventsManager.processEvent(new PersonMoneyEvent(this.currentTime, this.congestionInfo.getVehicleId2personId().get(vehicleId), amount));
						this.totalTollPaymentsPerDay_monetaryUnits = this.totalTollPaymentsPerDay_monetaryUnits + (-1 * amount);
					}
				}
			}
			
		}
				
		if (isOutputIteration() && writeOutputForThisInterval) CongestionInfoWriter.writeCongestionInfoTimeInterval(this.congestionInfo, outputDirectory);
			
	}

	private void updateCurrentTimeInterval() {
		double newTimeInterval = this.congestionInfo.getCurrentTimeBinEndTime() + this.congestionInfo.getTIME_BIN_SIZE();
		this.congestionInfo.setCurrentTimeBinEndTime(newTimeInterval);
	}	
	
	private void resetCurrentTimeIntervalInfo() {
		
		for (Id<Link> linkId : this.congestionInfo.getCongestionLinkInfos().keySet()) {
			this.congestionInfo.getCongestionLinkInfos().get(linkId).getLeavingVehicles().clear();
			this.congestionInfo.getCongestionLinkInfos().get(linkId).setTravelTimeLastLeavingAgent(0.);
			this.congestionInfo.getCongestionLinkInfos().get(linkId).setTravelTimeSum(0.);;
		}		
	}
	
	public void computeFinalTimeIntervals() {
		
		while (this.congestionInfo.getCurrentTimeBinEndTime() <= 30 * 3600.) {
			processTimeBin();			
		}
	}
	
	private boolean isOutputIteration() {
		if (this.congestionInfo.getWRITE_OUTPUT_ITERATION() == 0) {
			return false;
		} else if (this.iteration % this.congestionInfo.getWRITE_OUTPUT_ITERATION() == 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		checkTime(event.getTime());		
	}
	
}
