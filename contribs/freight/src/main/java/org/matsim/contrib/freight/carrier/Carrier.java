package org.matsim.contrib.freight.carrier;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

public interface Carrier extends Actor{

	public abstract Id getId();

	public abstract Id getDepotLinkId();

	public abstract Collection<CarrierPlan> getPlans();

	public abstract Collection<CarrierContract> getContracts();

	public abstract CarrierPlan getSelectedPlan();

	public abstract void setSelectedPlan(CarrierPlan selectedPlan);

	public abstract void setCarrierCapabilities(
			CarrierCapabilities carrierCapabilities);

	public abstract CarrierCapabilities getCarrierCapabilities();
	
	public abstract Collection<CarrierContract> getNewContracts();
	
	public abstract Collection<CarrierContract> getExpiredContracts();

}