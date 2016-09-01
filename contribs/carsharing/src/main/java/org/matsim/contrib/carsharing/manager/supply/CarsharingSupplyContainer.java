package org.matsim.contrib.carsharing.manager.supply;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public class CarsharingSupplyContainer {
	private Map<String, CompanyContainer> companies = new HashMap<String, CompanyContainer>();
	private Map<String, CSVehicle> allVehicles = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> allVehicleLocations = new HashMap<CSVehicle, Link>();
	private Set<String> companyNames;
	
	
	public Map<CSVehicle, Link> getAllVehicleLocations() {
		return allVehicleLocations;
	}

	public Map<String, CSVehicle> getAllVehicles() {
		return allVehicles;
	}

	private Scenario scenario;
	
	public CarsharingSupplyContainer(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public void addCompany(CompanyContainer companyContainer) {
		
		this.companies.put(companyContainer.getCompanyId(), companyContainer);
		
	}
	
	public CompanyContainer getCompany(String companyId) {
		
		return this.companies.get(companyId);
	}
	
	
	public CSVehicle getVehicleqWithId (String vehicleId) {
		
		return this.allVehicles.get(vehicleId);
	}

	public CSVehicle findClosestAvailableVehicle(Link startLink, String carsharingType, String typeOfVehicle,
			String companyId, double searchDistance) {
		
		CompanyContainer companyContainer = this.companies.get(companyId);
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		return vehiclesContainer.findClosestAvailableVehicle(startLink, typeOfVehicle, searchDistance);		
	}		

	public Link findClosestAvailableParkingSpace(Link destinationLink, String carsharingType,
			String companyId, double searchDistance) {
		CompanyContainer companyContainer = this.companies.get(companyId);
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		return vehiclesContainer.findClosestAvailableParkingLocation(destinationLink, searchDistance);	
	}

	public void populateSupply() {

		Network network = this.scenario.getNetwork();
		
		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(network);
		
		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup)
				scenario.getConfig().getModule( CarsharingConfigGroup.GROUP_NAME );

		reader.readFile(configGroup.getvehiclelocations());
		this.companies = reader.getCompanies();
		this.allVehicleLocations = reader.getAllVehicleLocations();
		this.allVehicles = reader.getAllVehicles();
		this.companyNames = reader.getCompanyNames();
	}

	public Set<String> getCompanyNames() {
		return companyNames;
	}	
}
