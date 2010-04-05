package playground.christoph.withinday;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.QSim;

import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class OldPeopleIdentifier extends DuringActivityIdentifier {

	private WithinDayQSim queueSim;

	@Override
	public AgentsToReplanIdentifier clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersonDriverAgent> getAgentsToReplan(double time,
			WithinDayReplanner withinDayReplanner) {

		ArrayList<PersonDriverAgent> list = new ArrayList<PersonDriverAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return list;
		}

		// select agents, which should be replanned within this time step
		for (PersonDriverAgent agent : queueSim.getActivityEndsList()) {
			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
				System.out.println("found agent");
				list.add(agent);
			}
		}

		return list;
	}

	public OldPeopleIdentifier(WithinDayQSim queueSim) {
		this.queueSim = queueSim;
	}

}
