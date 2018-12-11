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

/**
 *
 */
package org.matsim.contrib.av.robotaxi.fares.drt;

import org.apache.commons.lang.mutable.MutableDouble;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;

/**
 * @author jbischoff
 */
public class DrtFareHandlerTest {

    /**
     * Test method for {@link DrtFareHandler}.
     */
    @Test
    public void testDrtFareHandler() {

        Config config = ConfigUtils.createConfig();
        DrtFareConfigGroup tccg = new DrtFareConfigGroup();
        config.addModule(tccg);
        tccg.setBasefare(1);
        tccg.setDailySubscriptionFee(1);
        tccg.setDistanceFare_m(1.0 / 1000.0);
        tccg.setTimeFare_h(36);
        TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        config.addModule(taxiCfg);
        final MutableDouble fare = new MutableDouble(0);
        EventsManager events = EventsUtils.createEventsManager();
        DrtFareHandler tfh = new DrtFareHandler(tccg, events);
        events.addHandler(tfh);
        events.addHandler(new PersonMoneyEventHandler() {
            @Override
            public void handleEvent(PersonMoneyEvent event) {
                fare.add(event.getAmount());
            }

            @Override
            public void reset(int iteration) {
            }
        });
        Id<Person> p1 = Id.createPersonId("p1");

        events.processEvent(new PersonDepartureEvent(0.0, p1, Id.createLinkId("12"), taxiCfg.getMode()));
        events.processEvent(new DrtRequestSubmittedEvent(0.0, TransportMode.drt, Id.create(0, Request.class), p1, Id.createLinkId("12"), Id.createLinkId("23"), 240, 1000));
        events.processEvent(new PersonArrivalEvent(300.0, p1, Id.createLinkId("23"), taxiCfg.getMode()));

        //fare: 1 (daily fee) +2*1(basefare)+ 2*1 (distance) + (36/60)*2 = -(1+2+2+0,12) = -6.2
        Assert.assertEquals(-4.4, fare.getValue());
    }


}
