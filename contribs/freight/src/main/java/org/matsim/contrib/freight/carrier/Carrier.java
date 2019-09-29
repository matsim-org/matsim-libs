package org.matsim.contrib.freight.carrier;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * A carrier.
 * 
 * @author sschroeder
 *
 */
public interface Carrier extends HasPlansAndId<CarrierPlan, Carrier>, Attributable{
	
	public static int PLAN_MEMORY = 5;

	/**
	 * Gets the carrierId.
	 * 
	 * @return id
	 */
	@Override
	public abstract Id<Carrier> getId();

	/**
	 * Gets a collection of carrierPlans.
	 * 
	 * @return collection of {@link CarrierPlan}
	 */
	@Override
	public abstract List<CarrierPlan> getPlans();

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
	@Override
	public abstract CarrierPlan getSelectedPlan();

	/**
	 * Sets a {@link CarrierPlan} as selected.
	 * 
	 * <p>If selectePlan in not in plan-collection, it adds it.
	 * 
	 * @param selectedPlan to be set
	 */
	@Override
	public abstract void setSelectedPlan(CarrierPlan selectedPlan);

	/**
	 * Sets carrierCapabilities.
	 * 
	 * @param carrierCapabilities to be set
	 */
	public abstract void setCarrierCapabilities(CarrierCapabilities carrierCapabilities);

	/**
	 * Gets the carrierCapabilities.
	 * 
	 * @return {@link CarrierCapabilities}
	 */
	public abstract CarrierCapabilities getCarrierCapabilities();


	void clearPlans();
}
