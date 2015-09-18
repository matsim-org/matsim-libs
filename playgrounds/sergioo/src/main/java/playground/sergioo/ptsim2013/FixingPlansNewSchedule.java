package playground.sergioo.ptsim2013;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class FixingPlansNewSchedule {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario2.getConfig().transit().setUseTransit(true);
		new MatsimPopulationReader(scenario).readFile(args[0]);
		new TransitScheduleReader(scenario2).readFile(args[1]);
		ExperimentalTransitRouteFactory factory = new ExperimentalTransitRouteFactory();
		int numNull=0, noLine=0, noRoute=0, noRouteWithP=0, total=0;
		for(Person person:scenario.getPopulation().getPersons().values())
			for(Plan plan:person.getPlans())
				for(PlanElement planElement:plan.getPlanElements())
					if(planElement instanceof Leg && ((Leg)planElement).getMode().equals("pt")) {
						total++;
						Route route = ((Leg)planElement).getRoute();
						if(route==null)
							numNull++;
						else {
							ExperimentalTransitRoute eRoute = (ExperimentalTransitRoute) factory.createRoute(route.getStartLinkId(), route.getEndLinkId());
							eRoute.setStartLinkId(route.getStartLinkId());
							eRoute.setEndLinkId(route.getEndLinkId());
							eRoute.setRouteDescription(route.getRouteDescription());
							if(scenario2.getTransitSchedule().getTransitLines().get(eRoute.getLineId())==null) {
								noLine++;
								System.out.println(eRoute.getLineId());
								((Leg)planElement).setRoute(null);
							}
							else if(scenario2.getTransitSchedule().getTransitLines().get(eRoute.getLineId()).getRoutes().get(eRoute.getRouteId())==null) {
								if(eRoute.getRouteId().toString().contains("-p")) {
									route.setRouteDescription(route.getRouteDescription().replaceFirst("-p===", "==="));
									noRouteWithP++;
								}
								noRoute++;
							}
						}
					}
					else if(planElement instanceof Leg && ((Leg)planElement).getMode().equals("transit_walk"))
						((Leg)planElement).setRoute(null);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(args[2]);
		System.out.println(numNull+" "+noLine+" "+noRoute+" "+noRouteWithP+" "+total);
	}

}
