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
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.ikaddoura.moneyTravelDisutility.data.LinkInfo;
import playground.ikaddoura.moneyTravelDisutility.data.TimeBin;


/**
 * Analyzes link-, time- and vehicle-specific information about monetary payments.
 * Computes the average monetary payment per link, time and vehicle type which is required by the travel disutility computation.
 * 
 * @author ikaddoura
 */

public class MoneyEventAnalysis implements PersonMoneyEventHandler, LinkEnterEventHandler, IterationEndsListener {
	private static final Logger log = Logger.getLogger(MoneyEventAnalysis.class);
	
	@Inject
	private Scenario scenario;
		
	private final Map<Id<Vehicle>, Id<Link>> vehicleId2linkId = new HashMap<>();
	private final Map<Id<Person>, Id<Vehicle>> personId2vehicleId = new HashMap<>();
	private final Map<Id<Link>, LinkInfo> linkId2info = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		
		log.info("Resetting money event analysis information before the mobsim starts.");
		
		this.personId2vehicleId.clear();
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

	private int getIntervalNr(double time) {
		
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		int timeBinNr = (int) (time / timeBinSize);
		
		return timeBinNr;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.vehicleId2linkId.put(event.getVehicleId(), event.getLinkId());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		log.info("Iteration ends. Processing the data in the previous iteration...");
		for (LinkInfo info : this.linkId2info.values()) {

			info.computeAverageAmount();
			info.computeAverageAmountPerAgentType();
		}
		log.info("Iteration ends. Processing the data in the previous iteration... Done.");
	}

	public Map<Id<Link>, LinkInfo> getLinkId2info() {
		return linkId2info;
	}
}

