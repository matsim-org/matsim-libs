/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaWarmEmissionFactorKey.java
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

package playground.julia.emission.types;

import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.types.HbefaVehicleCategory;

/**
 * @author benjamin
 * @author julia
 * 
 **/
public class HbefaWarmEmissionFactorKey {
	
	private HbefaVehicleCategory hbefaVehicleCategory;
	private WarmPollutant hbefaComponent;
	private Integer hbefaParkingTime;
	private Integer hbefaDistance;
	private HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
	
	public HbefaWarmEmissionFactorKey(){
	}

	public HbefaWarmEmissionFactorKey(WarmPollutant pollutant,  int distance,int parkingTime,
			String vehicleAttributesTechnology,  String vehicleAttributesConcept,String vehicleAttributesClass,
			HbefaVehicleCategory vehicleCategory) {
			this.hbefaVehicleCategory= vehicleCategory;
			this.hbefaComponent = pollutant;
			this.hbefaParkingTime = parkingTime;
			this.hbefaDistance = distance;
			this.hbefaVehicleAttributes = new HbefaVehicleAttributes(vehicleAttributesTechnology, vehicleAttributesConcept, vehicleAttributesClass);
	}

	public HbefaVehicleCategory getHbefaVehicleCategory() {
		return hbefaVehicleCategory;
	}

	public void setHbefaVehicleCategory(HbefaVehicleCategory hbefaVehicleCategory) {
		this.hbefaVehicleCategory = hbefaVehicleCategory;
	}

	public WarmPollutant getHbefaComponent() {
		return hbefaComponent;
	}

	public void setHbefaComponent(WarmPollutant hbefaComponent) {
		this.hbefaComponent = hbefaComponent;
	}

	public Integer getHbefaParkingTime() {
		return hbefaParkingTime;
	}

	public void setHbefaParkingTime(Integer hbefaParkingTime) {
		this.hbefaParkingTime = hbefaParkingTime;
	}

	public Integer getHbefaDistance() {
		return hbefaDistance;
	}

	public void setHbefaDistance(Integer hbefaDistance) {
		this.hbefaDistance = hbefaDistance;
	}
		
	public HbefaVehicleAttributes getHbefaVehicleAttributes() {
		return hbefaVehicleAttributes;
	}

	public void setHbefaVehicleAttributes(HbefaVehicleAttributes hbefaVehicleAttributes) {
		this.hbefaVehicleAttributes = hbefaVehicleAttributes;
	}

	/* need to implement the "equals" method in order to be able to construct an "equal" key
	 later on (e.g. from data available in the simulation)*/
	@Override
	public boolean equals(Object obj) {
	        if(this == obj) {
	              return true;
	         }
	         if (!(obj instanceof HbefaWarmEmissionFactorKey)) {
	                return false; 
	         }
	         HbefaWarmEmissionFactorKey key = (HbefaWarmEmissionFactorKey) obj;
	         return hbefaVehicleCategory.equals(key.getHbefaVehicleCategory())
	         && hbefaComponent.equals(key.getHbefaComponent())
	         && hbefaParkingTime.equals(key.getHbefaParkingTime())
	         && hbefaDistance.equals(key.getHbefaDistance())
	         && hbefaVehicleAttributes.equals(key.getHbefaVehicleAttributes());
	}

	// if "equals" is implemented, "hashCode" also needs to be implemented
	@Override
	public int hashCode(){
		return toString().hashCode();
	} 
	

	// needed for "hashCode" method
	@Override
	public String toString(){
		return hbefaVehicleCategory + "; " 
		+ hbefaComponent + "; "
		+ hbefaParkingTime + "; "
		+ hbefaDistance+ "; "
		+ hbefaVehicleAttributes;
	}
}