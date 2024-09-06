package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** 
 * @author balac
 */
public class ParkCSVehicles implements AgentSource {
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private CarsharingSupplyInterface carsharingSupply;
	//private final static Logger log = LogManager.getLogger(ParkCSVehicles.class);
	
	public ParkCSVehicles(QSim qSim,
			CarsharingSupplyInterface carsharingSupply) {
		
		this.qsim = qSim;  
		this.modeVehicleTypes = new HashMap<>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
	
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.createDefaultVehicleType());
		}
		this.carsharingSupply =  carsharingSupply;
		modeVehicleTypes.put("twoway", VehicleUtils.createDefaultVehicleType());
		modeVehicleTypes.put("freefloating", VehicleUtils.createDefaultVehicleType());

		modeVehicleTypes.put("oneway", VehicleUtils.createDefaultVehicleType());
		
		
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Map<CSVehicle, Link> allVehicleLocations = this.carsharingSupply.getAllVehicleLocations();
		VehiclesFactory factory = this.qsim.getScenario().getVehicles().getFactory();

		for (CSVehicle vehicle : allVehicleLocations.keySet()) {
			final Vehicle basicVehicle = factory.createVehicle( Id.create( vehicle.getVehicleId(), Vehicle.class ),
					modeVehicleTypes.get( vehicle.getCsType() ) );

			final QVehicleImpl qvehicle = new QVehicleImpl( basicVehicle );
			// yyyyyy should react to new QVehicleFactory!  kai, nov'18
			
//			qsim.createAndParkVehicleOnLink(, allVehicleLocations.get(vehicle).getId());
			qsim.addParkedVehicle( qvehicle, allVehicleLocations.get(vehicle).getId() );
		}
		
		
	}

}
