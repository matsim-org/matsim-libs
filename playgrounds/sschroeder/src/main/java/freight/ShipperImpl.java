package freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Actor;


public class ShipperImpl implements Actor{
	private Id id;
	
	private ShipperPlan selectedPlan;
	
	private Collection<ShipperPlan> plans = new ArrayList<ShipperPlan>();
	
	private Id locationId;
	
	private ShipperKnowledge shipperKnowledge = new ShipperKnowledge();
	
	public ShipperKnowledge getShipperKnowledge() {
		return shipperKnowledge;
	}

	private Collection<ShipperContract> contracts = new ArrayList<ShipperContract>();

	public ShipperImpl(Id id, Id locationId) {
		super();
		this.id = id;
		this.locationId = locationId;
	}

	public ShipperPlan getSelectedPlan() {
		return selectedPlan;
	}

	public void setSelectedPlan(ShipperPlan selectedPlan) {
		this.selectedPlan = selectedPlan;
	}

	public Collection<ShipperPlan> getPlans() {
		return plans;
	}

	public Collection<ShipperContract> getContracts() {
		return contracts;
	}

	public Id getId() {
		return id;
	}

	public Id getLocationId() {
		return locationId;
	}
	
	@Override
	public String toString() {
		return "[shipperId="+id+"][linkId="+locationId+"]";
	}
	
	
}
