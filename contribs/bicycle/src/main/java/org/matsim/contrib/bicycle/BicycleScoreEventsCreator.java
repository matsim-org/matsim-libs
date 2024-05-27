/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * BicycleLegScoring.java                                                  *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * @author dziemke
 */
class BicycleScoreEventsCreator implements
//		SumScoringFunction.LegScoring, SumScoringFunction.ArbitraryEventScoring
		VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,VehicleLeavesTrafficEventHandler
{
// yyyy The car interaction is still somewhat primitive -- a vehicle leaving a link gets a penalty that is multiplied with the number of cars that
// are on the link at that moment.  Evidently, this could be improved, by counting the cars that actually overtake the bicycle.  Not very difficult
// ...

	private static final Logger log = LogManager.getLogger( BicycleScoreEventsCreator.class ) ;
	private final Network network;
	private final EventsManager eventsManager;
	private final AdditionalBicycleLinkScore additionalBicycleLinkScore;
	private final String bicycleMode;

	private final Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();
	private final Map<Id<Vehicle>,Id<Link>> firstLinkIdMap = new LinkedHashMap<>();
	private final Map<Id<Vehicle>,String> modeFromVehicle = new LinkedHashMap<>();
	private final Map<String,Map<Id<Link>,Double>> numberOfVehiclesOnLinkByMode = new LinkedHashMap<>();
	private final BicycleConfigGroup bicycleConfig;

	@Inject BicycleScoreEventsCreator( Scenario scenario, EventsManager eventsManager, AdditionalBicycleLinkScore additionalBicycleLinkScore ) {
		this.eventsManager = eventsManager;
		this.network = scenario.getNetwork();
		this.additionalBicycleLinkScore = additionalBicycleLinkScore;
		this.bicycleConfig = ConfigUtils.addOrGetModule( scenario.getConfig(), BicycleConfigGroup.class );
		this.bicycleMode = bicycleConfig.getBicycleMode();
	}

	@Override public void reset( int iteration ){
		vehicle2driver.reset( iteration );
	}

	@Override public void handleEvent( VehicleEntersTrafficEvent event ){
		vehicle2driver.handleEvent( event );

		this.firstLinkIdMap.put( event.getVehicleId(), event.getLinkId() );

		if ( this.bicycleConfig.isMotorizedInteraction() ){
			modeFromVehicle.put( event.getVehicleId(), event.getNetworkMode() );

			// inc count by one:
			numberOfVehiclesOnLinkByMode.putIfAbsent( event.getNetworkMode(), new LinkedHashMap<>() );
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get( event.getNetworkMode() );
			map.merge( event.getLinkId(), 1., Double::sum );
		}
	}

	@Override public void handleEvent( LinkEnterEvent event ) {
		if ( this.bicycleConfig.isMotorizedInteraction() ){
			// inc count by one:
			String mode = this.modeFromVehicle.get( event.getVehicleId() );
			numberOfVehiclesOnLinkByMode.putIfAbsent( mode, new LinkedHashMap<>() );
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get( mode );
			map.merge( event.getLinkId(), 1., Double::sum );
		}
	}

	@Override public void handleEvent( LinkLeaveEvent event ){
		if ( this.bicycleConfig.isMotorizedInteraction() ){
			// dec count by one:
			String mode = this.modeFromVehicle.get( event.getVehicleId() );
			numberOfVehiclesOnLinkByMode.putIfAbsent( mode, new LinkedHashMap<>() );
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get( mode );
			Gbl.assertIf( map.merge( event.getLinkId(), -1., Double::sum ) >= 0 );
		}

		if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			double amount = additionalBicycleLinkScore.computeLinkBasedScore( network.getLinks().get( event.getLinkId() ) );

			if ( this.bicycleConfig.isMotorizedInteraction() ) {
				// yyyy this is the place where instead a data structure would need to be build that counts interaction with every car
				// that entered the link after the bicycle, and left it before.  kai, jul'23
				var carCounts = this.numberOfVehiclesOnLinkByMode.get( TransportMode.car );
				if ( carCounts != null ){
					amount -= 0.004 * carCounts.getOrDefault( event.getLinkId(), 0. );
				}
			}

			final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
			Gbl.assertNotNull( driverOfVehicle );
			this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore" ) );
		} else {
			log.warn( "no driver found for vehicleId=" + event.getVehicleId() + "; not clear why this could happen");
		}
	}

	@Override public void handleEvent( VehicleLeavesTrafficEvent event ){
		if ( this.bicycleConfig.isMotorizedInteraction() ){
			// dec count by one:
			String mode = this.modeFromVehicle.get( event.getVehicleId() );
			numberOfVehiclesOnLinkByMode.putIfAbsent( mode, new LinkedHashMap<>() );
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get( mode );
			Gbl.assertIf( map.merge( event.getLinkId(), -1., Double::sum ) >= 0. );
		}
		if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			if( !Objects.equals( this.firstLinkIdMap.get( event.getVehicleId() ), event.getLinkId() ) ){
				// what is this good for?  maybe that bicycles that enter and leave on the same link should not receive the additional score?  kai, jul'23

				// yyyy in the link based scoring, it actually uses event.getReleativePositionOnLink.  Good idea!  kai, jul'23

				double amount = additionalBicycleLinkScore.computeLinkBasedScore( network.getLinks().get( event.getLinkId() ) );

				final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
				Gbl.assertNotNull( driverOfVehicle );
				this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore" ) );
			}
		} else {
			log.warn( "no driver found for vehicleId=" + event.getVehicleId() + "; not clear why this could happen" );
		}
		// Needs to be called last, because it will remove driver information
		vehicle2driver.handleEvent( event );
		// ---
	}

}
