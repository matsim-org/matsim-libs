/* *********************************************************************** *
 * project: org.matsim.*
 * SimVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The ``Q'' implementation of the MobsimVehicle.
 * <p></p>
 * Design thoughts:<ul>
 * <li> This needs to be public since the ``Q'' version of the
 * vehicle is used by more than one package.  This interfaces should, however, not be used outside the relevant
 * netsimengines.  In particular, the information should not be used for visualization.  kai, nov'11
 * <li> Might be possible to make all methods in this hierarchy protected and just inherit to a QueueVehicle.  kai, nov'11
 * </ul>
 *
 * @author nagel
 */

public class QVehicle extends QItem implements MobsimVehicle, Serializable {
	private static final Logger log = Logger.getLogger(QVehicle.class);
	private static final long serialVersionUID = -1331834361118700652L;
	
	private static class SerializationProxy implements Serializable {
		private final String vehicleIdAsString;
		private final MobsimDriverAgent driverAgent;
		//		private final Integer planIndex;
//		private final Integer routeIndex;
		private static final long serialVersionUID = 3771813209818182306L;
		private final double linkEnterTime ;
		private final double earliestLinkExitTime ;
		SerializationProxy( QVehicle qVeh ) {
			this.linkEnterTime = qVeh.linkEnterTime ;
			this.earliestLinkExitTime = qVeh.earliestLinkExitTime ;
			this.vehicleIdAsString = qVeh.id.toString() ;
//			this.driverId = qVeh.getDriver().getId() ;
//			this.planIndex = WithinDayAgentUtils.getCurrentPlanElementIndex( qVeh.getDriver() );;
//			this.routeIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex( qVeh.getDriver() );
			this.driverAgent = qVeh.getDriver() ;
		}

		Object readResolve() throws ObjectStreamException {
			final VehicleType type = VehicleUtils.getDefaultVehicleType() ;
			Vehicle basicVehicle = new VehicleImpl( Id.createVehicleId( vehicleIdAsString ), type ) ;
			final QVehicle qVeh = new QVehicle( basicVehicle );
			qVeh.setDriver( driverAgent );
			driverAgent.setVehicle( qVeh );
			final Link link = QNetsimEngine.qsim.getScenario().getNetwork().getLinks().get( driverAgent.getCurrentLinkId() );;
			qVeh.setCurrentLink( link );
			return qVeh ;
		}
		
	}
	
	private static int warnCount = 0;

	private double linkEnterTime = 0. ;
	private double earliestLinkExitTime = 0;
	private final int passengerCapacity;
	private DriverAgent driver = null;
	private Collection<PassengerAgent> passengers = null;
	private final Id<Vehicle> id;
	private Link currentLink = null;
	private final Vehicle vehicle;
	
	private Object writeReplace() {
		return new SerializationProxy( this ) ;
	}
	
	public QVehicle(final Vehicle basicVehicle) {
		this.id = basicVehicle.getId();
		this.vehicle = basicVehicle;
		this.passengers = new ArrayList<>();

		VehicleCapacity capacity = basicVehicle.getType().getCapacity();
		if (capacity == null) {
			this.passengerCapacity = 4;
			if (warnCount < 10) {
				log.warn("No VehicleCapacity (= maximum number of passengers) set in Vehicle. "
						+ "Using default value of 4.  This is only a problem if you need vehicles with different "
						+ "capacities, e.g. for minibuses.");
				warnCount++;
				if ( warnCount == 10 ) {
					log.warn( Gbl.FUTURE_SUPPRESSED ) ;
				}
			}
		} else {
			// do *not* subtract one for the driver! Most pt vehicles define the capacity without the driver.
			// for private cars, think about if we should subtract one from the capacity if the driver is set?
			// But if we do, change the number of seats of the default vehicle from 4 to 5.
			this.passengerCapacity = capacity.getSeats() +
					(capacity.getStandingRoom() == null ? 0 : capacity.getStandingRoom());
		}
	}

	void setCurrentLink(final Link link) {
		this.currentLink = link;
	}

	@Override
	public double getEarliestLinkExitTime() {
		return this.earliestLinkExitTime;
	}

	@Override
	public void setEarliestLinkExitTime(final double time) {
		this.earliestLinkExitTime = time;
	}

	@Override
	public Link getCurrentLink() {
		return this.currentLink;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		if ( this.driver instanceof MobsimDriverAgent ) {
			return (MobsimDriverAgent) this.driver;
		} else if ( this.driver==null ) {
			return null ;
		} else {
			throw new RuntimeException( "error (downstream methods need to be made to accept DriverAgent)") ;
		}
	}

	public void setDriver(final DriverAgent driver) {
		if (driver != null) {
			if (this.driver != null && !this.driver.getId().equals(driver.getId())) {
				throw new RuntimeException( "A driver (" + this.driver.getId() +") " +
						"is already set in vehicle " + this.getId() + ". " +
						"Setting agent " + driver.getId().toString() + " is not possible!");
			}
		}
		// TODO: To make this check possible, we would need something like removeDriver().
//		else {
//			throw new RuntimeException( "Driver to be set in vehicle " + this.getId() +
//					" is null!");
//		}

		this.driver = driver;
	}

	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

    @Override
    public double getSizeInEquivalents() {
        return vehicle.getType().getPcuEquivalents();
    }

    public double getFlowCapacityConsumptionInEquivalents() {
        return vehicle.getType().getPcuEquivalents() / vehicle.getType().getFlowEfficiencyFactor();
    }

	@Override
	public Vehicle getVehicle() {
		return this.vehicle;
	}

	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getId()
				+ ", on link " + this.currentLink.getId();
	}

	public double getMaximumVelocity() {
		return vehicle.getType().getMaximumVelocity();
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		return Collections.unmodifiableCollection(this.passengers);
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		if (this.passengers.size() < this.passengerCapacity) {
			return this.passengers.add(passenger);
		}
		throw new RuntimeException("vehicle already at capacity, thus not possible to add passenger") ;
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		return this.passengers.remove(passenger);
	}

	@Override
	public int getPassengerCapacity() {
		return this.passengerCapacity;
	}

	final double getLinkEnterTime() {
		return this.linkEnterTime;
	}

	final void setLinkEnterTime(double linkEnterTime) {
		// yyyyyy use in code!
		this.linkEnterTime = linkEnterTime;
	}
}
