package energy.controlers;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.controllers.AddHandlerAtStartupControler;
import org.matsim.contrib.transEnergySim.controllers.EventHandlerGroup;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.core.config.Config;

public class DisChargingControler extends AddHandlerAtStartupControler {

	
	private HashMap<Id, Vehicle> vehicles;
	private EnergyConsumptionTracker energyConsumptionTracker;


	
	public DisChargingControler(Config config,  HashMap<Id, Vehicle> vehicles) {
		super(config);
		init(vehicles);
		
	}

	public DisChargingControler(String[] args,  HashMap<Id, Vehicle> vehicles) {
		super(args);
		init(vehicles);
	}
	
		

	private void init(HashMap<Id, Vehicle> vehicles2) {
		this.vehicles = vehicles2;
		EventHandlerGroup handlerGroup = new EventHandlerGroup();
		setEnergyConsumptionTracker(new EnergyConsumptionTracker(vehicles, network));
		handlerGroup.addHandler(getEnergyConsumptionTracker());
		addHandler(handlerGroup);		
	}
	
	

	public void printStatisticsToConsole() {
		System.out.println("energy consumption stats");
		energyConsumptionTracker.getLog().printToConsole();
		System.out.println("===");

	}


	
	public EnergyConsumptionTracker getEnergyConsumptionTracker() {
		return energyConsumptionTracker;
	}

	private void setEnergyConsumptionTracker(EnergyConsumptionTracker energyConsumptionTracker) {
		this.energyConsumptionTracker = energyConsumptionTracker;
	}
	
}
