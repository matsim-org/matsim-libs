package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * This is a carrier that has capabilities and resources, jobs and plans to fulfill its obligations.
 * 
 *  
 * @author sschroeder, mzilske
 *
 */
public class CarrierImpl implements Carrier {

	public static Carrier newInstance(Id id, Id linkId){
		return new CarrierImpl(id,linkId);
	}
	
	/**
	 * A builder that builds a carrier.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a new carrier builder.
		 * 
		 * Note that the capability object of the carrier is constructed as well. If you set the capabilities with the carrier.setCarrierCap...()
		 * the existing CarrierCapabilities are lost.
		 * 
		 * @param id
		 * @param linkId
		 * @return
		 */
		public static Builder newInstance(Id id, Id linkId){
			return new Builder(id,linkId);
		}
		
		private List<CarrierShipment> shipments;
		private List<CarrierVehicle> vehicles;
		private List<CarrierPlan> plans;
		private Id id;
		private Id linkId;
		private CarrierPlan selectedPlan;
		
		private Builder(Id id, Id linkId) {
			shipments = new ArrayList<CarrierShipment>();
			vehicles = new ArrayList<CarrierVehicle>();
			plans = new ArrayList<CarrierPlan>();
			this.id = id;
			this.linkId = linkId;
		}
		
		public Builder addShipment(CarrierShipment shipment){
			shipments.add(shipment);
			return this;
		}
		
		public Builder addVehicle(CarrierVehicle vehicle){
			vehicles.add(vehicle);
			return this;
		}
		
		public Builder addPlan(CarrierPlan plan){
			plans.add(plan);
			return this;
		}
		
		public Builder setSelectedPlan(CarrierPlan plan){
			selectedPlan = plan;
			return this;
		}
		
		/**
		 * Finally builds the carrier.
		 * 
		 * @return Carrier
		 * @see Carrier
		 */
		public Carrier build(){
			return new CarrierImpl(this);
		}
		
		
	}
	
	private final Id id;

	private final Collection<CarrierPlan> plans = new ArrayList<CarrierPlan>();

	private final Id depotLinkId;

	private final Collection<CarrierShipment> shipments = new ArrayList<CarrierShipment>();

	private CarrierPlan selectedPlan;

	private CarrierCapabilities carrierCapabilities;
	
	
	private CarrierImpl(Builder builder){
		this.carrierCapabilities = CarrierCapabilities.newInstance();
		this.carrierCapabilities.getCarrierVehicles().addAll(builder.vehicles);
		shipments.addAll(builder.shipments);
		plans.addAll(builder.plans);
		selectedPlan = builder.selectedPlan;
		id = builder.id;
		depotLinkId = builder.linkId;		
	}
	
	private CarrierImpl(final Id id, final Id depotLinkId) {
		super();
		this.carrierCapabilities = CarrierCapabilities.newInstance();
		this.id = id;
		this.depotLinkId = depotLinkId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getId()
	 */
	@Override
	public Id getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getDepotLinkId()
	 */
	@Override
	public Id getDepotLinkId() {
		return depotLinkId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getPlans()
	 */
	@Override
	public Collection<CarrierPlan> getPlans() {
		return plans;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getContracts()
	 */
	@Override
	public Collection<CarrierShipment> getShipments() {
		return shipments;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getSelectedPlan()
	 */
	@Override
	public CarrierPlan getSelectedPlan() {
		return selectedPlan;
	}

	/**
	 * Selects the selectedPlan.
	 * 
	 * <p> If the plan-collection does not contain the selectedPlan, it is added to that collection.
	 * 
	 * @param selectedPlan to be selected
	 */
	@Override
	public void setSelectedPlan(CarrierPlan selectedPlan) {
		if(!plans.contains(selectedPlan)) plans.add(selectedPlan);
		this.selectedPlan = selectedPlan;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.mzilske.freight.Carrier#setCarrierCapabilities(playground.
	 * mzilske.freight.CarrierCapabilities)
	 */
	@Override
	public void setCarrierCapabilities(CarrierCapabilities carrierCapabilities) {
		this.carrierCapabilities = carrierCapabilities;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getCarrierCapabilities()
	 */
	@Override
	public CarrierCapabilities getCarrierCapabilities() {
		return carrierCapabilities;
	}


}
