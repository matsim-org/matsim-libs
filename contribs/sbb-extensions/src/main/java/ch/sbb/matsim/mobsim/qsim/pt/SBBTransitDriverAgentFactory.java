/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.mobsim.qsim.pt;

import java.util.Set;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;

/**
 * @author mrieser / SBB
 */
public class SBBTransitDriverAgentFactory implements TransitDriverAgentFactory {

    private final Set<String> deterministicModes;

	@Inject
	SBBTransitDriverAgentFactory(SBBTransitConfigGroup config) {
		deterministicModes = config.getDeterministicServiceModes();
	}

    SBBTransitDriverAgentFactory(Set<String> deterministicModes) {
        this.deterministicModes = deterministicModes;
    }

    @Override
    public AbstractTransitDriverAgent createTransitDriver(Umlauf umlauf, InternalInterface internalInterface, TransitStopAgentTracker transitStopAgentTracker) {
        String mode = umlauf.getUmlaufStuecke().get(0).getRoute().getTransportMode();
        if (this.deterministicModes.contains(mode)) {
            return new SBBTransitDriverAgent(umlauf, mode,transitStopAgentTracker, internalInterface);
        }
        return new TransitDriverAgentImpl(umlauf, TransportMode.car, transitStopAgentTracker, internalInterface);
    }

	@Override
	public AbstractTransitDriverAgent createTransitDriverFromMessage(Message message, Umlauf umlauf, InternalInterface internalInterface, TransitStopAgentTracker transitStopAgentTracker){

		var mode = umlauf.getUmlaufStuecke().getFirst().getRoute().getTransportMode();
		if (this.deterministicModes.contains(mode) && message instanceof SBBTransitDriverAgent.SBBTransitDriverMessage stdm) {
			return new SBBTransitDriverAgent(stdm, umlauf, mode, transitStopAgentTracker, internalInterface);
		} else if (message instanceof TransitDriverAgentImpl.TransitDriverMessage tdm) {
			return new TransitDriverAgentImpl(tdm, umlauf, TransportMode.car, transitStopAgentTracker, internalInterface);
		} else {
			throw new IllegalArgumentException("SBBTransitDriverAgentFactory only supports SBBTransitDriverAgent and TransitDriverAgentImpl messages but was passed: " + message.getClass().getName());
		}
	}

}
