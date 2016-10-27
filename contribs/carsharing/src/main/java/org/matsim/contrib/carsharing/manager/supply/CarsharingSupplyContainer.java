package org.matsim.contrib.carsharing.manager.supply;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
/** 
 * @author balac
 */
public class CarsharingSupplyContainer implements CarsharingSupplyInterface {
	private Map<String, CompanyContainer> companies = new HashMap<String, CompanyContainer>();
	private Map<String, CSVehicle> allVehicles = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> allVehicleLocations = new HashMap<CSVehicle, Link>();
	private Set<String> companyNames;
	private Scenario scenario;
	
	public CarsharingSupplyContainer(Scenario scenario) {
		this.scenario = scenario;
	}	
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#getAllVehicleLocations()
	 */
	@Override
	public Map<CSVehicle, Link> getAllVehicleLocations() {
		return allVehicleLocations;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#getAllVehicles()
	 */
	@Override
	public Map<String, CSVehicle> getAllVehicles() {
		return allVehicles;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#getCompany(java.lang.String)
	 */
	@Override
	public CompanyContainer getCompany(String companyId) {
		
		return this.companies.get(companyId);
	}
	
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#getVehicleWithId(java.lang.String)
	 */
	@Override
	public CSVehicle getVehicleWithId (String vehicleId) {
		
		return this.allVehicles.get(vehicleId);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#findClosestAvailableVehicle(org.matsim.api.core.v01.network.Link, java.lang.String, java.lang.String, java.lang.String, double)
	 */
	@Override
	public CSVehicle findClosestAvailableVehicle(Link startLink, String carsharingType, String typeOfVehicle,
			String companyId, double searchDistance) {
		
		CompanyContainer companyContainer = this.companies.get(companyId);
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		return vehiclesContainer.findClosestAvailableVehicle(startLink, typeOfVehicle, searchDistance);		
	}		

	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#findClosestAvailableParkingSpace(org.matsim.api.core.v01.network.Link, java.lang.String, java.lang.String, double)
	 */
	@Override
	public Link findClosestAvailableParkingSpace(Link destinationLink, String carsharingType,
			String companyId, double searchDistance) {
		CompanyContainer companyContainer = this.companies.get(companyId);
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		return vehiclesContainer.findClosestAvailableParkingLocation(destinationLink, searchDistance);	
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#populateSupply()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface#getCompanyNames()
	 */
	@Override
	public Set<String> getCompanyNames() {
		return companyNames;
	}	
}
