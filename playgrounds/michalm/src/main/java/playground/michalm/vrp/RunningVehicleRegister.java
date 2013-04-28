/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.api.experimental.events.handler.*;
import org.matsim.core.events.handler.EventHandler;


public class RunningVehicleRegister
    implements EventHandler, AgentDepartureEventHandler, AgentStuckEventHandler,
    AgentArrivalEventHandler
{
    private Map<Id, AgentDepartureEvent> runningAgentsMap = new HashMap<Id, AgentDepartureEvent>();


    @Override
    public void handleEvent(AgentDepartureEvent event)
    {
        runningAgentsMap.put(event.getPersonId(), event);
    }


    @Override
    public void handleEvent(AgentArrivalEvent event)
    {
        runningAgentsMap.remove(event.getPersonId());
    }


    @Override
    public void handleEvent(AgentStuckEvent event)
    {
        //throw new RuntimeException();
        System.err.println("AgentStuckEvent:");
        System.err.println(event);
    }


    public Set<Id> getRunningAgentIds()
    {
        return runningAgentsMap.keySet();
    }


    @Override
    public void reset(int iteration)
    {
        runningAgentsMap.clear();
    }
}
