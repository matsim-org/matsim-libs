package playground.matsim_decoupling.qsim;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.util.Modules;

import ch.ethz.matsim.av.config.AVConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatchmentListener;
import ch.ethz.matsim.av.framework.AVQSimModule;
import ch.ethz.matsim.av.schedule.AVOptimizer;
import playground.clruch.traveltimetracker.AVTravelTimeRecorder;

/**
 * TODO: Eventually this should not need to be reimplemented, but the AVQSimProvider should
 * be more flexible!
 * 
 * @author sebastian
 */
public class IDSCQSimProvider implements Provider<Mobsim> {
	@Inject
	private EventsManager eventsManager;
	@Inject
	private Collection<AbstractQSimPlugin> plugins;
	@Inject
	private Scenario scenario;

	@Inject
	private Injector injector;
	@Inject
	private AVConfig avconfig;

	@Override
	public Mobsim get() {
		QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);
		Injector childInjector = injector
				.createChildInjector(Modules.override(new AVQSimModule(avconfig, qSim)).with(new IDSCQSimModule(qSim)));

		qSim.addQueueSimulationListeners(childInjector.getInstance(AVOptimizer.class));
		qSim.addQueueSimulationListeners(childInjector.getInstance(AVDispatchmentListener.class));
		qSim.addQueueSimulationListeners(childInjector.getInstance(AVTravelTimeRecorder.class));

		qSim.addMobsimEngine(childInjector.getInstance(PassengerEngine.class));
		qSim.addDepartureHandler(childInjector.getInstance(PassengerEngine.class));
		qSim.addAgentSource(childInjector.getInstance(VrpAgentSource.class));

		return qSim;
	}
}
