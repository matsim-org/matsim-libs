package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/** reads a population file and counts how many agents use a given transit route according to selected plans  */
public class TrRouteUsersFinder {
	final Population pop;
	final TransitSchedule schedule;
	final Network net;
	List<Id> agentList = new ArrayList<Id>();
	
	public TrRouteUsersFinder (final Population pop, final TransitSchedule schedule, final Network net){
		this.pop = pop; 
		this.schedule = schedule;
		this.net = net;
	}

	private void countUsers(final String strRouteId){
		Generic2ExpRouteConverter generic2ExpRouteConverter = new Generic2ExpRouteConverter(schedule);
		List<Double> distList = new ArrayList<Double>();
		for (Person person: pop.getPersons().values()){
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if ((pe instanceof Leg)) {
					Leg leg = (Leg)pe;
					if (leg.getMode().equals(TransportMode.pt)){
						if (leg.getRoute() != null){
							ExperimentalTransitRoute expRoute = generic2ExpRouteConverter.convert((GenericRouteImpl) leg.getRoute());
							//System.out.println(expRoute.getRouteId());
							if ( expRoute.getRouteId().toString().contains(strRouteId) ){ 
								ExpTransRouteUtils expTransRouteUtils = new ExpTransRouteUtils(net, schedule, expRoute );
								distList.add(expTransRouteUtils.getExpRouteDistance());
								if(!agentList.contains(person.getId())) {
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
	
	public void writeUsersPop(final String strRouteId, final String newPopFile){
		this.countUsers(strRouteId);
		Population newPop= new DataLoader().createScenario().getPopulation();
		for(Id id: agentList){
			newPop.addPerson(this.pop.getPersons().get(id));
		}
		PopulationWriter popWriter = new PopulationWriter(newPop, net);
		popWriter.write(newPopFile);
	}
	
	
	public static void main(String[] args) {
		String netFilePath = null;
		String popFilePath = null;
		String trScheduleFile = null;
		String strRouteId = null;
		String outFilePath = null;
		
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
			trScheduleFile = args[2];
			strRouteId = args[3];
			outFilePath = args[4];
		}else{
			netFilePath = "../../";
			popFilePath ="../../"; 
			trScheduleFile = "../../pt_transitSchedule.xml.gz";
			strRouteId = "M44"; 
			outFilePath = "";
		}
		
		DataLoader dloader = new DataLoader();
		Scenario scn = dloader.readNetwork_Population(netFilePath, popFilePath);
		Population pop = scn.getPopulation();
		Network net = scn.getNetwork();
		TransitSchedule schedule = dloader.readTransitSchedule(trScheduleFile);	
		TrRouteUsersFinder trRouteUsersFinder = new TrRouteUsersFinder(pop, schedule, net);
		trRouteUsersFinder.writeUsersPop(strRouteId, outFilePath);
	}

}
