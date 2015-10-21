package playground.kai.usecases.ownmobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.facilities.ActivityFacility;

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

		Id<Person> agentId = Id.create("agentId", Person.class);
		Id<Link> linkId = Id.create("linkId", Link.class);
		Id<ActivityFacility> facilityId = Id.create("facilityId", ActivityFacility.class);
		String legMode = TransportMode.car;
		double time = 1. ;

		ActivityEndEvent aee = new ActivityEndEvent(time, agentId, linkId, facilityId, "actType") ;
		ev.processEvent( aee ) ;

		PersonDepartureEvent ade = new PersonDepartureEvent(time, agentId, linkId, legMode) ;

		Wait2LinkEvent aw2le = new Wait2LinkEvent(time, agentId, linkId, null, legMode, 1.0) ;

		LinkLeaveEvent lle = new LinkLeaveEvent(time, agentId, linkId, null) ;

		LinkEnterEvent lee = new LinkEnterEvent(time, agentId, linkId, null) ;

		PersonArrivalEvent aae = new PersonArrivalEvent(time, agentId, linkId, legMode) ;

		ActivityStartEvent ase = new ActivityStartEvent(time, agentId, linkId, facilityId, "acttype") ;

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
