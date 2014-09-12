package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.router.TripRouter;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda.PassivePlannerAgendaAgentFactory;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda.PassivePlannerTransitAgendaAgentFactory;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlanningAgendaFactory implements MobsimFactory {

	//Constants
	private final static Logger log = Logger.getLogger(PassivePlanningAgendaFactory.class);

	//Attributes
	private final PassivePlannerManager passivePlannerManager;

	//Constructors
	/**
	 * @param personHouseholdMapping
	 * @param tripRouter
	 * @param passivePlannerManager
	 */
	public PassivePlanningAgendaFactory(PassivePlannerManager passivePlannerManager, TripRouter tripRouter) {
		this.passivePlannerManager = passivePlannerManager;
	}

	//Methods
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().qsim();
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
            netsimEngFactory = new DefaultQNetsimEngineFactory();
        }
		QSim qSim = new QSim(sc, eventsManager);
		PassivePlanningActivityEngine activityEngine = new PassivePlanningActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		PlanningEngine planningEngine = new PlanningEngine(qSim);
		passivePlannerManager.setPlanningEngine(planningEngine);
		qSim.addMobsimEngine(planningEngine);
		qSim.addDepartureHandler(planningEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory;
		if(sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new PassivePlannerTransitAgendaAgentFactory(qSim, passivePlannerManager);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		}
		else
			agentFactory = new PassivePlannerAgendaAgentFactory(qSim, passivePlannerManager);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
