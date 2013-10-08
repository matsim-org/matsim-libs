package playground.kai.usecases.ownmobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;

public class Mobsim {
	private static final Logger log = Logger.getLogger(Mobsim.class);

	private Scenario sc ;
	private EventsManager ev ;

	public Mobsim( Scenario sc, EventsManager ev ) {
		this.sc = sc ;
		this.ev = ev ;
	}

	public void run() {
		// getting the network info should be w/o problems

		// getting the plans info should be w/o problems

		// the following tests the events generation
		EventsFactory eb = this.ev.getFactory();

		Id agentId = sc.createId("agentId");
		Id linkId = sc.createId("linkId");
		Id facilityId = sc.createId("facilityId");
		String legMode = TransportMode.car;
		double time = 1. ;

		ActivityEndEvent aee = eb.createActivityEndEvent( time, agentId, linkId, facilityId, "actType" ) ;
		ev.processEvent( aee ) ;

		PersonDepartureEvent ade = eb.createAgentDepartureEvent( time, agentId, linkId, legMode ) ;

		Wait2LinkEvent aw2le = eb.createAgentWait2LinkEvent(time,agentId,linkId, null) ;

		LinkLeaveEvent lle = eb.createLinkLeaveEvent( time, agentId, linkId, null ) ;

		LinkEnterEvent lee = eb.createLinkEnterEvent( time, agentId, linkId, null ) ;

		PersonArrivalEvent aae = eb.createAgentArrivalEvent( time, agentId, linkId, legMode ) ;

		ActivityStartEvent ase = eb.createActivityStartEvent( time, agentId, linkId, facilityId, "acttype" ) ;

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
