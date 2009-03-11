package playground.kai.usecases.basicmentalmodule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.basic.v01.BasicNetwork;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.interfaces.basic.v01.BasicPopulationBuilder;
import org.matsim.interfaces.basic.v01.BasicRoute;
import org.matsim.interfaces.basic.v01.BasicScenario;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Coord;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.replanning.modules.StrategyModule;

@SuppressWarnings("unused")
public class MyModule implements
StrategyModule,
ActEndEventHandler,
AgentDepartureEventHandler,
AgentWait2LinkEventHandler,
LinkLeaveEventHandler,
LinkEnterEventHandler,
AgentArrivalEventHandler,
ActStartEventHandler
// TODO: names of these events handlers ok?
{
	private static final Logger log = Logger.getLogger(MyModule.class);
	
	BasicScenario sc ; 
	BasicNetwork<BasicNode,BasicLink> net ;
	BasicPopulation<BasicPerson> pop ;
	
	public MyModule(Controler controler) {
		
		sc = controler.getScenarioData() ; // TODO in controler
		net = sc.getNetwork() ;
		pop = sc.getPopulation() ;
		
	}
	
	public void init() { // initReplanning() 
		
		// go through network and copy to my personal network:
		for ( BasicNode bn : net.getNodes().values() ) {
			Id id = bn.getId();
			Coord coord = bn.getCoord(); 
		}
		for ( BasicLink bl : net.getLinks().values() ) {

			Id id = bl.getId() ;

			BasicNode fNode = bl.getFromNode();
			BasicNode tNode = bl.getToNode() ;
			double len = bl.getLength() ;

			double fs = bl.getFreespeed(0.) ; 
			double cap = bl.getCapacity(0.) ;
			double nLanes = bl.getNumberOfLanes(0.) ; // TODO: getNumberOfLanes??
			// TODO: also getters w/o time argument?  I think that would contribute to robustness ...			
			
		}
		
		// go through population and copy to my personal population:
		for ( BasicPerson person : pop.getPersons().values() ) {
			
			Id id = person.getId();
			
//			double age = person.getAge();
//			String carAvail = person.getCarAvail(); // TODO: String??
//			person.getDesires(); // TODO: Do we understand this well enough to have it in the basic interface? 

			List<BasicPlan> plans = person.getPlans() ;
			
			for ( BasicPlan plan : plans ) {
//				BasicPlanImpl.ActLegIterator it = plan.getIterator() ;
//				// TODO wie l�sen wir das?
//				
//				// TODO: is the following how it is meant?  not terribly beautiful.  But what else?
//
//				// TODO: Can you check if the first act exists?
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
	
	public void handlePlan(Plan ppp) { // need handlePlan(BasicPlan) ??????
		BasicPlan plan = (BasicPlan) ppp ;
		
		BasicPopulationBuilder pb = pop.getPopulationBuilder() ; 
		
		try {
			Id id = sc.createId("1") ; 
			BasicPerson person = pb.createPerson(id) ;
			// (can't be used at this level, but useful anyways)
			
			BasicPlan newPlan = pb.createPlan(person) ; // replace (??) the plan by a completely new plan
			person.addPlan(newPlan) ; // now the person has the plan twice. 
			
			Coord coord = sc.createCoord(1.,1.) ;
			Id linkId = sc.createId("2" ) ;
			Id facId = sc.createId("3") ;
			
			// BasicLocation loc = pb.createFacility( coord ) ; // currently not possible (ok at this level)
			
			BasicLink link ;
//			BasicAct hAct = pb.createAct( "home", link ) ; // does not really make sense since there may be more than one facility at link
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
			
			BasicLeg leg = pb.createLeg(plan, BasicLeg.Mode.bike) ;
			plan.addLeg( leg ) ;
			
			List<Id> routeIdList = new ArrayList<Id>() ;
			routeIdList.add(id) ; routeIdList.add(id) ;
			BasicRoute route = pb.createRoute(id, id, routeIdList ) ;
			leg.setRoute(route) ;
			
//			BasicLink link ;
//			BasicAct wAct = pb.createAct("work") ;
//			plan.addAct( wAct ) ;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

	public void handleEvent(ActEndEvent event) {
		String str = event.getEventType(); // TODO: String?  Not an enum??
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

	public void handleEvent(ActStartEvent event) {
	}

	public void reset(int iteration) {
	}

	public void finish() {		
	}

}
