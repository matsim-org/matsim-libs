package org.matsim.contrib.carsharing.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.FFCSVehicle;
import org.matsim.contrib.carsharing.vehicles.FreeFloatingVehiclesLocation;
import org.matsim.contrib.carsharing.vehicles.OneWayCarsharingVehicleLocation;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.contrib.carsharing.vehicles.TwoWayCarsharingVehicleLocation;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


public class ParkCSVehicles implements AgentSource {
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private FreeFloatingVehiclesLocation ffvehiclesLocationqt;
	private OneWayCarsharingVehicleLocation owvehiclesLocationqt;
	private TwoWayCarsharingVehicleLocation twvehiclesLocationqt;
	
	private final static Logger log = Logger.getLogger(ParkCSVehicles.class);
	
	public ParkCSVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			FreeFloatingVehiclesLocation ffvehiclesLocationqt, OneWayCarsharingVehicleLocation owvehiclesLocationqt, TwoWayCarsharingVehicleLocation twvehiclesLocationqt) {
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.ffvehiclesLocationqt = ffvehiclesLocationqt;
		this.owvehiclesLocationqt = owvehiclesLocationqt;
		this.twvehiclesLocationqt = twvehiclesLocationqt;
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
		
		modeVehicleTypes.put("twowaycarsharing", VehicleUtils.getDefaultVehicleType());
		modeVehicleTypes.put("freefloating", VehicleUtils.getDefaultVehicleType());

		modeVehicleTypes.put("onewaycarsharing", VehicleUtils.getDefaultVehicleType());

	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		int counterTW = 0;
		int counterOW = 0;
		int counterFF = 0;

		if (ffvehiclesLocationqt != null)
		for (FFCSVehicle ffvehicle: ffvehiclesLocationqt.getQuadTree().values()) {
			

				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(ffvehicle.getVehicleId(), Vehicle.class),
						modeVehicleTypes.get("freefloating")), ffvehicle.getLink().getId() ) ;
				counterFF++;
			}
			
		
		if (owvehiclesLocationqt != null)
			for (OneWayCarsharingStation owstation: owvehiclesLocationqt.getQuadTree().values()) {
				Set<String> vehicleTypesAtStation = owstation.getVehicleIDsPerType().keySet();

				for (String type : vehicleTypesAtStation) {
					
					for (StationBasedVehicle vehicle : owstation.getVehicles(type)) {
						
						qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(vehicle.getVehicleId(), Vehicle.class),
								modeVehicleTypes.get("onewaycarsharing")), owstation.getLinkId());
						counterTW++;
						
					}					
				}				
			}		
		if (twvehiclesLocationqt != null) {
			for (TwoWayCarsharingStation twstation: twvehiclesLocationqt.getQuadTree().values()) {
				
				Set<String> vehicleTypesAtStation = twstation.getVehicleIDsPerType().keySet();
				
				for (String type : vehicleTypesAtStation) {
					
					for (StationBasedVehicle vehicle : twstation.getVehicles(type)) {
						
						qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(vehicle.getVehicleId(), Vehicle.class),
								modeVehicleTypes.get("twowaycarsharing")), twstation.getLinkId());
						counterTW++;
						
					}					
				}				
			}
			log.info("Parked " + counterTW + " twowaycarsharing vehicles.");
			log.info("Parked " + counterOW + " onewaycarsharing vehicles.");
			log.info("Parked " + counterFF + " freefloatingcarsharing vehicles.");

		}
		
	}

}
