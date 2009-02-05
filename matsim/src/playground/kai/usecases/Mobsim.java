package playground.kai.usecases;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.*;
import org.matsim.events.*;
import org.matsim.interfaces.basic.v01.*;
import org.matsim.utils.geometry.Coord;

@SuppressWarnings("unused")
public class Mobsim {
	private static final Logger log = Logger.getLogger(Mobsim.class);
	
	public Mobsim( BasicNet<BasicNode, BasicLink> net , BasicPopulation<BasicPerson> pop , Events eve ) {
		// TODO All the events stuff is not behind interfaces ...
		
		// for network, pop see MentalModule ...
		
			
		// finally, we need to be able to generate events:
		double time = 1. ;
		ActEndEvent aee = new ActEndEvent(time,"agentId","linkId","actType" ) ;
		eve.processEvent( aee ) ;

		int legNumber = 1 ;
		AgentDepartureEvent ade = new AgentDepartureEvent( time, "agentId", "linkId", legNumber ) ;

		AgentWait2LinkEvent aw2le = new AgentWait2LinkEvent(time,"agentId","linkId",legNumber) ;

		LinkLeaveEvent lle = new LinkLeaveEvent( time, "agentId", "linkId", legNumber ) ;

		LinkEnterEvent lee = new LinkEnterEvent( time, "agentId", "linkId", legNumber ) ;

		AgentArrivalEvent aae = new AgentArrivalEvent( time, "agentId", "linkId", legNumber ) ;

		ActStartEvent ase = new ActStartEvent( time, "agentId", "linkId", "acttype" ) ;

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
