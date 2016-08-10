package org.matsim.contrib.carsharing.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.FFVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


public class ParkCSVehicles implements AgentSource {
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private QuadTree<OneWayCarsharingStation> owvehiclesLocationqt;
	private QuadTree<TwoWayCarsharingStation> twvehiclesLocationqt;
	private Map<FFVehicle, Link> ffvehiclesMap;
	private final static Logger log = Logger.getLogger(ParkCSVehicles.class);
	
	public ParkCSVehicles(QSim qSim,
			CarSharingVehiclesNew carSharingVehicles) {
		
		this.qsim = qSim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.owvehiclesLocationqt = carSharingVehicles.getOwvehicleLocationQuadTree();
		this.twvehiclesLocationqt = carSharingVehicles.getTwvehicleLocationQuadTree();
		this.ffvehiclesMap = carSharingVehicles.getFfvehiclesMap();
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
		
		modeVehicleTypes.put("twowaycarsharing", VehicleUtils.getDefaultVehicleType());
		modeVehicleTypes.put("freefloating", VehicleUtils.getDefaultVehicleType());

		modeVehicleTypes.put("onewaycarsharing", VehicleUtils.getDefaultVehicleType());
		
		
	}

	@Override
	public void insertAgentsIntoMobsim() {
		int counterTW = 0;
		int counterOW = 0;
		int counterFF = 0;

		if (!this.ffvehiclesMap.isEmpty()) {
			
			for (FFVehicle ffvehicle : this.ffvehiclesMap.keySet()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(ffvehicle.getVehicleId(), Vehicle.class),
						modeVehicleTypes.get("freefloating")), this.ffvehiclesMap.get(ffvehicle).getId() ) ;
				counterFF++;
				
			}
		}
		
			
		
		if (owvehiclesLocationqt != null)
			for (OneWayCarsharingStation owstation: owvehiclesLocationqt.values()) {
				Set<String> vehicleTypesAtStation = owstation.getVehiclesPerType().keySet();

				for (String type : vehicleTypesAtStation) {
					
					for (StationBasedVehicle vehicle : owstation.getVehicles(type)) {
						
						qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(vehicle.getVehicleId(), Vehicle.class),
								modeVehicleTypes.get("onewaycarsharing")), owstation.getLinkId());
						counterTW++;
						
					}					
				}				
			}		
		if (twvehiclesLocationqt != null) {
			for (TwoWayCarsharingStation twstation: twvehiclesLocationqt.values()) {
				
				Set<String> vehicleTypesAtStation = twstation.getVehiclesPerType().keySet();
				
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
