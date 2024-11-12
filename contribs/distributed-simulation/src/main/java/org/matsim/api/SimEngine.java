package org.matsim.api;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.dsim.messages.SimStepMessage;

public interface SimEngine extends Steppable {

    /**
     * @param person person to accept
     * @param now    current simulation time
     */
    void accept(MobsimAgent person, double now);

    void process(SimStepMessage stepMessage, double now);

    void setInternalInterface(InternalInterface internalInterface);
}
