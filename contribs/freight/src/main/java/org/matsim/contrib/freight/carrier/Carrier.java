package org.matsim.contrib.freight.carrier;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

/**
 * A carrier.
 * 
 * @author sschroeder
 *
 */
public interface Carrier {
	
	public static int PLAN_MEMORY = 5;

	/**
	 * Gets the carrierId.
	 * 
	 * @return id
	 */
	public abstract Id getId();

	/**
	 * Gets a collection of carrierPlans.
	 * 
	 * @return collection of {@link CarrierPlan}
	 */
	public abstract Collection<CarrierPlan> getPlans();

	/**
	 * Gets a collection of carrierShipments
	 * 
	 * @return collection of {@link CarrierShipment}
	 */
	public abstract Collection<CarrierShipment> getShipments();
	
	/**
	 * Gets a collection of carrierServices
	 * 
	 * @return collection of {@link CarrierService}
	 */
	public abstract Collection<CarrierService> getServices();

	/**
	 * Gets the selected plan.
	 * 
	 * @return {@link CarrierPlan}
	 */
	public abstract CarrierPlan getSelectedPlan();

	/**
	 * Sets a {@link CarrierPlan} as selected.
	 * 
	 * <p>If selectePlan in not in plan-collection, it adds it.
	 * 
	 * @param selectedPlan
	 */
	public abstract void setSelectedPlan(CarrierPlan selectedPlan);

	/**
	 * Sets carrierCapabilities.
	 * 
	 * @param carrierCapabilities
	 */
	public abstract void setCarrierCapabilities(CarrierCapabilities carrierCapabilities);

	/**
	 * Gets the carrierCapabilities.
	 * 
	 * @return {@link CarrierCapabilities}
	 */
	public abstract CarrierCapabilities getCarrierCapabilities();

}