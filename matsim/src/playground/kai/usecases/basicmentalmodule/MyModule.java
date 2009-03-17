package playground.kai.usecases.basicmentalmodule;

import java.util.*;

import org.apache.log4j.Logger;

import org.matsim.interfaces.basic.v01.*;
import org.matsim.interfaces.basic.v01.facilities.*;
import org.matsim.interfaces.basic.v01.network.*;
import org.matsim.interfaces.basic.v01.population.*;

// TODO: add events handlers in basic. ok for today
import org.matsim.events.handler.*;

//TODO: use basic versions of this. ok for today
import org.matsim.events.* ;

// TODO: ???
import org.matsim.controler.Controler;

// TODO: ????  BasicStrategyModule???

// TODO: ????
import org.matsim.interfaces.core.v01.Plan;


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
			
			BasicPlan newPlan = pb.createPlan() ; // replace (??) the plan by a completely new plan
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
