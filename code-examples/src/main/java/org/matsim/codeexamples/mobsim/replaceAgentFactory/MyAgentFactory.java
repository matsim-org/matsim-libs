package org.matsim.codeexamples.mobsim.replaceAgentFactory;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.utils.timing.TimeInterpretation;

class MyAgentFactory implements AgentFactory {
	
	@Inject private QSim qsim;
	@Inject private TimeInterpretation timeInterpretation;
	
	@Override public MobsimAgent createMobsimAgentFromPerson( final Person p ) {

		return new PersonDriverAgentImpl( p.getSelectedPlan(), qsim , timeInterpretation);
	}
}
