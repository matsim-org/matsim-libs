package playground.kai.usecases.basicmentalmodule;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;


@SuppressWarnings("unused")
public class MyModule implements
PlanStrategyModule,
ActivityEndEventHandler
{
	private static final Logger log = Logger.getLogger(MyModule.class);
	
	Scenario sc ; 
	Network net ;
	Population pop ;
//	BasicFacilities facs ; // TODO: nicht konsequenterweise BasicFacilities<BasicFacility> ??  Maybe low prio.
	
	public MyModule(Scenario sc) {
		
		net = sc.getNetwork() ;
		pop = sc.getPopulation() ;
//		facs = sc.getFacilities() ;
		
	}
	
	public void prepareReplanning() { 
		
		// go through network and copy to my personal network:
		for ( Node bn : net.getNodes().values() ) {
			Id id = bn.getId();
			Coord coord = bn.getCoord(); 
		}
		for ( Link bl : net.getLinks().values() ) {

			Id id = bl.getId() ;

			Node fNode = bl.getFromNode();
			Node tNode = bl.getToNode() ;
			double len = bl.getLength() ;

			double fs = bl.getFreespeed(0.) ; 
			double cap = bl.getCapacity(0.) ;
			double nLanes = bl.getNumberOfLanes(0.) ; 
			// TODO: also getters w/o time argument?  I think that would contribute to robustness ...
			// But probably low prio.
			
		}
		
		// go through population and copy to my personal population:
		for ( Person person : pop.getPersons().values() ) {
			
			Id id = person.getId();
			
			List<? extends Plan> plans = person.getPlans() ;
			
			for ( Plan plan : plans ) {
				for ( Object oo : plan.getPlanElements() ) {
					if ( oo instanceof Activity ) {
						Activity act = (Activity) oo ;
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
						
					} else if ( oo instanceof Leg ) {
						Leg leg = (Leg) oo ;
						leg.getDepartureTime();
						leg.getMode();
						leg.getRoute();
						leg.getTravelTime();
					}
				}
			}

		}
	}
	
	public void handlePlan(Plan plan) {
		
		PopulationFactory pb = pop.getFactory() ; 
		
		try {
			Id id = sc.createId("1") ; 

			Person person = pb.createPerson(id) ;
			pop.addPerson(person);
			
			Plan newPlan = pb.createPlan() ; 
			person.addPlan(newPlan ) ;
			
			// FIXME: This creational method has the side effect of also adding the created Object.  In my view:
			// - either createAndAddPlan
			// - or createPlan w/o side effects
			
//			person.addPlan(newPlan) ; // now the person has the plan twice.
			
			Activity act = null ;
			
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
			
			Leg leg = pb.createLeg(TransportMode.bike) ;
			plan.addLeg( leg ) ;
			
			List<Id> routeIdList = new ArrayList<Id>() ;
			routeIdList.add(id) ; routeIdList.add(id) ;

//			BasicRoute route = pb.createRoute(id, id, routeIdList ) ;
//			leg.setRoute(route) ;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

	public void handleEvent(ActivityEndEvent event) {
		ActivityEndEvent ev = event ;
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
