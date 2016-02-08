package matsimConnector.engine;

import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.CAQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;


public class CAMobsimFactory implements MobsimFactory{

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

		if (!sc.getConfig().controler().getMobsim().equals(Constants.CA_MOBSIM_MODE)) {
			throw new RuntimeException("This factory does not make sense for " + sc.getConfig().controler().getMobsim()  );
		}
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		CAAgentFactory agentFactoryCA = new CAAgentFactory();
		CAEngine engineCA = new CAEngine(qSim,agentFactoryCA);
		
		qSim.addMobsimEngine(engineCA);
		
		ActivityEngine activityEngine = new ActivityEngine(eventsManager);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
	
		CAQNetworkFactory networkFactoryCA = new CAQNetworkFactory(engineCA,sc, agentFactoryCA);
		
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, networkFactoryCA);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		if (sc.getConfig().network().isTimeVariantNetwork()) 
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		
		return qSim;
	}
}
