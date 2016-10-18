package playground.sebhoerl.avtaxi.framework;

import java.util.Collection;

import com.google.inject.Injector;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Inject;

import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatchmentListener;
import playground.sebhoerl.avtaxi.schedule.AVOptimizer;

public class AVQSimProvider implements Provider<Mobsim> {
    @Inject private EventsManager eventsManager;
    @Inject private Collection<AbstractQSimPlugin> plugins;
    @Inject private Scenario scenario;

    @Inject private Injector injector;
    @Inject private AVConfig config;

    @Override
    public Mobsim get() {
        QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);
        Injector childInjector = injector.createChildInjector(new AVQSimModule(config, qSim));

        qSim.addQueueSimulationListeners(childInjector.getInstance(AVOptimizer.class));
        qSim.addQueueSimulationListeners(childInjector.getInstance(AVDispatchmentListener.class));

        qSim.addMobsimEngine(childInjector.getInstance(PassengerEngine.class));
        qSim.addDepartureHandler(childInjector.getInstance(PassengerEngine.class));
        qSim.addAgentSource(childInjector.getInstance(VrpAgentSource.class));

        return qSim;
    }
}
