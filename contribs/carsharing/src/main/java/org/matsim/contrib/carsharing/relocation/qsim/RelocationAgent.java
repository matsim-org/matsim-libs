package org.matsim.contrib.carsharing.relocation.qsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.CompanyContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * Mostly copied from tutorial.programming.ownMobsimAgentUsingRouter.MyMobsimAgent
 * 
 * MobsimDriverAgent that
 * - gets a person Id, start and destination link Id
 * - computes best path to it at every turn, using a Guidance instance
 */

public class RelocationAgent implements MobsimDriverAgent {
	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private Id<Person> id;
	private String companyId;
	private Guidance guidance;
	private MobsimTimer mobsimTimer;
	private Scenario scenario;
	private Network network;
	private CarsharingSupplyInterface carsharingSupply;
	// RelocationAgent needs CompanyContainer and Company

	private Id<Link> homeLinkId;
	private Id<Link> currentLinkId;
	private Id<Link> destinationLinkId;
	private String transportMode;
	private MobsimVehicle vehicle;
	private State state;

	private ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

	private ArrayList<PlanElement> planElements = new ArrayList<PlanElement>();

	private List<Double> relocationTimes = null;

	public RelocationAgent(Id<Person> id, String companyId, Id<Link> homeLinkId, Scenario scenario, Network network) {
		this.id = id;
		this.companyId = companyId;
		this.currentLinkId = this.homeLinkId = homeLinkId;
		this.scenario = scenario;
		this.network = network;
		this.startActivity();
	}

	public void setCarsharingSupplyContainer(CarsharingSupplyInterface carsharingSupply) {
		this.carsharingSupply = carsharingSupply;
	}

	public void setGuidance(Guidance guidance) {
		this.guidance = guidance;
	}

	public void setRelocationTimes(List<Double> relocationTimes) {
		this.relocationTimes = relocationTimes;
	}

	public ArrayList<RelocationInfo> getRelocations() {
		return this.relocations;
	}

	public ArrayList<PlanElement> getPlanElements()
	{
		return this.planElements;
	}

