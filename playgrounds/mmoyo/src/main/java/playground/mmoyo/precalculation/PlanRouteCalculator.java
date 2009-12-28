package playground.mmoyo.precalculation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class PlanRouteCalculator {
	///private ConnectionRate connectionRate = new ConnectionRate ();
	private String outputFile;
	private String plansFile;
	private DynamicConnection  dynamicConnection;
	private Map <Id, List<StaticConnection>> connectionMap = new TreeMap <Id, List<StaticConnection>>();;
	private TransitSchedule transitSchedule;
	private NetworkLayer net;
	private PopulationImpl population = new PopulationImpl();
	private KroutesCalculator kRoutesCalculator;
	PlainTimeTable plainTimeTable;
	
	public PlanRouteCalculator(final TransitSchedule transitSchedule, final NetworkLayer net, Map <Id, List<StaticConnection>> connectionMap, PopulationImpl population, KroutesCalculator kRoutesCalculator){
		this.transitSchedule = transitSchedule;
		this.net = net;
		this.connectionMap = connectionMap;
		this.population = population;
		this.kRoutesCalculator =kRoutesCalculator; 
	
		plainTimeTable = new PlainTimeTable(transitSchedule);
		///connectionRate = new ConnectionRate (connectionMap, nearStopMap);
	}
	
	/**Precalculates all routes for all plans*/
	public void PreCalRoutes(){
	
		long startTime = System.currentTimeMillis();
		int found=0;
		for (Person person: population.getPersons().values()) {
			Plan plan = person.getPlans().get(0);

			int foundConns=0;
			boolean first =true;
			
			ActivityImpl lastAct = null;       
			ActivityImpl thisAct= null;		 
				
			for (PlanElement pe : plan.getPlanElements()) {   		//temporarily commented in order to find only the first leg
				
				if (pe instanceof ActivityImpl) {  				
					thisAct= (ActivityImpl) pe;					
					if (!first) {								
						Coord lastActCoord = lastAct.getCoord();
			    		Coord actCoord = thisAct.getCoord();
			    		foundConns = kRoutesCalculator.findPTPath(lastActCoord, actCoord, 400);
					}
		    		lastAct = thisAct;
		    		first=false;
				}
			}
			if (foundConns>0) ++found;
			System.out.println (person.getId());
		}
		double duracion= System.currentTimeMillis()-startTime;
		int intDuracion= (int)duracion;
		System.out.print("duracion total:  ") ;
		System.out.print(intDuracion);
		System.out.println("agents: " + population.getPersons().values().size());
		System.out.println ("found:" + found);
	}
	
	
	/** sets the most appropriate route for plans from the precalculated static routes*/
	public void findRoutes(){
		PopulationImpl newPopulation = new PopulationImpl();
		
		for (Person person: population.getPersons().values()) {
				//if ( true ) {
				//PersonImpl person = population.getPersons().get(new IdImpl("35420")); // 5636428  2949483 
			PlanImpl newPlan = new PlanImpl(person);
			Plan plan = person.getPlans().get(0);
			boolean first =true;
			ActivityImpl lastAct = null;       
			ActivityImpl thisAct= null;		 
					
			//for (PlanElement pe : plan.getPlanElements()) {   		//temporarily commented in order to find only the first leg
			for	(int elemIndex=0; elemIndex<3; elemIndex++){          
				PlanElement pe= plan.getPlanElements().get(elemIndex); 
					
				if (pe instanceof ActivityImpl) {  				
					thisAct= (ActivityImpl) pe;					
					if (!first) {								
			    		//StaticConnection bestStatConnection  = dynamicConnection.getBestConnection(lastAct, thisAct);
			    		
			    		
			    		//Create a set of legs and links to insert the PT legs and trnasfer activities
			    		
			    		
			    		
			    		
					}
					lastAct= thisAct;
				}
			}
			
	
			((PersonImpl) person).exchangeSelectedPlan(newPlan, true);
			((PersonImpl) person).removeUnselectedPlans();
			newPopulation.addPerson(person);

			System.out.println("writing output plan file...");
			new PopulationWriter(newPopulation).writeFile(outputFile);
			System.out.println("Done");
		}
	}
	
	/*
	private void calculateWalkDistances(StaticConnection staticConnection, Coord coord1, Coord coord2){
		walkDistance1 = CoordUtils.calcDistance(coord1, staticConnection.getFromNode().getCoord());
		walkDistance2 = CoordUtils.calcDistance(coord2, staticConnection.getToNode().getCoord());
		walkTime1 = walkDistance1 * avgWalkSpeed;
		walkTime2 = walkDistance2 * avgWalkSpeed;
	}
	*/
	
	private Leg createLeg(double fromTime, PTtrip ptTrip){
		TransitStopFacility boardFacility = transitSchedule.getFacilities().get(ptTrip.getBoardFacilityId());
		TransitRoute trRoute = ptTrip.getTransitRoute();
		double nextDep = plainTimeTable.getNextDeparture(trRoute, boardFacility, fromTime);
		
		Leg leg = new LegImpl(ptTrip.getTransitRoute().getTransportMode());
		leg.setDepartureTime(nextDep);
		leg.setRoute(ptTrip.getRoute());
		leg.setTravelTime(ptTrip.getTravelTime());
		return leg;
	}
	
	
	


	
}
