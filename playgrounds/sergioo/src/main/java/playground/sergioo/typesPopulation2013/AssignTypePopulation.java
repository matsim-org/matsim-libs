package playground.sergioo.typesPopulation2013;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.typesPopulation2013.population.PersonImplPops;
import playground.sergioo.typesPopulation2013.population.PopulationWriter;

public class AssignTypePopulation {

	private enum Type {
		ID("id"),
		TRANSIT_LINE("line"),
		SOCIAL("social");
		
		private final String name;
		
		private Type(String name) {
			this.name = name;
		}
		private static Type getType(String name) {
			for(Type type:Type.values())
				if(type.name.equals(name))
					return type;
			return null;
		}
	}
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimPopulationReader(scenario)).parse(args[0]);
		Population population = null;
		switch(Type.getType(args[1])) {
		case ID:
			population = getPopulationTypesId(scenario, args);
			break;
		case TRANSIT_LINE:
			population = getPopulationTypesTransitLine(scenario, args);
			break;
		case SOCIAL:
			population = getPopulationTypesSocial(scenario, args);
			break;
		}
		(new MatsimNetworkReader(scenario)).readFile(args[2]);
		(new PopulationWriter(population)).write(args[3]);
	}
	private static Population getPopulationTypesSocial(Scenario scenario, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}
	private static Population getPopulationTypesTransitLine(Scenario scenario, String[] args) {
		scenario.getConfig().transit().setUseTransit(true);
		(new TransitScheduleReader(scenario)).readFile(args[4]);
		TransitLine line = scenario.getTransitSchedule().getTransitLines().get(Id.create(args[5], TransitLine.class));
        ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		for(Person person:scenario.getPopulation().getPersons().values())
			if(isRelatedWithLine(person, line))
				population.addPerson(new PersonImplPops(person, Id.create(line.getId(), Population.class)));
			else
				population.addPerson(new PersonImplPops(person, PersonImplPops.DEFAULT_POP_ID));
		return population;
	}
	private static boolean isRelatedWithLine(Person person, TransitLine line) {
		ExperimentalTransitRouteFactory factory = new ExperimentalTransitRouteFactory();
		for(Plan plan:person.getPlans())
			for(PlanElement planElement:plan.getPlanElements())
				if(planElement instanceof Leg && ((Leg)planElement).getRoute() instanceof Route) {
					Route origRoute = ((Leg) planElement).getRoute();
					ExperimentalTransitRoute route = (ExperimentalTransitRoute) factory.createRoute(origRoute.getStartLinkId(), origRoute.getEndLinkId());
					route.setStartLinkId(origRoute.getStartLinkId());
					route.setEndLinkId(origRoute.getEndLinkId());
					route.setRouteDescription(origRoute.getRouteDescription());
					for(TransitRoute transitRoute:line.getRoutes().values())
						for(TransitRouteStop stop:transitRoute.getStops())
							if(stop.getStopFacility().getId().equals(route.getAccessStopId()) || stop.getStopFacility().getId().equals(route.getEgressStopId()))
								return true;
				}
		return false;
	}
	private static Population getPopulationTypesId(Scenario scenario, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

}
