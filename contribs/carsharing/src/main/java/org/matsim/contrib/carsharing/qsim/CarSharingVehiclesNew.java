package org.matsim.contrib.carsharing.qsim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReader;
import org.matsim.contrib.carsharing.vehicles.FreeFloatingVehiclesLocation;
import org.matsim.contrib.carsharing.vehicles.OneWayCarsharingVehicleLocation;
import org.matsim.contrib.carsharing.vehicles.TwoWayCarsharingVehicleLocation;

public class CarSharingVehiclesNew {
	
	//private static final Logger log = Logger.getLogger(CarSharingVehicles.class);

	private Scenario scenario;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;
	private OneWayCarsharingVehicleLocation owvehiclesLocation;
	private TwoWayCarsharingVehicleLocation twvehiclesLocation;
	
	public CarSharingVehiclesNew(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public FreeFloatingVehiclesLocation getFreeFLoatingVehicles() {
		
		return this.ffvehiclesLocation;
	}
	
	public OneWayCarsharingVehicleLocation getOneWayVehicles() {
		
		
		return this.owvehiclesLocation;
	}
	
	public TwoWayCarsharingVehicleLocation getTwoWayVehicles() {
		
		return this.twvehiclesLocation;
	}
	
	public void readVehicleLocations() {
		
		CarsharingXmlReader carsharingReader = new CarsharingXmlReader(scenario.getNetwork());
		
		final TwoWayCarsharingConfigGroup configGrouptw = (TwoWayCarsharingConfigGroup)
				scenario.getConfig().getModule( TwoWayCarsharingConfigGroup.GROUP_NAME );

		carsharingReader.readFile(configGrouptw.getvehiclelocations());
		this.twvehiclesLocation = new TwoWayCarsharingVehicleLocation(scenario, carsharingReader.getTwStations());
		this.owvehiclesLocation = new OneWayCarsharingVehicleLocation(scenario, carsharingReader.getOwStations());
		this.ffvehiclesLocation = new FreeFloatingVehiclesLocation(scenario, carsharingReader.getFFVehicles());

		
	}

}
