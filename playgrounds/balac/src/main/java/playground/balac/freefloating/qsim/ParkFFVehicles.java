package playground.balac.freefloating.qsim;

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


public class ParkFFVehicles implements AgentSource {
	private Population population;
	private AgentFactory agentFactory;
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private boolean insertVehicles = true;
	private FreeFloatingVehiclesLocation ffvehiclesLocationqt;
	public ParkFFVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			FreeFloatingVehiclesLocation ffvehiclesLocationqt) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.ffvehiclesLocationqt = ffvehiclesLocationqt;
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		if (ffvehiclesLocationqt != null)
		for (FreeFloatingStation ffstation: ffvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id:ffstation.getIDs()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("FF_"+(id), Vehicle.class), modeVehicleTypes.get("freefloating")), ffstation.getLink().getId());

			}
			
		}
		
		
	}

}
