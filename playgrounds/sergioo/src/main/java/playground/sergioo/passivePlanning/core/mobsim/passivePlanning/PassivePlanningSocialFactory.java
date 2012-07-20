package playground.sergioo.passivePlanning.core.mobsim.passivePlanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.PersonHouseholdMapping;

import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents.PassivePlannerSocialAgentFactory;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents.PassivePlannerTransitSocialAgentFactory;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlanningSocialFactory implements MobsimFactory {

	//Constants
	private final static Logger log = Logger.getLogger(QSimFactory.class);

	//Attributes
	private final PassivePlannerManager passivePlannerManager;
	private final PersonHouseholdMapping personHouseholdMapping;
	private final IntermodalLeastCostPathCalculator leastCostPathCalculator;
	
	//Constructors
	/**
	 * @param personHouseholdMapping
	 * @param leastCostPathCalculator
	 * @param passivePlannerManager
	 */
	public PassivePlanningSocialFactory(PassivePlannerManager passivePlannerManager, PersonHouseholdMapping personHouseholdMapping, IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this.passivePlannerManager = passivePlannerManager;
		this.personHouseholdMapping = personHouseholdMapping;
		this.leastCostPathCalculator = leastCostPathCalculator;
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
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory;
		if (sc.getConfig().scenario().isUseTransit()) {
			if(parallel)
				agentFactory = new PassivePlannerTransitSocialAgentFactory(qSim, passivePlannerManager, personHouseholdMapping, leastCostPathCalculator);
			else
				agentFactory = new PassivePlannerTransitSocialAgentFactory(qSim, personHouseholdMapping, leastCostPathCalculator);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setUseUmlaeufe(true);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			if(parallel)
				agentFactory = new PassivePlannerSocialAgentFactory(qSim, passivePlannerManager, personHouseholdMapping, leastCostPathCalculator);
			else
				agentFactory = new PassivePlannerSocialAgentFactory(qSim, personHouseholdMapping, leastCostPathCalculator);
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
