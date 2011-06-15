package playground.christoph.withinday2;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class OldPeopleIdentifier extends DuringActivityIdentifier {

	private Netsim queueSim;

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {

		Set<PlanBasedWithinDayAgent> set = new HashSet<PlanBasedWithinDayAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return set;
		}

		// select agents, which should be replanned within this time step
		for (MobsimAgent pa : queueSim.getActivityEndsList()) {
//			PersonAgent agent = (PersonAgent) pa ;
//			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
			if ( pa instanceof HasPerson ) {
				Person person = ((HasPerson)pa).getPerson() ; 
				// this cast is ok (in my view), since not every MobsimAgent is a Person. kai, jun'11 
				
				if ( ((PersonImpl)person).getAge()==56 ) {
					// this cast is doubtful.  matsim should discuss how it wants to approach this. kai, jun'11 
					
					System.out.println("found agent");
					set.add((PlanBasedWithinDayAgent)pa);
				}
			}
		}

		return set;
	}

	public OldPeopleIdentifier(Netsim queueSim) {
		this.queueSim = queueSim;
	}

}