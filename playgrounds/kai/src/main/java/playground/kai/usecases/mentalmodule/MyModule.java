package playground.kai.usecases.mentalmodule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;

@SuppressWarnings("unused")
public class MyModule implements
PlanStrategyModule,
ActivityEndEventHandler,
AgentDepartureEventHandler,
AgentWait2LinkEventHandler,
LinkLeaveEventHandler,
LinkEnterEventHandler,
AgentArrivalEventHandler,
ActivityStartEventHandler
// TODO: names of these events handlers ok?
{
	private static final Logger log = Logger.getLogger(MyModule.class);

	ScenarioImpl sc;
	NetworkLayer net;
	Population pop;

	public MyModule(Controler controler) {

		this.sc = controler.getScenario() ; // TODO in controler
		this.net = this.sc.getNetwork() ;
		this.pop = this.sc.getPopulation() ;

	}

	public void prepareReplanning() { // initReplanning()

		// go through network and copy to my personal network:
		for ( Node bn : this.net.getNodes().values() ) {
			Id id = bn.getId();
			Coord coord = bn.getCoord();
		}
		for ( Link bl : this.net.getLinks().values() ) {

			Id id = bl.getId() ;

			Node fNode = bl.getFromNode();
			Node tNode = bl.getToNode();
			double len = bl.getLength();

			double fs = bl.getFreespeed();
			double cap = bl.getCapacity();
			double nLanes = bl.getNumberOfLanes();
		}

		// go through population and copy to my personal population:
		for ( Person pp : this.pop.getPersons().values() ) {
			PersonImpl person = (PersonImpl) pp;

			Id id = person.getId();

			double age = person.getAge();
			String carAvail = person.getCarAvail(); // TODO: String??
			person.getDesires(); // TODO: Do we understand this well enough to have it in the basic interface?

			List<Plan> plans = person.getPlans() ;

			for ( Plan plan : plans ) {
//				BasicPlanImpl.ActLegIterator it = plan.getIterator() ;
				// TODO ActLegIterator not in the basic interfaces

				// TODO: is the following how it is meant?  not terribly beautiful.  But what else?

				// TODO: Can you check if the first act exists?
//				BasicActivity act = it.nextAct();
//				Coord coord = act.getCoord();
//				double sTime = act.getStartTime() ;
//				double eTime = act.getEndTime() ;
//				Id fId = act.getFacilityId() ;
//				Id lId = act.getLinkId() ;
//				String type = act.getType() ;
//
//				while ( it.hasNextLeg() ) {
//					BasicLeg leg = it.nextLeg();
//					double dTime = leg.getDepartureTime();
////					double aTime = leg.getArrivalTime() ;
//					double tTime = leg.getTravelTime() ;
//
//					BasicLeg.Mode mode = leg.getMode() ;
//
//					BasicRoute route = leg.getRoute();
//
//					double dist = route.getDistance();
//					double ttime = route.getTravelTime() ;
//					Id slId = route.getStartLinkId() ;
//					Id elId = route.getEndLinkId() ;
//
//					List<Id> linkIds = route.getLinkIds() ;
//
//					BasicActivity nextAct = it.nextAct();
//				}
			}
		}
	}

	public void handlePlan(Plan plan) {

		PopulationFactory pb = this.pop.getFactory() ;



		try {
			Id id = this.sc.createId("1") ;
//			Person person = pb.createPerson(id) ;
			// (can't be used at this level, but useful anyways)

//			plan = pb.createPlan(person) ; // replace the plan by a completely new plan
//			person.addPlan(plan) ; // now the person has the plan twice.  to be clarified

			Coord coord = this.sc.createCoord(1.,1.) ;
			Id linkId = this.sc.createId("2" ) ;
			Id facId = this.sc.createId("3") ;

//			BasicLocation loc = pb.createFacility( coord ) ;

			Link link ;
//			BasicAct hAct = pb.createAct( "home", link ) ;
//
//			BasicFacility fac ;
//			BasicAct h2Act = pb.createAct( "home", fac ) ;
//
//			BasicAct h3Act = pb.createAct( "home", coord ) ;

//			BasicAct hAct = pb.createAct( "home" ) ;
//			hAct.setCoord( coord ) ;
//			hAct.setLink ( link ) ;
//			hAct.setFacility( fac ) ;
//			plan.addAct( hAct ) ;

			Leg leg = pb.createLeg(TransportMode.bike) ;
			plan.addLeg( leg ) ;

			List<Id> routeIdList = new ArrayList<Id>() ;
			routeIdList.add(id) ; routeIdList.add(id) ;
//			BasicRoute route = pb.createRoute(id, id, routeIdList ) ;
//			leg.setRoute(route) ;

//			BasicLink link ;
//			BasicAct wAct = pb.createAct("work") ;
//			plan.addAct( wAct ) ;

		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	public void handleEvent(ActivityEndEvent event) {
//		String str = event.getEventType(); // TODO: String?  Not an enum??
		Map<String,String> attribs = event.getAttributes() ; // TODO: String?  may be ok ...
	}

	public void handleEvent(AgentDepartureEvent event) {
	}

	public void handleEvent(AgentWait2LinkEvent event) {
	}

	public void handleEvent(LinkLeaveEvent event) {
	}

	public void handleEvent(LinkEnterEvent event) {
	}

	public void handleEvent(AgentArrivalEvent event) {
	}

	public void handleEvent(ActivityStartEvent event) {
	}

	public void reset(int iteration) {
	}

	public void finishReplanning() {
	}

}
