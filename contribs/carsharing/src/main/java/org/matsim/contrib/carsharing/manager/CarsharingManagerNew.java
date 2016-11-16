package org.matsim.contrib.carsharing.manager;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.CompanyContainer;
import org.matsim.contrib.carsharing.manager.supply.OneWayContainer;
import org.matsim.contrib.carsharing.manager.supply.TwoWayContainer;
import org.matsim.contrib.carsharing.manager.supply.VehiclesContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;

/** 
 * Class containing all the information about carsharing supply and demand.
 *  
 * @author balac
 */
public class CarsharingManagerNew implements CarsharingManagerInterface, IterationStartsListener{
	
	@Inject private Scenario scenario;
	@Inject private CurrentTotalDemand currentDemand;
	@Inject private KeepingTheCarModel keepTheCarModel;
	@Inject private ChooseTheCompany chooseCompany;
	@Inject private ChooseVehicleType chooseVehicleType;
	@Inject private CarsharingSupplyInterface carsharingSupplyContainer;
	@Inject private EventsManager eventsManager;
	@Inject private RouterProvider routerProvider;
	
	@Override
	public List<PlanElement> reserveAndrouteCarsharingTrip(Plan plan, String carsharingType, Leg legToBeRouted, Double time) {
		Network network = scenario.getNetwork();
		Person person = plan.getPerson();
		Link startLink = network.getLinks().get(legToBeRouted.getRoute().getStartLinkId());
		Link destinationLink = network.getLinks().get(legToBeRouted.getRoute().getEndLinkId());
		
		
		//=== get the vehicle for the trip if the agent has it already ===
		CSVehicle vehicle = getVehicleAtLocation(startLink,	plan.getPerson(), carsharingType);		

		boolean willHaveATripFromLocation = willUseTheVehicleLaterFromLocation(destinationLink.getId(), plan, legToBeRouted);
		
		double durationOfNextActivity = getDurationOfNextActivity(plan, legToBeRouted, time);		
		
		boolean keepTheCar = keepTheCarModel.keepTheCarDuringNextActivity(durationOfNextActivity, plan.getPerson(), carsharingType);	
		//TODO: create a method for getting the search distance
		double searchDistance = 1000.0;
		if (vehicle != null) {
			
			if ((willHaveATripFromLocation && keepTheCar) || (willHaveATripFromLocation && carsharingType.equals("twoway"))) {
			return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
					startLink, destinationLink, true, true);
			}
			else {
				
				if (carsharingType.equals("oneway")) {
				
					CompanyContainer companyContainer = this.carsharingSupplyContainer.getCompany(vehicle.getCompanyId());
					VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
					Link parkingLocation = vehiclesContainer.findClosestAvailableParkingLocation(destinationLink, searchDistance);					
					
					if (parkingLocation == null)
						return null;
					destinationLink = parkingLocation;
					vehiclesContainer.reserveParking(destinationLink);
					
					
				}
				else if (carsharingType.equals("twoway")) {
					CarsharingStation parkingStation = ((TwoWayContainer)this.carsharingSupplyContainer.getCompany(vehicle.getCompanyId()).
							getVehicleContainer(carsharingType)).getTwowaycarsharingstationsMap().
							get(((StationBasedVehicle)vehicle).getStationId());
					destinationLink = parkingStation.getLink();
				}			
				
				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
						startLink, destinationLink, false, true);				
			}			
		}
		else {
			
			//=== agent does not hold the vehicle, therefore must find a one from the supply side===
			//=== here he chooses the company, type of vehicle and in the end vehicle ===
			//=== possibly this could be moved to one method which decides based on the supply which vehicle to take
			String typeOfVehicle = chooseVehicleType.getPreferredVehicleType(plan, legToBeRouted);

			String companyId = chooseCompany.pickACompany(plan, legToBeRouted, time, typeOfVehicle);
			if (!companyId.equals("")) {
				vehicle = this.carsharingSupplyContainer.findClosestAvailableVehicle(startLink,
						carsharingType, typeOfVehicle, companyId, searchDistance);
				if (vehicle == null)
					return null;			
				CompanyContainer companyContainer = this.carsharingSupplyContainer.getCompany(companyId);

				VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
				Link stationLink = vehiclesContainer.getVehicleLocation(vehicle);
				companyContainer.reserveVehicle(vehicle);
			
				eventsManager.processEvent(new StartRentalEvent(time, carsharingType, startLink, stationLink, person.getId(), vehicle.getVehicleId()));
			
				if ((willHaveATripFromLocation && keepTheCar) || (willHaveATripFromLocation && carsharingType.equals("twoway"))) {
					return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
							stationLink, destinationLink, true, false);
				}
				else {
					if ((carsharingType.equals("oneway")) || (carsharingType.equals("freefloating"))) {
						Link parkingStationLink = this.carsharingSupplyContainer.findClosestAvailableParkingSpace(
								destinationLink, carsharingType, companyId, searchDistance);
						if (parkingStationLink == null)
							return null;
					
						vehiclesContainer.reserveParking(parkingStationLink);
					
						destinationLink = parkingStationLink;
					}
	
					return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
							stationLink, destinationLink, false, false);				
				}	
			}
			else
				return null;
		}					
	}
	
	private double getDurationOfNextActivity(Plan plan, Leg legToBeRouted, double time) {
		
		int index = plan.getPlanElements().indexOf(legToBeRouted);
		
		Activity a = (Activity) plan.getPlanElements().get(index + 1);
		
		return a.getEndTime() - time > 0.0 ? a.getEndTime() - time : 0.0;		
	}

	private boolean willUseTheVehicleLaterFromLocation(Id<Link> linkId, Plan plan, Leg currentLeg) {
		boolean willUseVehicle = false;

		String mode = currentLeg.getMode();
		List<PlanElement> planElements = plan.getPlanElements();

		int index = planElements.indexOf(currentLeg) + 1;

		for (int i = index; i < planElements.size(); i++) {

			if (planElements.get(i) instanceof Leg) {

				if (((Leg)planElements.get(i)).getMode().equals(mode)) {

					if (((Leg)planElements.get(i)).getRoute().getStartLinkId().toString().equals(linkId.toString())) {

						willUseVehicle = true;
					}
				}
			}
		}
		return willUseVehicle;
	}	
	private CSVehicle getVehicleAtLocation(Link currentLink, Person person, String carsharingType) {

		return this.currentDemand.getVehicleOnLink(person.getId(), currentLink, carsharingType);	
		
	}	
	@Override
	public void freeParkingSpot(String vehicleId, Id<Link> linkId) {
		
		CSVehicle vehicle = this.carsharingSupplyContainer.getVehicleWithId(vehicleId);
		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		CompanyContainer companyContainer = this.carsharingSupplyContainer.getCompany(vehicle.getCompanyId());
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(vehicle.getCsType());
		
		((OneWayContainer)vehiclesContainer).freeParkingSpot(link);			
	}

	@Override
	public boolean parkVehicle(String vehicleId, Id<Link> linkId) {
		CSVehicle vehicle = this.carsharingSupplyContainer.getVehicleWithId(vehicleId);
		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		this.carsharingSupplyContainer.getCompany(vehicle.getCompanyId()).parkVehicle(vehicle, link);
		return false;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.carsharingSupplyContainer.populateSupply();
		this.currentDemand.reset();
	}		
}
