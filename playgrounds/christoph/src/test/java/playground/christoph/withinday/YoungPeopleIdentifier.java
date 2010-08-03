package playground.christoph.withinday;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.interfaces.QVehicle;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;

import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class YoungPeopleIdentifier extends DuringLegIdentifier {

	private WithinDayQSim queueSim;

	@Override
	public AgentsToReplanIdentifier clone() {
		return this;
	}

	@Override
	public List<PersonAgent> getAgentsToReplan(double time,
			WithinDayReplanner withinDayReplanner) {
		
		ArrayList<PersonAgent> list = new ArrayList<PersonAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(time) !=  22000.0) {
			return list;
		}
		
		QLinkInternalI tmpLink = queueSim.getQNetwork().getLinks().get(new IdImpl("6"));
		List<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getVehQueue();

		// select agents, which should be replanned within this time step
		for (QLinkInternalI link:queueSim.getQNetwork().getLinks().values()){
			for (QVehicle vehicle : link.getVehQueue()) {
				PersonDriverAgent agent=vehicle.getDriver();
				System.out.println(agent.getPerson().getId());
				if (((PersonImpl) agent.getPerson()).getAge() == 18) {
					System.out.println("found agent");
					list.add(agent);
				}
			}
		}

		return list;
	}
	
	public YoungPeopleIdentifier(WithinDayQSim queueSim) {
		this.queueSim = queueSim;
	}


}
