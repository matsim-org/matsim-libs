package playground.kai.usecases.basicmentalmodule;

import java.util.ArrayList;
import java.util.List;

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
import org.matsim.interfaces.basic.v01.BasicScenario;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.facilities.BasicFacilities;
import org.matsim.interfaces.basic.v01.facilities.BasicFacility;
import org.matsim.interfaces.basic.v01.network.BasicLink;
import org.matsim.interfaces.basic.v01.network.BasicNetwork;
import org.matsim.interfaces.basic.v01.network.BasicNode;
import org.matsim.interfaces.basic.v01.population.BasicActivity;
import org.matsim.interfaces.basic.v01.population.BasicLeg;
import org.matsim.interfaces.basic.v01.population.BasicPerson;
import org.matsim.interfaces.basic.v01.population.BasicPlan;
import org.matsim.interfaces.basic.v01.population.BasicPlanElement;
import org.matsim.interfaces.basic.v01.population.BasicPopulation;
import org.matsim.interfaces.basic.v01.population.BasicPopulationBuilder;
import org.matsim.interfaces.basic.v01.population.BasicRoute;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.PlanStrategyModule;


@SuppressWarnings("unused")
public class MyModule implements
PlanStrategyModule,
ActEndEventHandler,
AgentDepartureEventHandler,
AgentWait2LinkEventHandler,
LinkLeaveEventHandler,
LinkEnterEventHandler,
AgentArrivalEventHandler,
ActStartEventHandler
{
	private static final Logger log = Logger.getLogger(MyModule.class);
	
	BasicScenario sc ; 
	BasicNetwork<BasicNode,BasicLink> net ;
	BasicPopulation<BasicPerson<BasicPlan>> pop ;
	BasicFacilities facs ; // TODO: nicht konsequenterweise BasicFacilities<BasicFacility> ??  Maybe low prio.
	
	public MyModule(Controler controler) {
		
		sc = controler.getScenarioData() ; // TODO in controler
		net = sc.getNetwork() ;
		pop = sc.getPopulation() ;
		facs = sc.getFacilities() ;
		
	}
	
	public void prepareReplanning() { // initReplanning() 
		
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
			double nLanes = bl.getNumberOfLanes(0.) ; 
			// TODO: also getters w/o time argument?  I think that would contribute to robustness ...
			// But probably low prio.
			
		}
		
		// go through population and copy to my personal population:
		for ( BasicPerson person : pop.getPersons().values() ) {
			
			Id id = person.getId();
			
			List<BasicPlan> plans = person.getPlans() ;
			
			for ( BasicPlan plan : plans ) {
				for ( BasicPlanElement oo : plan.getPlanElements() ) {
					if ( oo instanceof BasicActivity ) {
						BasicActivity act = (BasicActivity) oo ;
						act.getCoord() ; // deprecated ????
						act.getEndTime() ;
						Id facId = act.getFacilityId();
						act.getLinkId(); // stays here so mobsim can be run w/o facilities
						act.getStartTime();
						act.getType();
						
						BasicFacility fac = facs.getFacilities().get( facId ) ;
						fac.getCoord();
						fac.getActivityOptions();
						fac.getLinkId();
						
					} else if ( oo instanceof BasicLeg ) {
						BasicLeg leg = (BasicLeg) oo ;
						leg.getDepartureTime();
						leg.getMode();
						leg.getRoute();
						leg.getTravelTime();
					}
				}
			}

		}
	}
	
	public void handlePlan(Plan ppp) { // need handlePlan(BasicPlan) ??????
		BasicPlan plan = ppp ;
		
		BasicPopulationBuilder pb = pop.getPopulationBuilder() ; 
		
		try {
			Id id = sc.createId("1") ; 

			BasicPerson<BasicPlan> person = pb.createPerson(id) ;
			pop.getPersons().put(id,person);
			
			// (can't be used at this level, but useful anyways)
			// FIXME: createAndAddPerson ????
			
			BasicPlan newPlan = pb.createPlan(person) ; // replace (??) the plan by a completely new plan
			// FIXME: This creational method has the side effect of also adding the created Object.  In my view:
			// - either createAndAddPlan
			// - or createPlan w/o side effects
			
//			person.addPlan(newPlan) ; // now the person has the plan twice.
			
			BasicActivity act = null ;
			
			// construct activity from coord:
			Coord coord = sc.createCoord(1.,1.) ;
//			act = pb.createActivityFromCoord( "home", coord ) ; // FIXME
			
			// construct activity from link:
//			act = pb.createActivityFromLinkId( "home", id ) ; // FIXME
			
			// construct activity from facility:
//			act = pb.createActivityFromFacilityId( "home", id ) ; // FIXME
			
			act.setEndTime(123.);
			act.setFacilityId(id) ;
			act.setLinkId(id) ;
			act.setType("home") ;
			act.setStartTime(122.) ;
			
			BasicLeg leg = pb.createLeg(BasicLeg.Mode.bike) ;
			plan.addLeg( leg ) ;
			
			List<Id> routeIdList = new ArrayList<Id>() ;
			routeIdList.add(id) ; routeIdList.add(id) ;

			BasicRoute route = pb.createRoute(id, id, routeIdList ) ;
			
			leg.setRoute(route) ;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

	public void handleEvent(ActEndEvent event) {
		ActEndEvent ev = event ;
		ev.getActType();
		ev.getLinkId();
		ev.getPersonId();
		ev.getTime();
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

	public void finishReplanning() {		
	}

}
