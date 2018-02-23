package org.matsim.contrib.carsharing.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/** 
 * @author balac
 */
public class ParkCSVehicles implements AgentSource {
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private CarsharingSupplyInterface carsharingSupply;
	//private final static Logger log = Logger.getLogger(ParkCSVehicles.class);
	
	public ParkCSVehicles(QSim qSim, Scenario scenario,
			CarsharingSupplyInterface carsharingSupply) {
		
		this.qsim = qSim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = scenario.getConfig().qsim().getMainModes();
	
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
		this.carsharingSupply =  carsharingSupply;
		modeVehicleTypes.put("twoway", VehicleUtils.getDefaultVehicleType());
		modeVehicleTypes.put("freefloating", VehicleUtils.getDefaultVehicleType());

		modeVehicleTypes.put("oneway", VehicleUtils.getDefaultVehicleType());
		
		
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Map<CSVehicle, Link> allVehicleLocations = this.carsharingSupply.getAllVehicleLocations();

		for (CSVehicle vehicle : allVehicleLocations.keySet()) {
			
			qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(vehicle.getVehicleId(), Vehicle.class),
					modeVehicleTypes.get(vehicle.getCsType())), allVehicleLocations.get(vehicle).getId());
		}
		
		
	}

}
