/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.bvgAna.anaLevel1.personId2DelayAtStop;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverPrefixAnalyzer;

/**
 * Collects the <code>AgentDepartureEventHandler</code> and the corresponding <code>PersonEntersVehicleEventHandler</code>.
 *
 * @author ikaddoura, aneumann
 *
 */
public class PersonId2DelayAtStopHandler implements AgentDepartureEventHandler, PersonEntersVehicleEventHandler{
	private final Logger log = Logger.getLogger(PersonId2DelayAtStopHandler.class);
	private PtDriverPrefixAnalyzer ptDriverPrefixAnalyzer;
	
	private TreeMap<Id, PersonId2DelayAtStopData> id2DelayAtStopMap = new TreeMap<Id, PersonId2DelayAtStopData>();
	
	public PersonId2DelayAtStopHandler(PtDriverPrefixAnalyzer ptDriverPrefixAnalyzer){
		this.ptDriverPrefixAnalyzer = ptDriverPrefixAnalyzer;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {

		if(event.getPersonId().toString().startsWith(ptDriverPrefixAnalyzer.getPtDriverPrefix())){
			// pt driver
		} else {

			if(event.getLegMode() == TransportMode.pt){

				if(this.id2DelayAtStopMap.get(event.getPersonId()) == null){
//					this.log.debug("Adding new AgentDelayAtStopContainer for agent " + event.getPersonId() + " to map.");
					this.id2DelayAtStopMap.put(event.getPersonId(), new PersonId2DelayAtStopData(event.getPersonId()));
				}

				this.id2DelayAtStopMap.get(event.getPersonId()).addAgentDepartureEvent(event);
			} else {
				// no pt
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.id2DelayAtStopMap.containsKey(event.getPersonId())){
				this.id2DelayAtStopMap.get(event.getPersonId()).addPersonEntersVehicleEvent(event);
		}
	}

	/**
	 * Returns departure and enter vehicle time information.
	 *
	 * @return A map containing a <code>AgentDelayAtStopContainer</code> for each agent id
	 */
	public TreeMap<Id, PersonId2DelayAtStopData> getPersonId2DelayAtStopMap() {
		return this.id2DelayAtStopMap;
	}
	
}
