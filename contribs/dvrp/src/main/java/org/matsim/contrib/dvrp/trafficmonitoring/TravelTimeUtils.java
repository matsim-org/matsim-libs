/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.*;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class TravelTimeUtils
{
    public static TravelTime createTravelTimesFromEvents(Scenario scenario, String eventsFile)
    {
        TravelTimeCalculator ttCalculator = TravelTimeCalculator.create(scenario.getNetwork(),
                scenario.getConfig().travelTimeCalculator());
        initTravelTimeCalculatorFromEvents(ttCalculator, eventsFile);
        return ttCalculator.getLinkTravelTimes();
    }


    public static void initTravelTimeCalculatorFromEvents(TravelTimeCalculator ttCalculator,
            String eventsFile)
    {
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(ttCalculator);
        new MatsimEventsReader(events).readFile(eventsFile);
    }
}
