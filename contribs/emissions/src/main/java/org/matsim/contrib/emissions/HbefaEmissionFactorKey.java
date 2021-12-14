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
		if (!(o instanceof HbefaEmissionFactorKey)) return false;

		var that = (HbefaEmissionFactorKey) o;

		if (vehicleCategory != that.getVehicleCategory()) return false;
		if (!Objects.equals(vehicleAttributes, that.vehicleAttributes)) return false;
		return Objects.equals(component, that.component);
	}

	@Override
	public int hashCode() {
		int result = vehicleCategory.hashCode();
		result = 31 * result + vehicleAttributes.hashCode();
		result = 31 * result + component.hashCode();
		return result;
	}
}
