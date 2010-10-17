package playground.christoph.withinday2;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.interfaces.QSimI;

import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class OldPeopleIdentifier extends AgentsToReplanIdentifier {

	private QSimI queueSim;

	@Override
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {

		ArrayList<PersonAgent> list = new ArrayList<PersonAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return list;
		}

		// select agents, which should be replanned within this time step
		for (PersonAgent agent : queueSim.getActivityEndsList()) {
			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
				System.out.println("found agent");
				list.add(agent);
			}
		}

		return list;
	}

	public OldPeopleIdentifier( QSimI queueSim) {
		this.queueSim = queueSim;
	}

	@Override
	public AgentsToReplanIdentifier clone() {
		throw new UnsupportedOperationException() ;
	}

}
