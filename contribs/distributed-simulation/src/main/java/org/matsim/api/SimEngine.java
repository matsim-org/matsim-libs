package org.matsim.api;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.simulation.SimPerson;

public interface SimEngine extends Steppable {

    /**
     * @param person person to accept
     * @param now    current simulation time
     */
    void accept(SimPerson person, double now);

    void process(SimStepMessage stepMessage, double now);

    void setNextStateHandler(NextStateHandler nextStateHandler);
}
