package playground.christoph.withinday;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.population.PersonUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

public class YoungPeopleIdentifier extends DuringLegAgentSelector {

	private Netsim mobsim;

	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		Set<MobsimAgent> set = new HashSet<MobsimAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(time) !=  22000.0) {
			return set;
		}
		
		NetsimLink tmpLink = mobsim.getNetsimNetwork().getNetsimLinks().get(Id.create("6", Link.class));
//		Collection<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(Id.create("6")).getVehQueue();
		Collection<MobsimVehicle> tmpList=mobsim.getNetsimNetwork().getNetsimLinks().get(Id.create("6", Link.class)).getAllNonParkedVehicles();

		// select agents, which should be replanned within this time step
		for (NetsimLink link:mobsim.getNetsimNetwork().getNetsimLinks().values()){
//			for (QVehicle vehicle : link.getVehQueue()) {
				for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
				MobsimDriverAgent agent=vehicle.getDriver();
				System.out.println(agent.getId());
				if (PersonUtils.getAge(((HasPerson) agent).getPerson()) == 18) {
					System.out.println("found agent");
					set.add(agent);
				}
			}
		}

		return set;
	}
	
	/*package*/ YoungPeopleIdentifier(Netsim mobsim) {
		this.mobsim = mobsim;
	}


}
