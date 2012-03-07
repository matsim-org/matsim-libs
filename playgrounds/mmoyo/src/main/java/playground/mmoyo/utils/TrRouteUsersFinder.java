package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/** reads a population file and counts how many agents use a given transit route according to selected plans  */
public class TrRouteUsersFinder {
	final Population pop;
	final TransitSchedule schedule;
	final Network net;
	
	public TrRouteUsersFinder (final Population pop, final TransitSchedule schedule, final Network net){
		this.pop = pop; 
		this.schedule = schedule;
		this.net = net;
	}

	private void run(final String strRouteId){
		Generic2ExpRouteConverter generic2ExpRouteConverter = new Generic2ExpRouteConverter(schedule);
		List<Id> agentList = new ArrayList<Id>();
		List<Double> distList = new ArrayList<Double>();
		
		for (Person person: pop.getPersons().values()){
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if ((pe instanceof Leg)) {
					Leg leg = (Leg)pe;
					if (leg.getMode().equals(TransportMode.pt)){
						if (leg.getRoute() != null){
							ExperimentalTransitRoute expRoute = generic2ExpRouteConverter.convert((GenericRouteImpl) leg.getRoute());
							//System.out.println(expRoute.getRouteId());
							if ( expRoute.getRouteId().toString().equals(strRouteId) ){ 
								ExpTransRouteUtils expTransRouteUtils = new ExpTransRouteUtils(net, schedule, expRoute );
								distList.add(expTransRouteUtils.getExpRouteDistance());
				
								if(!agentList.contains(person.getId())  ) {
									agentList.add(person.getId());
								}	
							}
						}						
					}
				}
			}
		}
		
		double sume=0;
		for (Double dist: distList){
			sume  += dist;
		}
		
		System.out.println("number of agents: "+ agentList.size());
		System.out.println("average distance in m44: "+ sume/distList.size());
	}
	
	public static void main(String[] args) {
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz";
		String popFilePath = "../../input/paper/manualCalibration.10.plans.xml.gz";
		String trScheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String strRouteId = "B-M44.101.901.H";
		
		DataLoader dloader = new DataLoader();
		Population pop = dloader.readPopulation(popFilePath);
		TransitSchedule schedule = dloader.readTransitSchedule(trScheduleFile);	
		Network net = dloader.readNetwork(netFilePath); 
		TrRouteUsersFinder trRouteUsersFinder = new TrRouteUsersFinder(pop, schedule, net);
		trRouteUsersFinder.run(strRouteId);
	}

}
