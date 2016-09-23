package playground.sebhoerl.av.logic.agent;

import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;

public class AVAgentSource implements AgentSource {
    final private AVFleet fleet;
    final private QSim qsim;
    
    public AVAgentSource(AVFleet fleet, QSim qsim) {
        this.fleet = fleet;
        this.qsim = qsim;
    }
    
    @Override
    public void insertAgentsIntoMobsim() {
        for (AVAgent agent : fleet.getAgents().values()) {
            qsim.insertAgentIntoMobsim(agent);
            qsim.addParkedVehicle(agent.getVehicle(), agent.getCurrentLinkId());
        }
    }
}
