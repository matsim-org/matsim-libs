package org.matsim.contrib.carsharing.manager.supply;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.FreefloatingAreasReader;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.config.ConfigGroup;

import com.google.inject.Inject;

/**
 * @author balac
 */
public class CarsharingSupplyContainer implements CarsharingSupplyInterface {
	private Map<String, CompanyContainer> companies = new HashMap<String, CompanyContainer>();
	private Map<String, CompanyAgent> companyAgents = new HashMap<>();
	private Map<String, CSVehicle> allVehicles = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> allVehicleLocations = new HashMap<CSVehicle, Link>();
	private Set<String> companyNames;
	private Scenario scenario;
	
	public CarsharingSupplyContainer(Scenario scenario) {
		this.scenario = scenario;
		//populateSupply();
	}	
	
	
	@Override
	public Map<CSVehicle, Link> getAllVehicleLocations() {
		return allVehicleLocations;
	}

	
	@Override
	public Map<String, CSVehicle> getAllVehicles() {
		return allVehicles;
	}
	
	
	@Override
	public CompanyContainer getCompany(String companyId) {
		
		return this.companies.get(companyId);
	}
	
	
	
	@Override
	public CSVehicle getVehicleWithId (String vehicleId) {
		
		return this.allVehicles.get(vehicleId);
	}

	
	@Override
	public CSVehicle findClosestAvailableVehicle(Link startLink, String carsharingType, String typeOfVehicle,
			String companyId, double searchDistance) {
		
		CompanyContainer companyContainer = this.companies.get(companyId);
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		return vehiclesContainer.findClosestAvailableVehicle(startLink, typeOfVehicle, searchDistance);		
	}		

	
	@Override
	public Link findClosestAvailableParkingSpace(Link destinationLink, String carsharingType,
			String companyId, double searchDistance) {
		CompanyContainer companyContainer = this.companies.get(companyId);
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		return vehiclesContainer.findClosestAvailableParkingLocation(destinationLink, searchDistance);	
	}

	
	@Override
	public void populateSupply() {
		Network network = this.scenario.getNetwork();

		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup)
				scenario.getConfig().getModule( CarsharingConfigGroup.GROUP_NAME );

		final FreeFloatingConfigGroup ffConfigGroup = (FreeFloatingConfigGroup)
				this.scenario.getConfig().getModule(FreeFloatingConfigGroup.GROUP_NAME);

		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(network);

		String areasFile = ffConfigGroup.getAreas();
		if (areasFile != null) {
			FreefloatingAreasReader ffAreasReader = new FreefloatingAreasReader();
			ffAreasReader.parse(ConfigGroup.getInputFileURL(scenario.getConfig().getContext(), areasFile));
			reader.setFreefloatingAreas(ffAreasReader.getFreefloatingAreas());
		}

		reader.readFile(configGroup.getvehiclelocations());
		this.companies = reader.getCompanies();
		this.allVehicleLocations = reader.getAllVehicleLocations();
		this.allVehicles = reader.getAllVehicles();
		this.companyNames = reader.getCompanyNames();
		
		for (String companyName : this.companyNames) {
			
			CompanyAgent companyAGent = new CompanyAgentImpl(this.companies.get(companyName), "");
			this.companyAgents.put(companyName, companyAGent);
		}
		
	}

	
	@Override
	public Set<String> getCompanyNames() {
		return companyNames;
	}


	@Override
	public Map<String, CompanyAgent> getCompanyAgents() {
		return companyAgents;
	}	
}
