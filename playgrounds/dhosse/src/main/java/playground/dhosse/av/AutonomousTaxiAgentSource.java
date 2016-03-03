package playground.dhosse.av;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

public class AutonomousTaxiAgentSource implements AgentSource {

	private final DynActionCreator nextActionCreator;
	private final MatsimVrpContext context;
	private final VrpOptimizer optimizer;
	private final QSim qSim;
	
	private final VehicleType autonomousTaxiType;
	
	public AutonomousTaxiAgentSource(DynActionCreator nextActionCreator,
			MatsimVrpContext context, VrpOptimizer optimizer, QSim qSim) {
		
		this.nextActionCreator = nextActionCreator;
		this.context = context;
		this.optimizer = optimizer;
		this.qSim = qSim;
		
		this.autonomousTaxiType = createVehicleType();
		
	}

	@Override
	public void insertAgentsIntoMobsim() {
		
		VehiclesFactory qSimVehicleFactory = VehicleUtils.getFactory();
        for (Vehicle vrpVeh : context.getVrpData().getVehicles().values()) {
            Id<Vehicle> id = vrpVeh.getId();
            Id<Link> startLinkId = vrpVeh.getStartLink().getId();

            VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator, vrpVeh);
            DynAgent vrpAgent = new DynAgent(Id.createPersonId(id), startLinkId, qSim,
                    vrpAgentLogic);
            QVehicle mobsimVehicle = new QVehicle(qSimVehicleFactory.createVehicle(
                    Id.create(id, org.matsim.vehicles.Vehicle.class),
                    this.autonomousTaxiType));
            vrpAgent.setVehicle(mobsimVehicle);
            mobsimVehicle.setDriver(vrpAgent);

            qSim.addParkedVehicle(mobsimVehicle, startLinkId);
            qSim.insertAgentIntoMobsim(vrpAgent);
		
        }
        
	}
	
	private VehicleType createVehicleType(){

		//that's the reason for the existence of this class
		VehicleType type = VehicleUtils.getDefaultVehicleType();
		type.setMaximumVelocity(30 / 3.6);
		
		return type;
		
	}

}
