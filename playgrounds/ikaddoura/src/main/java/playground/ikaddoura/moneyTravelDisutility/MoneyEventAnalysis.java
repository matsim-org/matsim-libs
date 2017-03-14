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

package playground.ikaddoura.moneyTravelDisutility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.moneyTravelDisutility.data.LinkInfo;
import playground.ikaddoura.moneyTravelDisutility.data.TimeBin;


/**
 * Analyzes link-, time- and vehicle-specific information about monetary payments.
 * Computes the average monetary payment per link, time and vehicle type which is required by the travel disutility computation.
 * 
 * @author ikaddoura
 */

public class MoneyEventAnalysis implements PersonLinkMoneyEventHandler, LinkEnterEventHandler, IterationEndsListener, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(MoneyEventAnalysis.class);
	
	@Inject
	private Scenario scenario;
	
	@Inject(optional=true)
	private AgentFilter agentFilter;
	
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2driverIdToBeCharged = new HashMap<>();
	private final Map<Id<Link>, LinkInfo> linkId2info = new HashMap<>();
	
	private final boolean tollMSA = true;
	
	private int warnCounter2 = 0;
	private int warnCounter3 = 0;
	private int warnCounter4 = 0;
	
	private int tollUpdateCounter = 0;

	@Override
	public void reset(int iteration) {
		
		log.info("Resetting money event analysis information before the mobsim starts."
				+ "The average monetary payments are not set to zero in case they are required during the simulation.");
		
		this.vehicleId2driverIdToBeCharged.clear();
		
		for (LinkInfo linkInfo : this.linkId2info.values()) {
			for (TimeBin timeBin : linkInfo.getTimeBinNr2timeBin().values()) {
				timeBin.getEnteringAgents().clear();
				timeBin.getPersonId2amounts().clear();
			}
		}
		
		warnCounter2 = 0;
		warnCounter3 = 0;
	}

	@Override
	public void handleEvent(PersonLinkMoneyEvent event) {

		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = event.getPersonId();

		int timeBinNr = getIntervalNr(event.getRelevantTime());

		LinkInfo linkMoneyInfo = linkId2info.get(linkId);
		if (linkMoneyInfo != null) {

			TimeBin timeBin = linkMoneyInfo.getTimeBinNr2timeBin().get(timeBinNr);
			if (timeBin != null) {
				
				List<Double> amounts = timeBin.getPersonId2amounts().get(personId);
				
				if (amounts != null) {
					timeBin.getPersonId2amounts().get(personId).add(event.getAmount());
				} else {
					amounts = new ArrayList<>();
					amounts.add(event.getAmount());
					timeBin.getPersonId2amounts().put(personId, amounts);
				}
				
			} else {
				
				timeBin = new TimeBin(timeBinNr);
				List<Double> amounts = new ArrayList<>();
				amounts.add(event.getAmount());
				timeBin.getPersonId2amounts().put(personId, amounts);
				
				linkMoneyInfo.getTimeBinNr2timeBin().put(timeBinNr, timeBin);
			}
			
		} else {
			
			TimeBin timeBin = new TimeBin(timeBinNr);
			List<Double> amounts = new ArrayList<>();
			amounts.add(event.getAmount());
			timeBin.getPersonId2amounts().put(personId, amounts);

			linkMoneyInfo = new LinkInfo(linkId);
			linkMoneyInfo.getTimeBinNr2timeBin().put(timeBinNr, timeBin);
			linkId2info.put(linkId, linkMoneyInfo);
		}		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
				
		int timeBinNr = getIntervalNr(event.getTime());

		Id<Link> linkId = event.getLinkId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Person> personId = this.vehicleId2driverIdToBeCharged.get(vehicleId);
		
		if (linkId2info.get(linkId) != null) {
			LinkInfo linkMoneyInfo = linkId2info.get(linkId);
			
			if (linkMoneyInfo.getTimeBinNr2timeBin().get(timeBinNr) != null) {
				TimeBin timeBin = linkMoneyInfo.getTimeBinNr2timeBin().get(timeBinNr);
				timeBin.getEnteringAgents().add(personId);
				
			} else {
				
				TimeBin timeBin = new TimeBin(timeBinNr);
				timeBin.getEnteringAgents().add(personId);
				linkMoneyInfo.getTimeBinNr2timeBin().put(timeBinNr, timeBin);
			}
			
		} else {
			
			TimeBin timeBin = new TimeBin(timeBinNr);
			timeBin.getEnteringAgents().add(personId);
			
			LinkInfo linkMoneyInfo = new LinkInfo(linkId);
			linkMoneyInfo.getTimeBinNr2timeBin().put(timeBinNr, timeBin);
			linkId2info.put(linkId, linkMoneyInfo);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		// a vehicle may have several passengers (transit, taxi, ...) but we are interested in the driver to be charged. In the case of AV it is an imaginary vehicle driver (robot)...
		
		if (this.vehicleId2driverIdToBeCharged.containsKey(event.getVehicleId()) && (!event.getPersonId().toString().equals(event.getPersonId().toString()))) {
			if (warnCounter2 <= 5) {
				log.warn(event.getPersonId() + " enters vehicle " + event.getVehicleId() + ". Person " + this.vehicleId2driverIdToBeCharged.get(event.getVehicleId()) + " has entered the vehicle before"
						+ " and is considered as the transit / taxi driver.");
				if (warnCounter4 == 5) {
					log.warn("Further log statements of this type are not printed out.");
				}
			}
		} else {
			this.vehicleId2driverIdToBeCharged.put(event.getVehicleId(), event.getPersonId());
		}	
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		log.info("+++++ Iteration ends. Processing the data...");
		
		for (LinkInfo linkInfo : this.linkId2info.values()) {
			
			for (TimeBin timeBin : linkInfo.getTimeBinNr2timeBin().values()) {
			
				// storing the average from the previous iteration
				final double averageAmountPreviousIteration = timeBin.getAverageAmount();
				
				// resetting average from previous iteration
				timeBin.setAverageAmount(0.);
				
				// compute average for the current iteration				
				double averageAmount = computeAverageAmount(timeBin);
				
				double smoothenedAverageAmount;
				if (tollMSA) {
					// smoothening the average amount
					if (tollUpdateCounter > 0) {
						double blendFactor = 1.0 / (double) this.tollUpdateCounter;
						smoothenedAverageAmount = averageAmount * blendFactor + averageAmountPreviousIteration * (1 - blendFactor);
					} else {
						smoothenedAverageAmount = averageAmount;
					}
				} else {
					smoothenedAverageAmount = averageAmount;
				}
				
				// set smoothened average
				timeBin.setAverageAmount(smoothenedAverageAmount);
				
				// average amount per agent type
				if (this.agentFilter != null) {
					
					// storing the average from the previous iteration
					final Map<String, Double> agentTypeId2avgAmountPreviousIteration = new HashMap<>();
					for (String agentType : timeBin.getAgentTypeId2avgAmount().keySet()) {
						agentTypeId2avgAmountPreviousIteration.put(agentType, timeBin.getAgentTypeId2avgAmount().get(agentType));
					}
					
					// resetting averages from previous iteration
					timeBin.getAgentTypeId2avgAmount().clear();
					
					// compute averages for the current iteration and set smoothened averages
					computeAverageAmountPerAgentType(timeBin, agentTypeId2avgAmountPreviousIteration);
				}
			}
		}
		log.info("+++++ Iteration ends. Processing the data... Done.");
		tollUpdateCounter++;
	}

	private void computeAverageAmountPerAgentType(TimeBin timeBin, Map<String, Double> agentTypeId2avgAmountPreviousIteration) {
		final Map<String, Double> agentTypeIdPrefix2AmountSum = new HashMap<>();
		final Map<String, Integer> agentTypeIdPrefix2Counter = new HashMap<>();
				
		for (Id<Person> personId : timeBin.getPersonId2amounts().keySet()) {
			double totalAmountOfPerson = 0.;
			for (Double amount : timeBin.getPersonId2amounts().get(personId)) {
				totalAmountOfPerson += amount;
			}

			String agentType = this.agentFilter.getAgentTypeFromId(personId);	
			Double amountSum = agentTypeIdPrefix2AmountSum.get(agentType);
			
			if (amountSum != null) {
				agentTypeIdPrefix2AmountSum.put(agentType, amountSum + totalAmountOfPerson);

			} else {
				agentTypeIdPrefix2AmountSum.put(agentType, totalAmountOfPerson);
			}
		}
		
		for (Id<Person> personId : timeBin.getEnteringAgents()) {

			String agentType = this.agentFilter.getAgentTypeFromId(personId);
			Integer counter = agentTypeIdPrefix2Counter.get(agentType);
			
			if (counter != null) {
				agentTypeIdPrefix2Counter.put(agentType, counter + 1);

			} else {
				agentTypeIdPrefix2Counter.put(agentType, 1);
			}
		}
		
		for (String agentType : agentTypeIdPrefix2AmountSum.keySet()) {
			
			if (agentTypeIdPrefix2Counter.get(agentType) == null) {
				
				if (warnCounter2 <= 5) {
					log.warn("No entering agent of type " + agentType + " in time bin " + timeBin.getTimeBinNr() + " even though there are person money events (total monetary amounts: " + agentTypeIdPrefix2AmountSum.get(agentType) + ")."
							+ " This happens if road segments are very long. Can't compute the average amount for that time bin and road segment.");
					warnCounter2++;
					if (warnCounter2 == 5) {
						log.warn("Further log statements of this type are not printed out.");
					}
				}
				
			} else {
				double amountSum = agentTypeIdPrefix2AmountSum.get(agentType);
				double counter = agentTypeIdPrefix2Counter.get(agentType);
				double averageAmount = amountSum / counter;
				
				double smoothenedAmount;
				if (tollMSA) {
					// smoothening the average amount
					if (tollUpdateCounter > 0) {
						double blendFactor = 1.0 / (double) this.tollUpdateCounter;
						
						double avgAmountPreviousIteration = 0.;
						if (agentTypeId2avgAmountPreviousIteration.get(agentType) != null) {
							avgAmountPreviousIteration = agentTypeId2avgAmountPreviousIteration.get(agentType);
						}
						
						smoothenedAmount = averageAmount * blendFactor + avgAmountPreviousIteration * (1 - blendFactor);
					} else {
						smoothenedAmount = averageAmount;
					}
				} else {
					smoothenedAmount = averageAmount;
				}

				smoothenedAmount = averageAmount;
				
				if (smoothenedAmount != 0.) timeBin.getAgentTypeId2avgAmount().put(agentType, smoothenedAmount);
			}
		}
	}

	private double computeAverageAmount(TimeBin timeBin) {
		double sum = 0.;

		for (Id<Person> personId : timeBin.getPersonId2amounts().keySet()) {
			for (Double amount : timeBin.getPersonId2amounts().get(personId)) {
				sum += amount;
			}
		}
		
		double average = 0.;
		if (timeBin.getEnteringAgents().size() == 0) {
			if (warnCounter3 <= 5) {
				log.warn("No entering agent in time bin " + timeBin.getTimeBinNr() + " even though there are person money events (total monetary amounts: " + sum + ")."
						+ " Can't compute the average amount per time bin.");
				warnCounter3++;
				if (warnCounter3 == 5) {
					log.warn("Further log statements of this type are not printed out.");
				}
			}

		} else {
			average = sum / timeBin.getEnteringAgents().size();
		}
		return average;
	}
	
	private int getIntervalNr(double time) {
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();		
		return (int) (time / timeBinSize);
	}

	public Map<Id<Link>, LinkInfo> getLinkId2info() {
		return linkId2info;
	}

}

