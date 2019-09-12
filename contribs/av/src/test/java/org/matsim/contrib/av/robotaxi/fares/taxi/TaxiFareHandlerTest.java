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
package org.matsim.contrib.av.robotaxi.fares.taxi;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
public class TaxiFareHandlerTest {

	/**
     * Test method for {@link TaxiFareHandler}.
	 */
	@Test
	public void testTaxiFareHandler() {
		Network network = createNetwork();
		Config config = ConfigUtils.createConfig();
		TaxiFareConfigGroup tccg = new TaxiFareConfigGroup();
		config.addModule(tccg);
		tccg.setBasefare(1);
        tccg.setMinFarePerTrip(1.5);
		tccg.setDailySubscriptionFee(1);
		tccg.setDistanceFare_m(1.0/1000.0);
		tccg.setTimeFare_h(36);
		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		MultiModeTaxiConfigGroup multiTaxiCfg = new MultiModeTaxiConfigGroup();
		multiTaxiCfg.addParameterSet(taxiCfg);
		config.addModule(multiTaxiCfg);
		final MutableDouble fare = new MutableDouble(0);
		EventsManager events = EventsUtils.createEventsManager();
        TaxiFareHandler tfh = new TaxiFareHandler(tccg, network, events);
		events.addHandler(tfh);
		events.addHandler(new PersonMoneyEventHandler() {

			@Override
			public void handleEvent(PersonMoneyEvent event) {
				fare.add(event.getAmount());

			}

			@Override
			public void reset(int iteration) {}
		});
		Id<Person> p1 = Id.createPersonId("p1");
		Id<Vehicle> t1 = Id.createVehicleId("v1");
		events.processEvent(new PersonDepartureEvent(0.0, p1 , Id.createLinkId("12"), taxiCfg.getMode()));
		events.processEvent(new PersonEntersVehicleEvent(60.0, p1 , t1));
		events.processEvent(new LinkEnterEvent(61,t1,Id.createLinkId("23")));
		events.processEvent(new PersonArrivalEvent(120.0, p1, Id.createLinkId("23"), taxiCfg.getMode()));

		events.processEvent(new PersonDepartureEvent(180.0, p1 , Id.createLinkId("12"), taxiCfg.getMode()));
		events.processEvent(new PersonEntersVehicleEvent(240.0, p1 , t1));
		events.processEvent(new LinkEnterEvent(241,t1,Id.createLinkId("23")));
		events.processEvent(new PersonArrivalEvent(300.0, p1, Id.createLinkId("23"), taxiCfg.getMode()));

		//fare: 1 (daily fee) +2*1(basefare)+ 2*1 (distance) + (36/60)*2 = -(1+2+2+0,12) = -6.2
		Assert.assertEquals(-6.2, fare.getValue(), 0);

		// test minFarePerTrip
		events.processEvent(new PersonDepartureEvent(360.0, p1 , Id.createLinkId("23"), taxiCfg.getMode()));
		events.processEvent(new PersonEntersVehicleEvent(400.0, p1 , t1));
		events.processEvent(new LinkEnterEvent(401,t1,Id.createLinkId("34")));
		events.processEvent(new PersonArrivalEvent(410.0, p1, Id.createLinkId("34"), taxiCfg.getMode()));

		/*
         * fare new trip: 0 (daily fee already paid) + 0.1 (distance)+ 1 basefare + 0.1 (time) = 1.2 < minFarePerTrip = 1.5
         * --> new total fare: 6.2 (previous trip) + 1.5 (minFarePerTrip for new trip) = 7.7
         */
		Assert.assertEquals(-7.7, fare.getValue(), 0);
	}

	private Network createNetwork(){
		Network network = NetworkUtils.createNetwork();
		Node n1 = NetworkUtils.createNode( Id.createNodeId(1), new Coord(0,0));
		Node n2 = NetworkUtils.createNode(Id.createNodeId(2), new Coord(2000,0));
		Node n3 = NetworkUtils.createNode(Id.createNodeId(3), new Coord(2000,2000));
		Node n4 = NetworkUtils.createNode(Id.createNodeId(4), new Coord(2100,2000));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		NetworkUtils.createAndAddLink(network, Id.createLinkId(12), n1, n2, 1000.0, 100 , 100 , 1 , "1", "");
		NetworkUtils.createAndAddLink(network, Id.createLinkId(23), n2, n3, 1000.0, 100 , 100 , 1 , "1", "");
		NetworkUtils.createAndAddLink(network, Id.createLinkId(34), n3, n4, 100.0, 100 , 100 , 1 , "1", "");
		return network;

	}

}
