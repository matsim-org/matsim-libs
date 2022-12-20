/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;


import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * @author dgrether
 */
public final class VehicleCapacity implements Attributable {
	// maybe at least these subtypes should be immutable?
	// --> no, we make them fully settable, "data base in memory".  This has been the matsim design from the beginning, and all attempts to deviate from it
	// always seem to lead to awkward code.  If parallel computing pieces need immutable data structures, they first need to copy them from the in-memory
	// data base.  kai, sep'19

	private Integer seats = 0;
	private Integer standingRoom = 0 ;
	private Double volumeInCubicMeters = Double.POSITIVE_INFINITY ;
	private Double weightInTons = Double.POSITIVE_INFINITY ;
	private Attributes attributes = new AttributesImpl() ;
	private Double other = Double.POSITIVE_INFINITY ;

	/* package-private */ VehicleCapacity(){ }
	public Integer getSeats() {
		return seats;
	}
	public Integer getStandingRoom() {
		return standingRoom;
	}
	public VehicleCapacity setSeats( Integer seats ) {
		this.seats = seats;
		return this ;
	}
	public VehicleCapacity setStandingRoom( Integer standingRoom ) {
		this.standingRoom = standingRoom;
		return this ;
	}
	public Double getVolumeInCubicMeters() {
		return volumeInCubicMeters;
	}
	public VehicleCapacity setVolumeInCubicMeters( double volumeInCubicMeters ) {
		this.volumeInCubicMeters = volumeInCubicMeters;
		return this ;
	}
	public Double getWeightInTons() {
		return weightInTons;
	}
	public VehicleCapacity setWeightInTons( double weightInTons ) {
		this.weightInTons = weightInTons;
		return this ;
	}
	public Attributes getAttributes(){
		return this.attributes ;
	}
	public VehicleCapacity setOther( double other ){
		this.other = other ;
		return this ;
	}
	public Double getOther() {
		return this.other;
	}
}
