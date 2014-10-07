package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.LegImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**filters a population leaving only persons using some transit routes*/
public class TrRouteFilter4Plan {

	public Population filterPlan (Population population, List<Id<TransitRoute>> trRoutesIds ){
		List<Id> personsToDel = new ArrayList<Id>();
		for (Person person : population.getPersons().values()){
			boolean keepPerson = false;
			for (Plan plan : person.getPlans()){
				List<PlanElement> planElements = plan.getPlanElements();
				for (PlanElement planElement : planElements){
					if (planElement instanceof Leg) {
						LegImpl leg = (LegImpl)planElement;
						if (leg.getRoute()!= null && (leg.getRoute() instanceof ExperimentalTransitRoute) ){
							ExperimentalTransitRoute expTrRoute = ((ExperimentalTransitRoute)leg.getRoute());
							keepPerson =  keepPerson || trRoutesIds.contains(expTrRoute.getRouteId()); 
						}
					}
				}
			}
			if (!keepPerson) personsToDel.add(person.getId());
		}
		
		//delete persons that do not use the routes
		for (Id personId : personsToDel){
			population.getPersons().remove(personId);
		}
		
		return population;
	}
	
	public static void main(String[] args) {
		String netFilePath = null;
		String popFilePath = null;
		List<Id<TransitRoute>> trRoutesIds = new ArrayList<>();
		
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
			trRoutesIds.add(Id.create(args[2], TransitRoute.class));
			trRoutesIds.add(Id.create(args[3], TransitRoute.class));
			trRoutesIds.add(Id.create(args[4], TransitRoute.class));
			trRoutesIds.add(Id.create(args[5], TransitRoute.class));
		}else{
			netFilePath = "../../multimodalNet.xml.gz";
			popFilePath ="../../"; 
			trRoutesIds.add(Id.create("B-M44.101.901.H", TransitRoute.class));
			trRoutesIds.add(Id.create("B-M44.101.901.R", TransitRoute.class));
			trRoutesIds.add(Id.create("B-M44.102.901.H", TransitRoute.class));
			trRoutesIds.add(Id.create("B-M44.102.901.R", TransitRoute.class));
		}
		
		DataLoader dloader = new DataLoader();
		Scenario scn = dloader.readNetwork_Population(netFilePath, popFilePath);
		Population pop = scn.getPopulation();
		pop=  new TrRouteFilter4Plan().filterPlan(pop, trRoutesIds);
		
		//write population
		System.out.println("writing output plan file...");
		PopulationWriter popWriter = new PopulationWriter(pop, scn.getNetwork());
		File file = new File(popFilePath);
		String outputFile = file.getParent() + "/" + file.getName() + "Trackedonlym44.xml.gz";
		popWriter.write(outputFile) ;
		System.out.println("done");
		
		//write a sample
		popWriter = new PopulationWriter(new FirstPersonsExtractor().run(pop, 100), scn.getNetwork());
		popWriter.write(outputFile + "Sample.xml") ;
		
	}

}
