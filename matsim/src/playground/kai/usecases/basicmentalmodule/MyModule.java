package playground.kai.usecases.basicmentalmodule;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityEndEventHandler;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.network.BasicNetwork;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicPopulationFactory;
import org.matsim.api.basic.v01.replanning.BasicPlanStrategyModule;


@SuppressWarnings("unused")
public class MyModule implements
BasicPlanStrategyModule,
BasicActivityEndEventHandler
{
	private static final Logger log = Logger.getLogger(MyModule.class);
	
	BasicScenario sc ; 
	BasicNetwork<BasicNode,BasicLink> net ;
	BasicPopulation<BasicPerson<BasicPlan>> pop ;
//	BasicFacilities facs ; // TODO: nicht konsequenterweise BasicFacilities<BasicFacility> ??  Maybe low prio.
	
	public MyModule(BasicScenario sc) {
		
		net = sc.getNetwork() ;
		pop = sc.getPopulation() ;
//		facs = sc.getFacilities() ;
		
	}
	
	public void prepareReplanning() { 
		
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
				for ( Object oo : plan.getPlanElements() ) {
					if ( oo instanceof BasicActivity ) {
						BasicActivity act = (BasicActivity) oo ;
						act.getCoord() ; // deprecated ????
						act.getEndTime() ;
						Id facId = act.getFacilityId();
						act.getLinkId(); // stays here so mobsim can be run w/o facilities
						act.getStartTime();
						act.getType();
						
//						BasicFacility fac = facs.getFacilities().get( facId ) ;
//						fac.getCoord();
//						fac.getActivityOptions();
//						fac.getLinkId();
						
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
	
	public void handlePlan(BasicPlan ppp) { // need handlePlan(BasicPlan) ??????
		BasicPlan plan = ppp ;
		
		BasicPopulationFactory pb = pop.getFactory() ; 
		
		try {
			Id id = sc.createId("1") ; 

			BasicPerson<BasicPlan> person = pb.createPerson(id) ;
			pop.addPerson(person);
			
			BasicPlan newPlan = pb.createPlan(person) ; 
			person.addPlan(newPlan ) ;
			
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
//			act.setFacilityId(id) ;
//			act.setLinkId(id) ;
			act.setType("home") ;
			act.setStartTime(122.) ;
			
			BasicLeg leg = pb.createLeg(TransportMode.bike) ;
			plan.addLeg( leg ) ;
			
			List<Id> routeIdList = new ArrayList<Id>() ;
			routeIdList.add(id) ; routeIdList.add(id) ;

//			BasicRoute route = pb.createRoute(id, id, routeIdList ) ;
//			leg.setRoute(route) ;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

	public void handleEvent(BasicActivityEndEvent event) {
		BasicActivityEndEvent ev = event ;
		ev.getActType();
		ev.getLinkId();
		ev.getPersonId();
		ev.getTime();
	}


	public void reset(int iteration) {
	}

	public void finishReplanning() {		
	}

}
