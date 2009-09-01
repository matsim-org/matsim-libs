package playground.kai.usecases.mobsim;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Events;
import org.matsim.core.api.experimental.events.EventsBuilder;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;

public class Mobsim {
	private static final Logger log = Logger.getLogger(Mobsim.class);
	
	public Mobsim( Scenario sc, Events ev ) {
		EventsBuilder eb = ev.getBuilder();
		
		// finally, we need to be able to generate events:
		Id agentId = new IdImpl("agentId");
		Id linkId = new IdImpl("linkId");
		double time = 1. ;
		
		ActivityEndEvent aee = eb.createActivityEndEvent( time, agentId, linkId, "actType" ) ); 
		ev.processEvent( aee ) ;

		AgentDepartureEvent ade = eb.createAgentDepartureEvent( time, agentId, linkId ) ;

		AgentWait2LinkEvent aw2le = eb.createAgentWait2LinkEvent(time,agentId,linkId) ;

		LinkLeaveEvent lle = eb.createLinkLeaveEvent( time, agentId, linkId ) ;

		LinkEnterEvent lee = eb.createLinkEnterEvent( time, agentId, linkId ) ;

		AgentArrivalEvent aae = eb.createAgentArrivalEvent( time, agentId, linkId ) ;

		ActivityStartEvent ase = eb.createActivityStartEvent( time, agentId, linkId, "acttype" ) ;

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
