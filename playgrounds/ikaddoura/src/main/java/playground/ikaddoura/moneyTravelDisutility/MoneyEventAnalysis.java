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
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
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

public class MoneyEventAnalysis implements PersonMoneyEventHandler, LinkEnterEventHandler, IterationEndsListener, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(MoneyEventAnalysis.class);
	
	@Inject
	private Scenario scenario;
	
	@Inject(optional=true)
	private AgentFilter agentFilter;
	
	private final Map<Id<Vehicle>, Id<Link>> vehicleId2linkId = new HashMap<>();
	private final Map<Id<Person>, Id<Vehicle>> personId2vehicleId = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();
	private final Map<Id<Link>, LinkInfo> linkId2info = new HashMap<>();

	// only for testing purposes
	protected MoneyEventAnalysis(Scenario scenario) {
		this.scenario = scenario;
	}
	
	// required
	public MoneyEventAnalysis() {
	
	}

	@Override
	public void reset(int iteration) {
		
		log.info("Resetting money event analysis information before the mobsim starts.");
		
		this.personId2vehicleId.clear();
		this.vehicleId2personId.clear();
		this.vehicleId2linkId.clear();
		this.linkId2info.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		Id<Person> personId = event.getPersonId();
		Id<Vehicle> vehicleId = this.personId2vehicleId.get(personId);
		Id<Link> linkId = vehicleId2linkId.get(vehicleId);
				
		int timeBinNr = getIntervalNr(event.getTime());
		
		if (linkId2info.containsKey(linkId)) {
			LinkInfo linkMoneyInfo = linkId2info.get(linkId);
			
			if (linkMoneyInfo.getTimeBinNr2timeBin().containsKey(timeBinNr)) {
				TimeBin timeBin = linkMoneyInfo.getTimeBinNr2timeBin().get(timeBinNr);
				
				if (timeBin.getPersonId2amounts().containsKey(vehicleId)) {
					timeBin.getPersonId2amounts().get(vehicleId).add(event.getAmount());
				} else {
					List<Double> amounts = new ArrayList<>();
					amounts.add(event.getAmount());
					timeBin.getPersonId2amounts().put(personId, amounts);
				}
				
			} else {
				
				TimeBin timeBin = new TimeBin(timeBinNr);
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

			LinkInfo linkMoneyInfo = new LinkInfo(linkId);
			linkMoneyInfo.getTimeBinNr2timeBin().put(timeBinNr, timeBin);
			linkId2info.put(linkId, linkMoneyInfo);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		this.vehicleId2linkId.put(event.getVehicleId(), event.getLinkId());
		
		int timeBinNr = getIntervalNr(event.getTime());
		
		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Person> personId = this.vehicleId2personId.get(vehicleId);
		Id<Link> linkId = vehicleId2linkId.get(vehicleId);
		
		if (linkId2info.containsKey(linkId)) {
			LinkInfo linkMoneyInfo = linkId2info.get(linkId);
			
			if (linkMoneyInfo.getTimeBinNr2timeBin().containsKey(timeBinNr)) {
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
		this.personId2vehicleId.put(event.getPersonId(), event.getVehicleId());
		this.vehicleId2personId.put(event.getVehicleId(), event.getPersonId());
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		log.info("Iteration ends. Processing the data in the previous iteration...");
		for (LinkInfo linkInfo : this.linkId2info.values()) {
			
			for (TimeBin timeBin : linkInfo.getTimeBinNr2timeBin().values()) {
			
				// average amount
				double averageAmount = computeAverageAmount(timeBin);
				timeBin.setAverageAmount(averageAmount);
				
				// average amount per agent type
				if (this.agentFilter != null) computeAverageAmountPerAgentType(timeBin);
			}
		}
		log.info("Iteration ends. Processing the data in the previous iteration... Done.");
	}

	private void computeAverageAmountPerAgentType(TimeBin timeBin) {
		final Map<String, Double> agentTypeIdPrefix2AmountSum = new HashMap<>();
		final Map<String, Integer> agentTypeIdPrefix2Counter = new HashMap<>();
				
		for (Id<Person> personId : timeBin.getPersonId2amounts().keySet()) {
			double totalAmountOfPerson = 0.;
			for (Double amount : timeBin.getPersonId2amounts().get(personId)) {
				totalAmountOfPerson += amount;
			}

			String agentType = this.agentFilter.getAgentTypeFromId(personId);
			
			if (agentTypeIdPrefix2AmountSum.containsKey(agentType)) {
				double amountSum = agentTypeIdPrefix2AmountSum.get(agentType);
				agentTypeIdPrefix2AmountSum.put(agentType, amountSum + totalAmountOfPerson);

			} else {
				agentTypeIdPrefix2AmountSum.put(agentType, totalAmountOfPerson);
			}
		}
		
		for (Id<Person> personId : timeBin.getEnteringAgents()) {

			String agentType = this.agentFilter.getAgentTypeFromId(personId);
			
			if (agentTypeIdPrefix2Counter.containsKey(agentType)) {
				int counter = agentTypeIdPrefix2Counter.get(agentType);
				agentTypeIdPrefix2Counter.put(agentType, counter + 1);

			} else {
				agentTypeIdPrefix2Counter.put(agentType, 1);
			}
		}
		
		for (String agentType : agentTypeIdPrefix2AmountSum.keySet()) {
			timeBin.getAgentTypeId2avgAmount().put(agentType, agentTypeIdPrefix2AmountSum.get(agentType) / agentTypeIdPrefix2Counter.get(agentType) );
		}
	}

	private double computeAverageAmount(TimeBin timeBin) {
		double sum = 0.;

		for (Id<Person> personId : timeBin.getPersonId2amounts().keySet()) {
			System.out.println(personId);
			for (Double amount : timeBin.getPersonId2amounts().get(personId)) {
				sum += amount;
			}
		}		
		return sum / timeBin.getEnteringAgents().size();
	}
	
	private int getIntervalNr(double time) {
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();		
		return (int) (time / timeBinSize);
	}

	public Map<Id<Link>, LinkInfo> getLinkId2info() {
		return linkId2info;
	}
}

