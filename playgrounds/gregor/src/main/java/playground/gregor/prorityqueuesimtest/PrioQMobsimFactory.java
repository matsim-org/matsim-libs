package playground.gregor.prorityqueuesimtest;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;



public class PrioQMobsimFactory implements MobsimFactory {
	private final static Logger log = Logger.getLogger(PrioQMobsimFactory.class);
	
	PrioQEngine sim2DEngine = null;

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

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
		QSim qSim = QSim.createQSimWithDefaultEngines(sc, eventsManager, netsimEngFactory);
		AgentFactory agentFactory;

		if (!sc.getConfig().controler().getMobsim().equals("prioQ")) {
			throw new RuntimeException("This factory does not make sense for " + sc.getConfig().controler().getMobsim()  );
		} else {
			agentFactory = new PrioQAgentFactory(qSim,sc);
		}

		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		PrioQEngine e = new PrioQEngine(qSim);
		this.sim2DEngine = e;
		qSim.addMobsimEngine(e);
		PrioQDepartureHandler d = new PrioQDepartureHandler(e);
		qSim.addDepartureHandler(d);
		
		
//		//DEBUG
//		QLinkControl contr = new QLinkControl(qSim);
//		qSim.addMobsimEngine(contr);
//		//END DEBUG
		
		return qSim;
	}

	public PrioQEngine getSim2DEngine() {
		return this.sim2DEngine;
	}

}
