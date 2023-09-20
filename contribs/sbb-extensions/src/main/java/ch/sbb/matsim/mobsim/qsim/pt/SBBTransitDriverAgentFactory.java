/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.mobsim.qsim.pt;

import java.util.Set;
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

}
