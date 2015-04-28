package playground.wrashid.parkingSearch.ppSim.jdepSim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;

public class JDEPSim implements Mobsim {

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
