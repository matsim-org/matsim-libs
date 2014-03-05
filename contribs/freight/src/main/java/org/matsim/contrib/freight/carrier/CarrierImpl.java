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

	public static Carrier newInstance(Id id){
		return new CarrierImpl(id);
	}
	
	private final Id id;

	private final List<CarrierPlan> plans;

	private final Collection<CarrierShipment> shipments; 

	private final Collection<CarrierService> services;

	private CarrierCapabilities carrierCapabilities;
	
	private CarrierPlan selectedPlan;
	
	private CarrierImpl(final Id id) {
		super();
		this.carrierCapabilities = CarrierCapabilities.newInstance();
		this.id = id;
		services = new ArrayList<CarrierService>();
		shipments = new ArrayList<CarrierShipment>();
		plans = new ArrayList<CarrierPlan>();
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
//	@Override
//	public Id getDepotLinkId() {
//		return depotLinkId;
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.mzilske.freight.Carrier#getPlans()
	 */
	@Override
	public List<CarrierPlan> getPlans() {
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

	public Collection<CarrierService> getServices(){
		return services;
	}

	@Override
	public boolean addPlan(CarrierPlan p) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public CarrierPlan createCopyOfSelectedPlanAndMakeSelected() {
		CarrierPlan newPlan = CarrierImpl.copyPlan(this.selectedPlan) ;
		this.setSelectedPlan( newPlan ) ;
		return newPlan ;
	}

	public static CarrierPlan copyPlan(CarrierPlan plan2copy) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : plan2copy.getScheduledTours()) {
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = sTour.getTour().duplicate();
			tours.add(ScheduledTour.newInstance(tour, vehicle, depTime));
		}
		CarrierPlan copiedPlan = new CarrierPlan(plan2copy.getCarrier(), tours);
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		return copiedPlan;
	
	}

	@Override
	public boolean removePlan(CarrierPlan p) {
		return this.plans.remove(p);
	}
	

}