    /**
     * - add RelocationInfo to relocations
     * - reserve car sharing vehicle, aka remove it from storage structure
     */
	public void dispatchRelocation(RelocationInfo info) {
		CompanyContainer companyContainer = this.carsharingSupply.getCompany(this.companyId);		
		CSVehicle vehicle = this.carsharingSupply.getVehicleWithId(info.getVehicleId());
		if (true == companyContainer.reserveVehicle(vehicle)) {
			this.relocations.add(info);

			log.info("relocationAgent " + this.id + " removed vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId());
		} else {
			log.info("relocationAgent " + this.id + " could not remove vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId());
		}
	}

	public void setMobsimTimer(MobsimTimer mobsimTimer) {
		this.mobsimTimer = mobsimTimer;
	}

	public MobsimTimer getMobsimTimer() {
		return this.mobsimTimer;
	}

	public double getTimeOfDay() {
		if (this.getMobsimTimer() != null) {
			return this.getMobsimTimer().getTimeOfDay();
		}

		return 0;
	}

	public void reset() {
		this.relocations.clear();
		this.planElements.clear();
		this.currentLinkId = this.homeLinkId;
		this.startActivity();

		log.info("resetting agent " + this.getId());
	}

	protected void startLeg(String transportMode) {
		Leg leg = PopulationUtils.createLeg(transportMode);
		leg.setDepartureTime(this.getTimeOfDay());
		leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(this.getCurrentLinkId(), this.getDestinationLinkId()));
		this.planElements.add(leg);
	}

	protected void endLeg() {
		try {
			Leg leg = (Leg) this.getCurrentPlanElement();
			leg.setTravelTime(this.getTimeOfDay() - leg.getDepartureTime());
		} catch (Exception e) {
			// do nothing
		}
	}

	protected void addLinkId(Id<Link> linkId) {
		try {
			Leg leg = (Leg) this.getCurrentPlanElement();
			NetworkRoute route =  (NetworkRoute)leg.getRoute();

			List<Id<Link>> linkIds = new ArrayList<Id<Link>>(route.getLinkIds());
			linkIds.add(linkId);
			Route newRoute = RouteUtils.createLinkNetworkRouteImpl(route.getStartLinkId(), linkIds, route.getEndLinkId());

			leg.setRoute(newRoute);
		} catch (Exception e) {
			// do nothing
		}
	}

	protected void startActivity() {
		Activity activity = PopulationUtils. createActivityFromLinkId("work", this.getCurrentLinkId());
		activity.setStartTime(this.getTimeOfDay());
		this.planElements.add(activity);
		this.state = State.ACTIVITY;
	}

	protected void endActivity() {
		try {
			Activity activity = (Activity) this.getCurrentPlanElement();
			activity.setEndTime(this.getTimeOfDay());
		} catch (Exception e) {
			// do nothing
		}
	}

	protected PlanElement getCurrentPlanElement()
	{
		return this.planElements.get(this.planElements.size() - 1);
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId;
	}

	@Override
	public Id<Person> getId() {
		return this.id ;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public double getActivityEndTime() {
		double now = this.getTimeOfDay();

		if (this.relocationTimes != null) {
			if (this.relocations.isEmpty() == false) {
				return now;
			} else {
				for (Double relocationTime : this.relocationTimes) {
					if (now < relocationTime + 1) {
						return relocationTime + 1;
					}
				}
			}
		}

		return Double.POSITIVE_INFINITY;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		try {
			this.endActivity();
			this.prepareRelocation(this.relocations.get(0));
		} catch (IndexOutOfBoundsException e) {
			// resume idling
			this.startActivity();
		}
	}

	private void prepareRelocation(RelocationInfo relocationInfo) {
		this.destinationLinkId = relocationInfo.getStartLinkId();
		this.transportMode = TransportMode.bike;
		this.state = State.LEG;
		this.startLeg(TransportMode.bike);
	}

	private void executeRelocation(RelocationInfo relocationInfo) {
		this.destinationLinkId = relocationInfo.getEndLinkId();
		this.transportMode = TransportMode.car;
		// TODO: set vehicle
		this.startLeg(TransportMode.car);
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		this.endLeg();

		if (this.relocations.isEmpty()) {
			this.destinationLinkId = null;
			this.startActivity();
		} else {
			if (this.getDestinationLinkId().equals(this.homeLinkId) &&
					!this.getDestinationLinkId().equals(this.relocations.get(0).getStartLinkId())) {
				try {
					this.prepareRelocation(this.relocations.get(0));
				} catch (IndexOutOfBoundsException e) {
					this.destinationLinkId = null;
					this.startActivity();
				}
			} else if (this.getDestinationLinkId().equals(this.relocations.get(0).getStartLinkId())) {
				RelocationInfo relocationInfo = this.relocations.get(0);
				relocationInfo.setStartTime(now);
				this.executeRelocation(relocationInfo);
			} else if (this.getDestinationLinkId().equals(this.relocations.get(0).getEndLinkId())) {
				this.deliverCarSharingVehicle();

				try {
					RelocationInfo relocationInfo = this.relocations.get(0);
					relocationInfo.setEndTime(now);
					this.relocations.remove(0); 
					this.prepareRelocation(this.relocations.get(0));
				} catch (IndexOutOfBoundsException e) {
					this.destinationLinkId = this.homeLinkId;
					this.transportMode = TransportMode.bike;
					this.startLeg(TransportMode.bike);
				}
			}
		}
	}

	@Override
	public void setStateToAbort(double now) {
		this.state = State.ABORT;
	}

	@Override
	public Double getExpectedTravelTime() {
		if (this.transportMode.equals("bike"))
			return 600.0;
		else
			return this.guidance.getExpectedTravelTime(this.network.getLinks().get(this.getCurrentLinkId()),
					this.network.getLinks().get(this.getDestinationLinkId()), this.getTimeOfDay(), this.transportMode,
					null);
	}

    @Override
    public Double getExpectedTravelDistance() {
		return this.guidance.getExpectedTravelDistance(
				this.network.getLinks().get(this.getCurrentLinkId()),
				this.network.getLinks().get(this.getDestinationLinkId()),
				this.getTimeOfDay(),
				this.transportMode,
				null
		);
    }

    @Override
	public String getMode() {
		return this.transportMode;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		this.currentLinkId = linkId;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.guidance.getBestOutgoingLink(
				this.network.getLinks().get(this.getCurrentLinkId()),
				this.network.getLinks().get(this.getDestinationLinkId()),
				this.getTimeOfDay()
		);
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		this.addLinkId(newLinkId);
		this.currentLinkId = newLinkId ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.getCurrentLinkId().equals( this.getDestinationLinkId() ) ) {
			return true;
		}

		return false ;
	}

	private void deliverCarSharingVehicle() {
		CompanyContainer companyContainer = this.carsharingSupply.getCompany(this.companyId);		
		CSVehicle vehicle = this.carsharingSupply.getVehicleWithId(this.relocations.get(0).getVehicleId());
		companyContainer.parkVehicle(vehicle, this.network.getLinks().get(this.getCurrentLinkId()));
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return (this.relocations.get(0) != null) ? Id.create(this.relocations.get(0).getVehicleId(), Vehicle.class) : null;
	}

	@Override
	public Facility getCurrentFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Facility getDestinationFacility() {
		// TODO Auto-generated method stub
		return null;
	}
}
