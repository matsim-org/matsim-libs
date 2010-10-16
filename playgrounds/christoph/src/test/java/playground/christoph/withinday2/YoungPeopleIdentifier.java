package playground.christoph.withinday2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.interfaces.QLink;
import org.matsim.ptproject.qsim.interfaces.QSimI;
import org.matsim.ptproject.qsim.interfaces.QVehicle;

import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class YoungPeopleIdentifier extends DuringLegIdentifier {

	private QSimI queueSim;

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
		
		QLink tmpLink = queueSim.getQNetwork().getLinks().get(new IdImpl("6"));
//		Collection<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getVehQueue();
		Collection<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getAllNonParkedVehicles();

		// select agents, which should be replanned within this time step
		for (QLink link:queueSim.getQNetwork().getLinks().values()){
//			for (QVehicle vehicle : link.getVehQueue()) {
				for (QVehicle vehicle : link.getAllNonParkedVehicles()) {
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
	
	YoungPeopleIdentifier(QSimI queueSim) {
		this.queueSim = queueSim;
	}


}
