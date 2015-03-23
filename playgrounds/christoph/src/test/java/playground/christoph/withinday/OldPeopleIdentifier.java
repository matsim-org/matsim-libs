package playground.christoph.withinday;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;

public class OldPeopleIdentifier extends DuringActivityAgentSelector {

	private Netsim mobsim;

	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {

		Set<MobsimAgent> set = new HashSet<MobsimAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return set;
		}

		// select agents, which should be replanned within this time step
//		for (MobsimAgent pa : mobsim.getActivityEndsList()) {
////			PersonAgent agent = (PersonAgent) pa ;
////			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
//			if ( pa instanceof HasPerson ) {
//				Person person = ((HasPerson)pa).getPerson() ;
//				if ( ((PersonImpl)person).getAge() == 56 ) {
//					System.out.println("found agent");
//					set.add(pa);
//				}
//			}
//		}

		return set;
	}

	/*package*/ OldPeopleIdentifier(Netsim mobsim) {
		this.mobsim = mobsim;
	}

}
