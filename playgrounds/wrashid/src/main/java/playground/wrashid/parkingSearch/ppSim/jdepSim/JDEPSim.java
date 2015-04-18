package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;

import playground.wrashid.parkingSearch.ppSim.ttmatrix.DummyTTMatrix;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public class JDEPSim implements RunnableMobsim{

	private Scenario sc;
	private EventsManager eventsManager;

	public JDEPSim(Scenario sc, EventsManager eventsManager){
		this.sc = sc;
		this.eventsManager = eventsManager;
	}
	
	@Override
	public void run() {
		Message.messageQueue=new MessageQueue();
		Message.eventsManager=eventsManager;
		
		
		for (Person p:sc.getPopulation().getPersons().values()){
			new AgentEventMessage(p);
		}
		
		Message.messageQueue.startSimulation();
	}
	
}
