/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.signals.data.consistency;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.lanes.LanesConsistencyChecker;

/**
 * @author tthunig
 */
public class LanesAndSignalsCleaner {

    public void run(Scenario scenario){
        LanesConsistencyChecker lanesConsistency = new LanesConsistencyChecker(scenario.getNetwork(), scenario.getLanes());
        lanesConsistency.setRemoveMalformed(true);
        lanesConsistency.checkConsistency();

        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsDataConsistencyChecker signalsConsistency = new SignalSystemsDataConsistencyChecker(scenario.getNetwork(), scenario.getLanes(), signalsData);
        signalsConsistency.checkConsistency();
        SignalGroupsDataConsistencyChecker signalGroupsConsistency = new SignalGroupsDataConsistencyChecker(scenario);
        signalGroupsConsistency.checkConsistency();
        SignalControlDataConsistencyChecker signalControlConsistency = new SignalControlDataConsistencyChecker(scenario);
        signalControlConsistency.checkConsistency();
    }
}
