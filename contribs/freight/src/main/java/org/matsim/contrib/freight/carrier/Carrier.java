package org.matsim.contrib.freight.carrier;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

public interface Carrier {
	
	public static int PLAN_MEMORY = 5;

	public abstract Id getId();

	public abstract Id getDepotLinkId();

	public abstract Collection<CarrierPlan> getPlans();

	public abstract Collection<CarrierShipment> getShipments();

	public abstract CarrierPlan getSelectedPlan();

	public abstract void setSelectedPlan(CarrierPlan selectedPlan);

	public abstract void setCarrierCapabilities(CarrierCapabilities carrierCapabilities);

	public abstract CarrierCapabilities getCarrierCapabilities();

}