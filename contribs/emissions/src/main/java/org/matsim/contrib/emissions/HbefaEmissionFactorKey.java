/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import java.util.Objects;

abstract class HbefaEmissionFactorKey {

	private HbefaVehicleCategory vehicleCategory;
	private HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
	private Pollutant component;

	HbefaEmissionFactorKey(HbefaEmissionFactorKey copyFrom) {
		this.vehicleCategory = copyFrom.getVehicleCategory();
		this.vehicleAttributes = copyFrom.getVehicleAttributes();
		this.component = copyFrom.getComponent();
	}

	HbefaEmissionFactorKey() {

	}

	public HbefaVehicleCategory getVehicleCategory() {
		return vehicleCategory;
	}

	public void setVehicleCategory(HbefaVehicleCategory vehicleCategory) {
		this.vehicleCategory = vehicleCategory;
	}

	public HbefaVehicleAttributes getVehicleAttributes() {
		return vehicleAttributes;
	}

	public void setVehicleAttributes(HbefaVehicleAttributes vehicleAttributes) {
		this.vehicleAttributes = vehicleAttributes;
	}

	public Pollutant getComponent() {
		return component;
	}

	public void setComponent(Pollutant component) {
		this.component = component;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HbefaEmissionFactorKey efk)) return false;

		if (vehicleCategory != efk.getVehicleCategory()) return false;
		if (!Objects.equals(vehicleAttributes, efk.vehicleAttributes)) return false;
		return Objects.equals(component, efk.component);
	}

	@Override
	public int hashCode() {
		int result = vehicleCategory.hashCode();
		result = 31 * result + vehicleAttributes.hashCode();
		result = 31 * result + component.hashCode();
		return result;
	}
}
