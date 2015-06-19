/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupsDataConsistencyChecker
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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;

import playground.dgrether.designdrafts.consistency.ConsistencyChecker;


/**
 * @author dgrether
 *
 */
public class SignalGroupsDataConsistencyChecker implements ConsistencyChecker {

	
	private static final Logger log = Logger.getLogger(SignalGroupsDataConsistencyChecker.class);
	
	private SignalsData signalsData;

	public SignalGroupsDataConsistencyChecker(Scenario scenario) {
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

	}
	
	/**
	 * @see playground.dgrether.designdrafts.consistency.ConsistencyChecker#checkConsistency()
	 */
	@Override
	public void checkConsistency() {
		log.info("Checking consistency of SignalGroupsData...");
		this.checkGroupToSignalsMatching();
		log.info("Checked consistency of SignalGroupsData.");
	}

	private void checkGroupToSignalsMatching() {
		SignalGroupsData groups = this.signalsData.getSignalGroupsData();
		SignalSystemsData signals = this.signalsData.getSignalSystemsData();
		for (Map<Id<SignalGroup>, SignalGroupData> groupByIdMap : groups.getSignalGroupDataBySignalSystemId().values()) {
			for (SignalGroupData group : groupByIdMap.values()) {
				for (Id<Signal> signalId : group.getSignalIds()) {
					SignalSystemData signalSystem = signals.getSignalSystemData().get(group.getSignalSystemId());
					if (signalSystem == null) {
						log.error("Error: No SignalSystem for SignalGroup." );
						log.error("\t\tSignalGroup Id: " + group.getId() + " is specified to be in SignalSystem Id: " + group.getSignalSystemId() + " but "
								+ " this signal system is not existing in the specification of the signal systems.");
						
					}
					else if (! signalSystem.getSignalData().containsKey(signalId)){
							log.error("Error: No Signal for SignalGroup." );
							log.error("\t\tSignalGroup Id: " + group.getId() + " of SignalSystem Id: " + group.getSignalSystemId() + " points"
									+ " to  Signal Id: " + signalId + " but this signal is not specified. ");
						}
					}
				}
			}
		}
		
		
	}

