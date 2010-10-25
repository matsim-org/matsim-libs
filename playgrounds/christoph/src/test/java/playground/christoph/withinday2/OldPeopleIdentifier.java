package playground.christoph.withinday2;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

public class OldPeopleIdentifier extends DuringActivityIdentifier {

	private Mobsim queueSim;

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

	public OldPeopleIdentifier(Mobsim queueSim) {
		this.queueSim = queueSim;
	}

}