/**
 * 
 */
package sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierImpl;
import playground.mzilske.freight.carrier.CarrierPlan;
import playground.mzilske.freight.carrier.CarrierVehicle;
import playground.mzilske.freight.carrier.ScheduledTour;
import playground.mzilske.freight.carrier.Tour;
import playground.mzilske.freight.carrier.TourBuilder;
import sandbox.SandBoxTrafficGenerator.Company;
import sandbox.SandBoxTrafficGenerator.Region;



/**
 * @author stefan
 *
 */
public class TourGenerator {
	
	private List<Region> regions;
	
	private List<Company> companies;
	
	private List<Carrier> carriers;
	
	private VehicleFleetBuilder vehicleFleetBuilder;
	
	public TourGenerator(List<Region> regions, List<Company> companies, List<Carrier> carriers) {
		super();
		this.regions = regions;
		this.companies = companies;
		this.carriers = carriers;
	}
	

	public void setVehicleFleetBuilder(VehicleFleetBuilder vehicleFleetBuilder) {
		this.vehicleFleetBuilder = vehicleFleetBuilder;
	}


	public void run(){
		for(Company company : companies){
			CarrierImpl carrier = new CarrierImpl(company.getId(), company.getLinkId());
			Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
			CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
			carrier.setSelectedPlan(carrierPlan);
			company.setCarrier(carrier);
			vehicleFleetBuilder.buildVehicleFleet(company, carrier);
			buildCarrierPlan(company);
			carriers.add(carrier);
		}
	}
	
	
	private void buildVehicleFleet(Carrier carrier) {
		
		
	}

	private void buildCarrierPlan(Company company) {
		int nOfTours = generateNofTours();
		for(int i=0;i<nOfTours;i++){
			Tour tour = generateTour(company);
			CarrierVehicle carrierVehicle = getCarrierVehicle(company.getCarrier(),(i+1));
			company.getCarrier().getSelectedPlan().getScheduledTours().add(new ScheduledTour(tour, carrierVehicle, 0.0));
		}
	}

	private CarrierVehicle getCarrierVehicle(Carrier carrier, int i) {
		CarrierVehicle vehicle = new CarrierVehicle(makeId(carrier.getId().toString() + "_" + i), carrier.getDepotLinkId());
		return vehicle;
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

	private Tour generateTour(Company company) {
		TourBuilder builder = new TourBuilder();
		int nOfActivities = generateNofActivities();
		builder.scheduleStart(company.getCarrier().getDepotLinkId());
		Id lastDestination = company.getCarrier().getDepotLinkId();
		for(int i=0;i<nOfActivities;i++){
			Id destinationLink = getRandomRegionLink(lastDestination);
			builder.scheduleGeneralActivity("pause", destinationLink , null, null, 300.0);
			lastDestination = destinationLink;
		}
		builder.scheduleEnd(company.getCarrier().getDepotLinkId());
		return builder.build();
	}

	private Id getRandomRegionLink(Id lastDestination) {
		boolean regionLinkFound = false;
		Id linkId = null;
		while(!regionLinkFound){
			int randomIndex = MatsimRandom.getRandom().nextInt(regions.size());
			Region hit = regions.get(randomIndex);
			if(!hit.getLinkId().equals(lastDestination)){
				linkId = hit.getLinkId();
				regionLinkFound = true;
			}
		}
		return linkId;
	}

	private int generateNofActivities() {
		return MatsimRandom.getRandom().nextInt(10)+1;
	}

	private int generateNofTours() {
		return MatsimRandom.getRandom().nextInt(3)+1;
	}	
}
