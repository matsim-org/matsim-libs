package playground.balac.twowaycarsharingredisigned.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;




public class ParkTWVehicles implements AgentSource {
	//private Population population;
	//private AgentFactory agentFactory;
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	//private boolean insertVehicles = true;
	private TwoWayCSVehicleLocation twvehiclesLocationqt;
	public ParkTWVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			TwoWayCSVehicleLocation twvehiclesLocationqt) {
		//this.population = population;
		//this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.twvehiclesLocationqt = twvehiclesLocationqt;
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		if (twvehiclesLocationqt != null)
		for (TwoWayCSStation owstation: twvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id:owstation.getIDs()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("TW_"+(id), Vehicle.class), modeVehicleTypes.get("onewaycarsharing")), owstation.getLink().getId());

			}
			
		}
		
		
	}

}
