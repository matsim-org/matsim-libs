package playground.gregor.sim2d_v2.simulation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;

public class HybridQ2DMobsimFactory implements MobsimFactory {

	private final static Logger log = Logger.getLogger(HybridQ2DMobsimFactory.class);
	
	Sim2DEngine sim2DEngine = null;

	@Override
	public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {

		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		QNetsimEngineFactory netsimEngFactory;
		if (numOfThreads > 1) {
			eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
			netsimEngFactory = new ParallelQNetsimEngineFactory();
			log.info("Using parallel QSim with " + numOfThreads + " threads.");
		} else {
			netsimEngFactory = new DefaultQSimEngineFactory();
		}
		QSim qSim = new QSim(sc, eventsManager, netsimEngFactory);
		AgentFactory agentFactory;

		if (!sc.getConfig().controler().getMobsim().equals("hybridQ2D")) {
			throw new RuntimeException("This factory does not make sense for " + sc.getConfig().controler().getMobsim()  );
		} else {
			agentFactory = new Sim2DAgentFactory(qSim,sc);
		}

		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		Sim2DEngine e = new Sim2DEngine(qSim);
		this.sim2DEngine = e;
		qSim.addMobsimEngine(e);
		Sim2DDepartureHandler d = new Sim2DDepartureHandler(e);
		qSim.addDepartureHandler(d);
		return qSim;
	}

	public Sim2DEngine getSim2DEngine() {
		return this.sim2DEngine;
	}
}
