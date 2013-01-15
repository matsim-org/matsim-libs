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

package playground.michalm.vrp.data.network.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;


public class TravelTimeCalculators
{
    public static TravelTime createTravelTimeFromEvents(String eventFileName,
            Scenario scenario)
    {
        TravelTimeCalculator ttimeCalc = new TravelTimeCalculatorFactoryImpl()
                .createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig()
                        .travelTimeCalculator());

        EventsManager inputEvents = EventsUtils.createEventsManager();
        inputEvents.addHandler(ttimeCalc);
        new EventsReaderXMLv1(inputEvents).parse(eventFileName);

        return ttimeCalc.getLinkTravelTimes();
    }
}
