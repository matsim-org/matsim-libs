package playground.christoph.withinday;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.QVehicle;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

public class YoungPeopleIdentifier extends DuringLegIdentifier {

	private Mobsim mobsim;

	@Override
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {
		
		ArrayList<PersonAgent> list = new ArrayList<PersonAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(time) !=  22000.0) {
			return list;
		}
		
		NetsimLink tmpLink = mobsim.getNetsimNetwork().getNetsimLinks().get(new IdImpl("6"));
//		Collection<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getVehQueue();
		Collection<QVehicle> tmpList=mobsim.getNetsimNetwork().getNetsimLinks().get(new IdImpl("6")).getAllNonParkedVehicles();

		// select agents, which should be replanned within this time step
		for (NetsimLink link:mobsim.getNetsimNetwork().getNetsimLinks().values()){
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
	
	/*package*/ YoungPeopleIdentifier(Mobsim mobsim) {
		this.mobsim = mobsim;
	}


}
