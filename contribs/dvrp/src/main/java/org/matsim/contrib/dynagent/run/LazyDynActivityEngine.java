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

package org.matsim.contrib.dynagent.run;

import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;


/**
 * It might be nicer to have ActivityEngine as a delegate, not as the superclass. But there is a
 * hardcoded "instanceof ActivityEngine" check in QSim :-( TODO introduce an ActivityEngine
 * interface?
 */
public class LazyDynActivityEngine
    extends ActivityEngine
{
    private InternalInterface internalInterface;


    public LazyDynActivityEngine(EventsManager eventsManager, AgentCounter agentCounter)
    {
        super(eventsManager, agentCounter);
    }


    public void rescheduleDynActivity(DynAgent dynAgent)
    {
        internalInterface.rescheduleActivityEnd(dynAgent);
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
        super.setInternalInterface(internalInterface);
    }
}
