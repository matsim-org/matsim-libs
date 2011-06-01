package playground.michalm.vrp.sim;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.ptproject.qsim.*;

import pl.poznan.put.vrp.cvrp.data.*;
import playground.michalm.vrp.data.*;


public class VRPSimFactory
    implements MobsimFactory
{
    private final MATSimVRPData data;
    private final AlgorithmParams algParams;
    private List<SimulationListener> simListeners = new ArrayList<SimulationListener>();


    public VRPSimFactory(MATSimVRPData data, AlgorithmParams algParams)
    {
        this.data = data;
        this.algParams = algParams;
    }


    @Override
    public Simulation createMobsim(Scenario sc, EventsManager eventsManager)
    {
        QSim sim = new QSim(sc, eventsManager);
        sim.setAgentFactory(new VRPAgentFactory(sim, data));

        VRPSimEngine vrpSimEngine = new VRPSimEngine(sim, data, algParams);
        data.setVrpSimEngine(vrpSimEngine);
        sim.addMobsimEngine(vrpSimEngine);

        for (SimulationListener listener : simListeners) {
            sim.addQueueSimulationListeners(listener);
        }

        return sim;
    }


    public void addSimulationListener(SimulationListener listener)
    {
        simListeners.add(listener);
    }


    public void removeSimulationListener(SimulationListener listener)
    {
        simListeners.remove(listener);
    }
}
