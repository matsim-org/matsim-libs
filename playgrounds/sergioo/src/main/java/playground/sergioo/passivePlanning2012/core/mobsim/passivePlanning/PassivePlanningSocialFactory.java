package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.router.TripRouter;
import org.matsim.households.PersonHouseholdMapping;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social.PassivePlannerSocialAgentFactory;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social.PassivePlannerTransitSocialAgentFactory;
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
		QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		QSim qSim = new QSim(sc, eventsManager);
		PlanningEngine planningEngine = new PlanningEngine(qSim);
		passivePlannerManager.setPlanningEngine(planningEngine);
		qSim.addMobsimEngine(planningEngine);
		qSim.addDepartureHandler(planningEngine);
		PassivePlanningActivityEngine activityEngine = new PassivePlanningActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim);
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory;
		if(sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new PassivePlannerTransitSocialAgentFactory(qSim, passivePlannerManager, personHouseholdMapping);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
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
