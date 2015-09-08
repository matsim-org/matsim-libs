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

package playground.agarwalamit.congestionPricing.GLKN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * Based on Laemmel2011Diss
 */

public class CongestionHandlerImplV7 implements PersonDepartureEventHandler,
LinkEnterEventHandler, LinkLeaveEventHandler, PersonStuckEventHandler, PersonArrivalEventHandler {

	public enum CongestionImpls {
		KN, // no documentation yet, see email
		GL; // Laemmel2011Diss
	}
	
	private final Logger log = Logger.getLogger(CongestionHandlerImplV7.class);

	public CongestionHandlerImplV7(EventsManager events, Scenario scenario, CongestionImpls congestionImpl){
		this.events = events;
		this.scenario = scenario;
		this.congestionImpl = congestionImpl;

		this.congestedModes.addAll(this.scenario.getConfig().qsim().getMainModes());
		if (congestedModes.size()>1) throw new RuntimeException("Mixed traffic is not tested yet.");

		storeLinkInfo();
	}

	private void storeLinkInfo(){
		for(Link l : this.scenario.getNetwork().getLinks().values()){
			LinkCongestionInfo lci = new LinkCongestionInfo();
			lci.setLinkId(l.getId());
			double flowCapacity_CapPeriod = l.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
			double marginalDelay_sec = ((1 / (flowCapacity_CapPeriod / this.scenario.getNetwork().getCapacityPeriod()) ) );
			lci.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);
			link2LinkCongestionInfo.put(l.getId(), lci);
		}
	}

	private EventsManager events ;
	private Scenario scenario;
	private final List<String> congestedModes = new ArrayList<String>();
	private Map<Id<Link>,LinkCongestionInfo> link2LinkCongestionInfo = new HashMap<Id<Link>, LinkCongestionInfo>();
	private double totalDelay = 0;
	private double totalInternalizedDelay = 0;
	private CongestionImpls congestionImpl ;

	@Override
	public void reset(int iteration) {
		link2LinkCongestionInfo.clear();
		storeLinkInfo();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects "
				+ "because there are no linkLeaveEvents for stucked agents.: \n" + event.toString());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		Id<Person> pId = Id.createPersonId(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();
		double linkLeaveTime = event.getTime();

		LinkCongestionInfo lci = link2LinkCongestionInfo.get(linkId);
		
		double freeSpeedLeaveTime = lci.getPersonId2freeSpeedLeaveTime().get(pId);

		double delay = linkLeaveTime - freeSpeedLeaveTime;

		if(delay > 0.){
			totalDelay += delay;
			lci.getFlowQueue().add(pId);

			List<Id<Person>> enteringAgentsList = new ArrayList<Id<Person>>(lci.getPersonId2linkEnterTime().keySet());
			
			boolean isThisLastAgentOnLink = enteringAgentsList.size() - 1 == 0 ? true :false;

			if( isThisLastAgentOnLink ) { // last agent on the link is delayed, thus, queue will dissolve immediately.

					throwCongestionEvents(event);	

			} else { // check for the headway i.e. if more agents will be queued, continue
				// nextFreeSpeedLinkLeaveTime - current leave time < timeHeadway ==> charge later.
				Id<Person> nextAgent = enteringAgentsList.get(1);
				double nextFreeSpeedLinkLeaveTime = lci.getPersonId2freeSpeedLeaveTime().get(nextAgent); 

				if( nextFreeSpeedLinkLeaveTime - linkLeaveTime >= lci.getMarginalDelayPerLeavingVehicle_sec() ){

					throwCongestionEvents(event);

				} else { //charge later
					
				}
			}
		} else {
			lci.getPersonId2freeSpeedLeaveTime().remove(pId);
		}

		lci.getPersonId2linkEnterTime().remove(pId);
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		LinkCongestionInfo lci = this.link2LinkCongestionInfo.get(event.getLinkId());

		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
		double minLinkTravelTime = Math.floor(link.getLength()/link.getFreespeed());

		lci.getPersonId2freeSpeedLeaveTime().put(personId, event.getTime()+ minLinkTravelTime + 1.0);
		lci.getPersonId2linkEnterTime().put(personId, event.getTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		String travelMode = event.getLegMode();
		if(congestedModes.contains(travelMode)){
			LinkCongestionInfo lci = this.link2LinkCongestionInfo.get(event.getLinkId());
			lci.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			lci.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
		}

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String travelMode = event.getLegMode();

		if(congestedModes.contains(travelMode)){
			LinkCongestionInfo lci = this.link2LinkCongestionInfo.get(event.getLinkId());
			lci.getPersonId2linkEnterTime().remove(event.getPersonId());
			lci.getPersonId2freeSpeedLeaveTime().remove(event.getPersonId());
		}

	}

	private void throwCongestionEvents(LinkLeaveEvent event) {
		LinkCongestionInfo lci = this.link2LinkCongestionInfo.get(event.getLinkId());
		List<Id<Person>> leavingAgents = new ArrayList<Id<Person>>(lci.getFlowQueue());
		Id<Person> nullAffectedAgent = Id.createPersonId("NullAgent");

		switch (congestionImpl) {
		case GL:
		{
			double queueDissolveTime = event.getTime() /*+ lci.getMarginalDelayPerLeavingVehicle_sec()*/; // not yet sure, if this is right.
			
			for(Id<Person> person : leavingAgents){
				double delayToPayFor = queueDissolveTime - lci.getPersonId2freeSpeedLeaveTime().get(person);
				this.totalInternalizedDelay += delayToPayFor;
				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "GL_Approach", person, nullAffectedAgent, delayToPayFor, event.getLinkId(), lci.getPersonId2linkEnterTime().get(person));
				this.events.processEvent(congestionEvent);
				lci.getPersonId2freeSpeedLeaveTime().remove(person);
			}

			lci.getFlowQueue().clear();
		}
			break;
		case KN:
		{
			int noOfDelayedAgents = leavingAgents.size();
			
			if(noOfDelayedAgents < 2) return; // can't calculate headway from one agent only
			int thisPesonDelayingOtherPersons = 0;
			
			for(int ii = noOfDelayedAgents-2; ii>=0;ii--){
				Id<Person> thisPerson = leavingAgents.get(ii);
				thisPesonDelayingOtherPersons++;

//				double headway =  lci.getPersonId2linkLeaveTime().get(leavingAgents.get(ii+1))  - lci.getPersonId2linkLeaveTime().get(thisPerson) ;
				double headway = lci.getLastLeaveEvent().getTime() - event.getTime() ;
				// ????
				
				
				double delayToPayFor = thisPesonDelayingOtherPersons * headway;
				
				this.totalInternalizedDelay += delayToPayFor;
				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "KN_Approach", thisPerson, nullAffectedAgent, delayToPayFor, event.getLinkId(), lci.getPersonId2linkEnterTime().get(thisPerson));
				this.events.processEvent(congestionEvent);
			}
		}
			break;
		default:
			throw new RuntimeException(congestionImpl+" is not known. Aborting...");
		}
	}

	public double getTotalDelay() {
		return totalDelay;
	}

	public double getTotalInternalizedDelay() {
		return totalInternalizedDelay;
	}

}
