package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.analysis.filters.population.AbstractPersonFilter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.Generic2ExpRouteConverter;

/** tracks passenger traveling along the given stops of a transit route based on population*/
public class PassengerTracker2 extends AbstractPersonFilter {
	private static final Logger log = Logger.getLogger(PassengerTracker2.class);
	private Generic2ExpRouteConverter converter;
	final List<Id<TransitLine>> transitLineIdList;
	int counter=0;
	
	public PassengerTracker2 (final List<Id<TransitLine>> transitLineIdList, /*final Network net,*/ final TransitSchedule schedule){
		this.transitLineIdList = transitLineIdList;
		converter = new Generic2ExpRouteConverter(schedule);
	}
	
	public List<Id<Person>> getTrackedPassengers(Population[] popArray) {
		List<Id<Person>> travelPersonList = new ArrayList<>();
		for (Population pop : popArray){
			List<Id<Person>> tmpPersonList = getTrackedPassengers(pop);
			for (Id<Person> tmpId : tmpPersonList){
				if(!travelPersonList.contains(tmpId)){
					travelPersonList.add(tmpId);
				}
			}
		}
		return travelPersonList;
	}
	
	public List<Id<Person>> getTrackedPassengers(final Population population) {
		List<Id<Person>> travelPersonList = new ArrayList<>();
		for (Person person : population.getPersons().values()) {
			if( judge(person)){
				if(!travelPersonList.contains(person.getId())){
				   travelPersonList.add(person.getId());
				}
			}
		}
		return travelPersonList;
	}
	
	/** removes persons who do not travel in the given transit lines*/
	private void removeNonTrackedPassengers(Population population) {
		List<Id<Person>> trackedPersons = getTrackedPassengers(population);
		population.getPersons().keySet().retainAll(trackedPersons);
	}
	
	@Override
	public boolean judge(Person person) {
		for (Plan plan : person.getPlans()){
			for (PlanElement pe: plan.getPlanElements()){
				if (pe instanceof Leg) {
					Leg leg = (Leg)pe;
					if(leg.getMode().equals(TransportMode.pt)){
						if (leg.getRoute()!= null && (leg.getRoute() instanceof org.matsim.api.core.v01.population.Route)){
							ExperimentalTransitRoute expRoute = converter.convert((GenericRouteImpl) leg.getRoute());
							
							if (transitLineIdList.contains(expRoute.getLineId())){
								return true;
							}
							//if (expRoute.getLineId().equals(line.getId())){  
							//	return true;
								//find out if the passenger travels along the stops
								/*
								ExpTransRouteUtils exputil = new ExpTransRouteUtils(net, schedule, expRoute);
								for (TransitRouteStop stop :stopList){
									if (exputil.getStops().contains(stop)){
										return true;
									}
								}
								*/
						}
					}
				}
			}
		}
		log.info( ++ counter);
		return false;
	}		

	public static void main(String[] args) {
		String popFile;
		String netFile;
		String scheduleFile;
		String lineIdsList = "";
		
		if(args.length>0){
			popFile =args[0];
			netFile = args[1];
			scheduleFile = args[2];
		}else{
			popFile ="../../";
			netFile = "../../";
			scheduleFile = "../../";
		}
		
		//convert lines ids from string to Id objects
		List <Id<TransitLine>> transitLineIdList = new ArrayList<>();
		String[] idArray = lineIdsList.split(",");
		for (String strId: idArray){
			transitLineIdList.add(Id.create(strId, TransitLine.class));
		}
		
		//read data
		DataLoader loader = new DataLoader();
		Scenario scn = loader.readNetwork_Population(netFile, popFile);
		Population pop = scn.getPopulation();
		Network net = scn.getNetwork();
		System.out.println("original agents: " + pop.getPersons().size());
		TransitSchedule schedule= loader.readTransitSchedule(scheduleFile);

		new PassengerTracker2(transitLineIdList, schedule).removeNonTrackedPassengers(pop);
		
		//write population
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(pop, net);
		File file = new File(popFile);
		popwriter.write(file.getParent() + "/" + file.getName() + "tracked.xml.gz") ;
		System.out.println("final agents:" + pop.getPersons().size() );
		
	}
	
}