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
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.households.PersonHouseholdMapping;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda.PassivePlannerTransitAgendaAgentFactory;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social.PassivePlannerSocialAgentFactory;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlanningSocialFactory implements MobsimFactory {

	//Constants
	private final static Logger log = Logger.getLogger(PassivePlanningSocialFactory.class);

	//Attributes
	private final PassivePlannerManager passivePlannerManager;
	private final PersonHouseholdMapping personHouseholdMapping;
	
	//Constructors
	/**
	 * @param personHouseholdMapping
	 * @param tripRouter
	 * @param passivePlannerManager
	 */
	public PassivePlanningSocialFactory(PassivePlannerManager passivePlannerManager, PersonHouseholdMapping personHouseholdMapping, TripRouter tripRouter) {
		this.passivePlannerManager = passivePlannerManager;
		this.personHouseholdMapping = personHouseholdMapping;
	}

	//Methods
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
        // Get number of parallel Threads
        int numOfThreads = conf.getNumberOfThreads();
        QNetsimEngineFactory netsimEngFactory;
        boolean parallel = false;
        if (numOfThreads > 1) {
        	parallel = true;
            eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
            netsimEngFactory = new ParallelQNetsimEngineFactory();
            log.info("Using parallel QSim with " + numOfThreads + " threads.");
        } else {
            netsimEngFactory = new DefaultQSimEngineFactory();
        }
		QSim qSim = new QSim(sc, eventsManager);
		PlanningEngine planningEngine = new PlanningEngine(passivePlannerManager);
		qSim.addMobsimEngine(planningEngine);
		qSim.addDepartureHandler(planningEngine);
		PassivePlanningActivityEngine activityEngine = new PassivePlanningActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory;
		if(sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new PassivePlannerTransitAgendaAgentFactory(qSim, passivePlannerManager, personHouseholdMapping);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setUseUmlaeufe(true);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		}
		else
			agentFactory = new PassivePlannerSocialAgentFactory(qSim, passivePlannerManager, personHouseholdMapping);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
