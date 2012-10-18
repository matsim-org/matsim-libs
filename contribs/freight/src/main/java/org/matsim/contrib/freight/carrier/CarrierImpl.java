package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

public class CarrierImpl implements Carrier {

	private final Id id;

	private final Collection<CarrierPlan> plans = new ArrayList<CarrierPlan>();

	private final Id depotLinkId;

	private final Collection<CarrierShipment> shipments = new ArrayList<CarrierShipment>();

	private CarrierPlan selectedPlan;

	private CarrierCapabilities carrierCapabilities;
	
	public CarrierImpl(final Id id, final Id depotLinkId) {
		super();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.mzilske.freight.Carrier#setSelectedPlan(playground.mzilske
	 * .freight.CarrierPlan)
	 */
	@Override
	public void setSelectedPlan(CarrierPlan selectedPlan) {
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
