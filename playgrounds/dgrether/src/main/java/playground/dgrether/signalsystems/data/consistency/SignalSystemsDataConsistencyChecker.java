/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystems20ConsistencyChecker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.data.consistency;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;

import playground.dgrether.designdrafts.consistency.ConsistencyChecker;


/**
 * @author dgrether
 *
 */
public class SignalSystemsDataConsistencyChecker implements ConsistencyChecker {

	
	private static final Logger log = Logger.getLogger(SignalSystemsDataConsistencyChecker.class);
	
	private ScenarioImpl scenario;

	private SignalsData signalsData;

	private LaneDefinitions lanes;

	public SignalSystemsDataConsistencyChecker(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.signalsData = scenario.getScenarioElement(SignalsData.class);
		this.lanes = scenario.getLaneDefinitions();
	}
	
	/**
	 * @see playground.dgrether.designdrafts.consistency.ConsistencyChecker#checkConsistency()
	 */
	@Override
	public void checkConsistency() {
		if (this.signalsData == null) {
			log.error("No SignalsData instance found as ScenarioElement of Scenario instance!");
			log.error("Nothing to check, aborting!");
			return;
		}
		this.checkSignalToLinkMatching();
		this.checkSignalToLaneMatching();
	}

	private void checkSignalToLinkMatching() {
		Network net = this.scenario.getNetwork();
		SignalSystemsData signalSystems = this.signalsData.getSignalSystemsData();
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			for (SignalData signal : system.getSignalData().values()) {
				if (! net.getLinks().containsKey(signal.getLinkId())){
					log.error("Error: No Link for Signal: "); 
					log.error("\t\tSignalData Id: "  + signal.getId() + " of SignalSystemData Id: " + system.getId() 
							+ " is located at Link Id: " + signal.getLinkId() + " but this link is not existing in the network!");
				}
			}
		}
		
	}

	private void checkSignalToLaneMatching() {
		SignalSystemsData signalSystems = this.signalsData.getSignalSystemsData();
		
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			for (SignalData signal : system.getSignalData().values()) {
				if (signal.getLaneIds() != null || ! signal.getLaneIds().isEmpty()){
					if (! this.lanes.getLanesToLinkAssignments().containsKey(signal.getLinkId())){
						log.error("Error: No LanesToLinkAssignment for Signals:");
						log.error("\t\tSignalData Id: "  + signal.getId() + " of SignalSystemData Id: " + system.getId() 
							+ " is located at some lanes of Link Id: " + signal.getLinkId() + " but there is no LanesToLinkAssignemt existing in the LaneDefinitions.");
					}
					else {
						LanesToLinkAssignment l2l = this.lanes.getLanesToLinkAssignments().get(signal.getLinkId());
						for (Id laneId : signal.getLaneIds()) {
							if (! l2l.getLanes().containsKey(laneId)) {
								log.error("Error: No Lane for Signal: "); 
								log.error("\t\tSignalData Id: "  + signal.getId() + " of SignalSystemData Id: " + system.getId() 
										+ " is located at Link Id: " + signal.getLinkId() + " at Lane Id: " + laneId + " but this link is not existing in the network!");
							}
						}
					}
				}
			}
		}
	}

}
