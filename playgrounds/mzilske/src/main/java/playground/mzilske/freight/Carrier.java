package playground.mzilske.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

public interface Carrier {

	public abstract CarrierKnowledge getKnowledge();

	public abstract void setKnowledge(CarrierKnowledge knowledge);

	public abstract Id getId();

	public abstract Id getDepotLinkId();

	public abstract Collection<CarrierPlan> getPlans();

	public abstract Collection<Contract> getContracts();

	public abstract CarrierPlan getSelectedPlan();

	public abstract void setSelectedPlan(CarrierPlan selectedPlan);

	public abstract void setCarrierCapabilities(
			CarrierCapabilities carrierCapabilities);

	public abstract CarrierCapabilities getCarrierCapabilities();

}