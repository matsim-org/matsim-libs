package playground.christoph.withinday;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class OldPeopleIdentifier extends DuringActivityIdentifier {

	private Mobsim mobsim;

	@Override
	public Set<PersonAgent> getAgentsToReplan(double time, Id withinDayReplannerId) {

		Set<PersonAgent> set = new HashSet<PersonAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return set;
		}

		// select agents, which should be replanned within this time step
		for (PersonAgent agent : mobsim.getActivityEndsList()) {
			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
				System.out.println("found agent");
				set.add(agent);
			}
		}

		return set;
	}

	/*package*/ OldPeopleIdentifier(Mobsim mobsim) {
		this.mobsim = mobsim;
	}

}
