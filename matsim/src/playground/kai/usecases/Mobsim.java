package playground.kai.usecases;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.network.BasicNetwork;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;

@SuppressWarnings("unused")
public class Mobsim {
	private static final Logger log = Logger.getLogger(Mobsim.class);
	
	public Mobsim( BasicNetwork<BasicNode, BasicLink> net , BasicPopulation<BasicPerson> pop , EventsImpl eve ) {
		// TODO All the events stuff is not behind interfaces ...
		
		// for network, pop see MentalModule ...
		
			
		// finally, we need to be able to generate events:
		Id agentId = new IdImpl("agentId");
		Id linkId = new IdImpl("linkId");
		double time = 1. ;
		ActivityEndEventImpl aee = new ActivityEndEventImpl(time, agentId, linkId, "actType" ) ;
		eve.processEvent( aee ) ;

		
		int legNumber = 1 ;
		AgentDepartureEventImpl ade = new AgentDepartureEventImpl( time, agentId, linkId ) ;

		AgentWait2LinkEventImpl aw2le = new AgentWait2LinkEventImpl(time,agentId,linkId) ;

		LinkLeaveEventImpl lle = new LinkLeaveEventImpl( time, agentId, linkId ) ;

		LinkEnterEventImpl lee = new LinkEnterEventImpl( time, agentId, linkId ) ;

		AgentArrivalEventImpl aae = new AgentArrivalEventImpl( time, agentId, linkId ) ;

		ActivityStartEventImpl ase = new ActivityStartEventImpl( time, agentId, linkId, "acttype" ) ;

		// TODO: None of this is behind interfaces.  Needed if we want to accept "external" mobsims.  Do we want that?
		// If so, we would need to be sure that we want to maintain the create methods.
		// (Since ctors cannot be in interfaces, would need to replace them by create methods.)


		// Using typed constructors means that an external mobsim writer needs to maintain the 
		// BasicPersons & BasicLinks, because those cannot be generated from the interfaces but would need to be 
		// maintained.

		// TODO: Also: If we want to allow external mobsim writers access to snapshot writers etc., we need even
		// more interfaces.

		// Looks fairly hopeless for the time being ...
			
	}
	
}
