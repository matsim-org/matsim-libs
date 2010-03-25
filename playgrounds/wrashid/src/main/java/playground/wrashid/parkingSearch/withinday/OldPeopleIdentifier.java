package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.DriverAgent;
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
	public List<DriverAgent> getAgentsToReplan(double time,
			WithinDayReplanner withinDayReplanner) {

		ArrayList<DriverAgent> list = new ArrayList<DriverAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return list;
		}

		// select agents, which should be replanned within this time step
		for (DriverAgent agent : queueSim.getActivityEndsList()) {
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
