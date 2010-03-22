package playground.christoph.withinday;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QVehicle;

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
	public List<DriverAgent> getAgentsToReplan(double time,
			WithinDayReplanner withinDayReplanner) {
		
		ArrayList<DriverAgent> list = new ArrayList<DriverAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(time) !=  22000.0) {
			return list;
		}
		
		QLink tmpLink = queueSim.getQNetwork().getLinks().get(new IdImpl("6"));
		List<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getVehQueue();

		// select agents, which should be replanned within this time step
		for (QLink link:queueSim.getQNetwork().getLinks().values()){
			for (QVehicle vehicle : link.getVehQueue()) {
				DriverAgent agent=vehicle.getDriver();
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
