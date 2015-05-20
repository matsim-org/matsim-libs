/* *********************************************************************** *
 * project: org.matsim.*
 * ParkedVehicleProvider.java
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
package playground.thibautd.pseudoqsim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import playground.thibautd.socnetsim.qsim.QVehicleProvider;

/**
 * @author thibautd
 */
public class ParkedVehicleProvider implements QVehicleProvider {
	private final Map<Id, QVehicle> vehicles  = new HashMap<Id, QVehicle>();
	private final Map<Id, Id> vehicle2parkingLink = new HashMap<Id, Id>();

	@Override
	public QVehicle getVehicle(final Id vehicleId) {
		return vehicles.get( vehicleId );
	}

	public void addVehicle(final QVehicle vehicle, final Id linkId) {
		final QVehicle old = vehicles.put( vehicle.getId() , vehicle );
		if ( old != null ) throw new IllegalStateException( "already a vehicle "+vehicle.getId() );
		park( vehicle.getId() , linkId );
	}

	public Id unpark(final Id vehicleId) {
		return vehicle2parkingLink.remove( vehicleId );
	}

	public boolean unpark(final Id vehicleId , final Id linkId ) {
		final Id parkLink = vehicle2parkingLink.get( vehicleId );

		if ( !linkId.equals( parkLink ) ) return false;

		vehicle2parkingLink.remove( vehicleId );
		return true;
	}

	public void park(final Id vehicleId , final Id linkId ) {
		final Id parkLink = vehicle2parkingLink.put( vehicleId , linkId );

		if ( parkLink != null ) {
			throw new IllegalStateException( "vehicle "+vehicleId+" already parked at link "+parkLink+" when trying to park at link "+linkId );
		}
	}
}

