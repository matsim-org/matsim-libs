package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

public class CarrierImpl {
	
	private Id id;
	
	private CarrierPlan selectedPlan;
	
	private Collection<CarrierPlan> plans = new ArrayList<CarrierPlan>();
	
	private Id depotLinkId;
	
	private Collection<Contract> contracts = new ArrayList<Contract>();

	private CarrierCapabilities carrierCapabilities;
	
	private CarrierKnowledge knowledge;

	public CarrierKnowledge getKnowledge() {
		return knowledge;
	}

	public void setKnowledge(CarrierKnowledge knowledge) {
		this.knowledge = knowledge;
	}

	public CarrierImpl(Id id, Id depotLinkId) {
		super();
		this.id = id;
		this.depotLinkId = depotLinkId;
	}

	public Id getId() {
		return id;
	}

	public Id getDepotLinkId() {
		return depotLinkId;
	}

	public Collection<CarrierPlan> getPlans() {
		return plans;
	}

	public Collection<Contract> getContracts() {
		return contracts;
	}

	public CarrierPlan getSelectedPlan() {
		return selectedPlan;
	}

	public void setSelectedPlan(CarrierPlan selectedPlan) {
		this.selectedPlan = selectedPlan;
	}

	public void setCarrierCapabilities(CarrierCapabilities carrierCapabilities) {
		this.carrierCapabilities = carrierCapabilities;
	}

	public CarrierCapabilities getCarrierCapabilities() {
		return carrierCapabilities;
	}
	
}
