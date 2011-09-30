package playground.mzilske.freight.carrier;

import java.util.Collection;


public interface CarrierPlanBuilder {

	public abstract CarrierPlan buildPlan(
			CarrierCapabilities carrierCapabilities,
			Collection<CarrierContract> carrierContracts);

}