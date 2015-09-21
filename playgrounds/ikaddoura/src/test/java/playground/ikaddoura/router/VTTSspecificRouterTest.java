/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.router;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import com.vividsolutions.jts.util.Assert;

import playground.ikaddoura.analysis.vtts.VTTSHandler;

/**
 * 
 * Tests a router which accounts for the agent's real VTTS.
 * The VTTS is computed by {@link VTTSHandler}. 
 * 
 * @author ikaddoura
 *
 */

public class VTTSspecificRouterTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * There are two agents. One agent is more pressed for time than the other one which results in different VTTS.
	 * There are two routes. One expensive but fast route. One cheap but slow route.
	 * Now see which route is chosen by which agent.
	 * 
	 */
	@Test
	public final void test1(){
		
		// starts the VTTS-specific router
		
		final String configFile1 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS.xml";
		final Controler controler = new Controler(configFile1);
		final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
		final VTTSTravelTimeAndDistanceBasedTravelDisutilityFactory factory = new VTTSTravelTimeAndDistanceBasedTravelDisutilityFactory(vttsHandler) ;
		factory.setSigma(0.); // no randomness
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindTravelDisutilityFactory().toInstance( factory );
			}
		}); 		
		
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();

		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(vttsHandler);
				event.getControler().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// high VTTS person's VTTS: 17.21 EUR/h (this is a value provided by the VTTSHandler)
		// low VTTS person's VTTS: 10.37 EUR/h (this is a value provided by the VTTSHandler)
		
		// high speed and costly route: 15 km; 50 km/h (travel time: 0.3h)
		// high VTTS person: 17.21 EUR/km * 0.3 km + 15 km * 0.002 EUR/m --> 35.16 EUR
		// low VTTS person: 10.37 * 0.3 + 15 * 2 --> 33.11 EUR		
		
		// low speed and cheap route: 10 km; 10 km/h (travel time: 1h)
		// high VTTS person: 17.21 * 1 + 10 * 2 --> 37.21 EUR
		// low VTTS person: 10.37 * 1 + 10 * 2 --> 30.37 EUR
		
		// ==> the high VTTS person will choose the high speed but expensive route (35.16 < 37.21)
		// ==> the low VTTS person will choose the low speed but cheap route (30.37 < 33.11)
		
		Id<Link> longDistanceShortTimeLinkId = Id.createLinkId("link_1_2");
		Id<Link> shortDistanceLongTimeLinkId = Id.createLinkId("link_3_6");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// the high VTTS person should use the fast but expensive route
				Assert.equals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
			
			if (id.toString().contains("lowVTTS")) {

				// the low VTTS person should use the slow but cheap route
				Assert.equals(true, vehicleId2linkIds.get(id).contains(shortDistanceLongTimeLinkId));
			}
		}		
	}
}
