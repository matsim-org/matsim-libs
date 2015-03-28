package playground.balac.allcsmodestest.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.balac.freefloating.qsim.FreeFloatingStation;
import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingStation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingVehicleLocation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;

public class ParkCSVehicles implements AgentSource {
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private FreeFloatingVehiclesLocation ffvehiclesLocationqt;
	private OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocationqt;
	private TwoWayCSVehicleLocation twvehiclesLocationqt;
	
	private final static Logger log = Logger.getLogger(ParkCSVehicles.class);
	
	public ParkCSVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			FreeFloatingVehiclesLocation ffvehiclesLocationqt, OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocationqt, TwoWayCSVehicleLocation twvehiclesLocationqt) {
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
		if (ffvehiclesLocationqt != null)
		for (FreeFloatingStation ffstation: ffvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id:ffstation.getIDs()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("FF_"+(id), Vehicle.class), modeVehicleTypes.get("freefloating")), ffstation.getLink().getId());

			}
			
		}
		if (owvehiclesLocationqt != null)
			for (OneWayCarsharingRDWithParkingStation owstation: owvehiclesLocationqt.getQuadTree().values()) {
				
				for (String id:owstation.getIDs()) {
					qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("OW_"+(id), Vehicle.class), modeVehicleTypes.get("onewaycarsharing")), owstation.getLink().getId());

				}
				
			}
		
		if (twvehiclesLocationqt != null) {
			for (TwoWayCSStation twstation: twvehiclesLocationqt.getQuadTree().values()) {
				
				for (String id : twstation.getIDs()) {
					
					qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("TW_"+id, Vehicle.class), modeVehicleTypes.get("twowaycarsharing")), twstation.getLink().getId());
					counterTW++;
				}
				
			}
			log.info("Parked " + counterTW + " twowaycarsharing vehicles.");
		}
		
	}

}
