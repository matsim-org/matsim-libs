package org.matsim.codeexamples.mobsim.replaceAgentFactory;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

import java.util.Map;

class MyAgentFactory implements AgentFactory {
	
	@Inject private QSim qsim;
	@Inject private TripRouter tripRouter; // injecting TripRouter at level of qsim fails ... no longer
	@Inject private Scenario scenario ;
	@Inject private Map<String,TravelTime> travelTimes ;
	
	@Override public MobsimAgent createMobsimAgentFromPerson( final Person p ) {
		final PersonDriverAgentImpl agent = new PersonDriverAgentImpl( p.getSelectedPlan(), qsim );
		return agent ;
	}
}
