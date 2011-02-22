package playground.christoph.withinday;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class OldPeopleIdentifier extends DuringActivityIdentifier {

	private Netsim mobsim;

	@Override
	public Set<WithinDayAgent> getAgentsToReplan(double time, Id withinDayReplannerId) {

		Set<WithinDayAgent> set = new HashSet<WithinDayAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return set;
		}

		// select agents, which should be replanned within this time step
		for (PlanAgent pa : mobsim.getActivityEndsList()) {
			PersonAgent agent = (PersonAgent) pa ;
			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
				System.out.println("found agent");
				set.add((WithinDayAgent)agent);
			}
		}

		return set;
	}

	/*package*/ OldPeopleIdentifier(Netsim mobsim) {
		this.mobsim = mobsim;
	}

}
