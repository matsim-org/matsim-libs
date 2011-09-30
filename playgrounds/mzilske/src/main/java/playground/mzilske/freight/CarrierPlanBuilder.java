package playground.mzilske.freight;

import java.util.Collection;


public interface CarrierPlanBuilder {

	public abstract CarrierPlan buildPlan(
			CarrierCapabilities carrierCapabilities,
			Collection<CarrierContract> carrierContracts);

}