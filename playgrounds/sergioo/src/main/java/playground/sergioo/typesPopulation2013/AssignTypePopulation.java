package playground.sergioo.typesPopulation2013;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.typesPopulation2013.population.PersonImplPops;
import playground.sergioo.typesPopulation2013.population.PopulationWriter;

public class AssignTypePopulation implements PersonAlgorithm {

	private enum Type {
		ID("id"),
		TRANSIT_LINE("line"),
		SOCIAL("social"),
		MODE("mode");
		
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
	
	private String[] args;
	private Scenario scenario;
	private Population population;
	private static TransitActsRemover remover = new TransitActsRemover();
	
	public AssignTypePopulation(String[] args, Scenario scenario, Population population) {
		this.args = args;
		this.scenario = scenario;
		this.population = population;
	}
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = new PopulationImpl((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		((PopulationImpl)population).setIsStreaming(true);
		if(Type.getType(args[1]).equals(Type.TRANSIT_LINE)) {
			scenario.getConfig().scenario().setUseTransit(true);
			(new TransitScheduleReader(scenario)).readFile(args[4]);
		}
		(new MatsimNetworkReader(scenario)).readFile(args[2]);
		PopulationWriter writer = new PopulationWriter(population, scenario.getNetwork());
		writer.startStreaming(args[3]);
		((PopulationImpl)population).addAlgorithm(writer);
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new AssignTypePopulation(args, scenario, population));
		(new MatsimPopulationReader(scenario)).parse(args[0]);
		writer.closeStreaming();
	}
	private void getPopulationTypesModeOld(Person person, String[] args) {
		if(planWithMode(person.getSelectedPlan(), args[4]))
			population.addPerson(new PersonImplPops((PersonImpl)person, new IdImpl(args[4])));
		else
			population.addPerson(new PersonImplPops((PersonImpl)person, PersonImplPops.DEFAULT_POP_ID));
	}
	private void getPopulationTypesMode(Person person, String[] args) {
		if(planWithMode(person.getSelectedPlan(), args[4])) {
			Set<Plan> deletePlans = new HashSet<Plan>();
			for(Plan plan: person.getPlans())
				if(!planWithMode(plan, args[4]))
					deletePlans.add(plan);
				else
					remover.run(plan);
			for(Plan plan: deletePlans)
				person.getPlans().remove(plan);
			population.addPerson(new PersonImplPops((PersonImpl)person, new IdImpl(args[4])));
		}
		else {
			Set<Plan> deletePlans = new HashSet<Plan>();
			for(Plan plan: person.getPlans())
				if(planWithMode(plan, args[4]))
					deletePlans.add(plan);
			for(Plan plan: deletePlans)
				person.getPlans().remove(plan);
			population.addPerson(new PersonImplPops((PersonImpl)person, PersonImplPops.DEFAULT_POP_ID));
		}
	}
	private static boolean planWithMode(Plan plan, String mode) {
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Leg && ((Leg)planElement).getMode().contains(mode))
				return true;
		return false;
	}
	private Population getPopulationTypesSocial(Person person, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}
	private void getPopulationTypesTransitLine(Person person, String[] args) {
		TransitLine line = scenario.getTransitSchedule().getTransitLines().get(new IdImpl(args[5]));
		if(isRelatedWithLine(person, line))
			population.addPerson(new PersonImplPops((PersonImpl)person, line.getId()));
		else
			population.addPerson(new PersonImplPops((PersonImpl)person, PersonImplPops.DEFAULT_POP_ID));
	}
	private static boolean isRelatedWithLine(Person person, TransitLine line) {
		ExperimentalTransitRouteFactory factory = new ExperimentalTransitRouteFactory();
		for(Plan plan:person.getPlans())
			for(PlanElement planElement:plan.getPlanElements())
				if(planElement instanceof Leg && ((Leg)planElement).getRoute() instanceof GenericRoute) {
					ExperimentalTransitRoute route = (ExperimentalTransitRoute) factory.createRoute(((Leg)planElement).getRoute().getStartLinkId(), ((Leg)planElement).getRoute().getEndLinkId());
					route.setRouteDescription(((Leg)planElement).getRoute().getStartLinkId(), ((GenericRoute)((Leg)planElement).getRoute()).getRouteDescription(), ((Leg)planElement).getRoute().getEndLinkId());
					for(TransitRoute transitRoute:line.getRoutes().values())
						for(TransitRouteStop stop:transitRoute.getStops())
							if(stop.getStopFacility().getId().equals(route.getAccessStopId()) || stop.getStopFacility().getId().equals(route.getEgressStopId()))
								return true;
				}
		return false;
	}
	private void getPopulationTypesId(Person person, String[] args) {
		// TODO Auto-generated method stub
	}
	@Override
	public void run(Person person) {
		switch(Type.getType(args[1])) {
		case ID:
			getPopulationTypesId(person, args);
			break;
		case TRANSIT_LINE:
			getPopulationTypesTransitLine(person, args);
			break;
		case SOCIAL:
			getPopulationTypesSocial(person, args);
			break;
		case MODE:
			getPopulationTypesMode(person, args);
		}
	}

}
