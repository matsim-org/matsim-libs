package org.matsim.contrib.carsharing.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.FreeFloatingVehiclesLocation;
import org.matsim.contrib.carsharing.vehicles.OneWayCarsharingVehicleLocation;
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
		for (FreeFloatingStation ffstation: ffvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id:ffstation.getIDs()) {
				//log.info("Parked freefloating car with id: " + id);

				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("FF_"+(id), Vehicle.class), modeVehicleTypes.get("freefloating")), ffstation.getLinkId() ) ;
				counterFF++;
			}
			
		}
		if (owvehiclesLocationqt != null)
			for (OneWayCarsharingStation owstation: owvehiclesLocationqt.getQuadTree().values()) {
				
				for (String id:owstation.getIDs()) {
					qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("OW_"+(id), Vehicle.class), modeVehicleTypes.get("onewaycarsharing")), owstation.getLinkId());
					counterOW++;
				}
				
			}
		
		if (twvehiclesLocationqt != null) {
			for (TwoWayCarsharingStation twstation: twvehiclesLocationqt.getQuadTree().values()) {
				
				for (String id : twstation.getIDs()) {
					
					qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("TW_"+id, Vehicle.class), modeVehicleTypes.get("twowaycarsharing")), twstation.getLinkId());
					counterTW++;
				}
				
			}
			log.info("Parked " + counterTW + " twowaycarsharing vehicles.");
			log.info("Parked " + counterOW + " onewaycarsharing vehicles.");
			log.info("Parked " + counterFF + " freefloatingcarsharing vehicles.");

		}
		
	}

}
