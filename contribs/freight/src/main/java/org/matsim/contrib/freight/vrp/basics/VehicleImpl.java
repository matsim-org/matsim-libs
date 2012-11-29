/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.basics;

/**
 * 
 * @author stefan schroeder
 * 
 */

public class VehicleImpl implements Vehicle {

	public static class NoVehicle extends VehicleImpl {

		public NoVehicle() {
			super("noVehicle", null, null);
		}
		
		public int getCapacity(){
			return 0;
		}
		
	}
	
	public static class Type {
		public final String typeId;
		public final int capacity;
		public final VehicleCostParams vehicleCostParams;

		public Type(String typeId, int capacity,
				VehicleCostParams vehicleCostParams) {
			super();
			this.typeId = typeId;
			this.capacity = capacity;
			this.vehicleCostParams = vehicleCostParams;
		}
	}

	public static class VehicleCostParams {
		public final double fix;
		public final double perTimeUnit;
		public final double perDistanceUnit;

		public VehicleCostParams(double fix, double perTimeUnit,
				double perDistanceUnit) {
			super();
			this.fix = fix;
			this.perTimeUnit = perTimeUnit;
			this.perDistanceUnit = perDistanceUnit;
		}
	}

	public static class Factory {
		public Type createType(String typeId, int capacity,
				VehicleCostParams vehicleCostParams) {
			return new Type(typeId, capacity, vehicleCostParams);
		}

		public VehicleCostParams createVehicleCostParams(double fix,
				double perTimeUnit, double perDistanceUnit) {
			return new VehicleCostParams(fix, perTimeUnit, perDistanceUnit);
		}

		public VehicleImpl createVehicle(String id, String location, Type type) {
			return new VehicleImpl(id, location, type);
		}

		public VehicleImpl createtStandardVehicle() {
			return createVehicle(
					"standard",
					"noLocation",
					createType("standard", 0,
							createVehicleCostParams(0, 0, 1.0)));
		}
	}

	public static Factory getFactory() {
		return new Factory();
	}

	public static NoVehicle createNoVehicle(){
		return new NoVehicle();
	}
	
	private Type type;

	private String locationId;

	private String id;

	private double earliestDeparture = 0.0;

	private double latestArrival = Double.MAX_VALUE;

	public VehicleImpl(String id, String locationId, Type type) {
		super();
		this.locationId = locationId;
		this.id = id;
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.freight.vrp.basics.Vehicle#getEarliestDeparture()
	 */
	@Override
	public double getEarliestDeparture() {
		return earliestDeparture;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.contrib.freight.vrp.basics.Vehicle#setEarliestDeparture(double
	 * )
	 */

	public void setEarliestDeparture(double earliestDeparture) {
		this.earliestDeparture = earliestDeparture;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.freight.vrp.basics.Vehicle#getLatestArrival()
	 */
	@Override
	public double getLatestArrival() {
		return latestArrival;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.contrib.freight.vrp.basics.Vehicle#setLatestArrival(double)
	 */

	public void setLatestArrival(double latestArrival) {
		this.latestArrival = latestArrival;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.freight.vrp.basics.Vehicle#getLocationId()
	 */
	@Override
	public String getLocationId() {
		return locationId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.freight.vrp.basics.Vehicle#getType()
	 */
	@Override
	public Type getType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.freight.vrp.basics.Vehicle#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.freight.vrp.basics.Vehicle#getCapacity()
	 */
	@Override
	public int getCapacity() {
		return type.capacity;
	}
}
