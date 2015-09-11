package playground.sergioo.ptsim2013;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PopulationLinksToStopLinks implements PersonAlgorithm {

	private final Network network;

	public PopulationLinksToStopLinks(Network network) {
		this.network = network;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans())
			processPlan(plan);
	}

	private void processPlan(final Plan plan) {
		ActivityImpl act = null, ptAct = null;
		Id<TransitStopFacility> eStopId = null, eStopId2 = null;
		ExperimentalTransitRouteFactory factory = new ExperimentalTransitRouteFactory();
		for (PlanElement planElement : plan.getPlanElements()) {
			if(planElement instanceof ActivityImpl && !((ActivityImpl)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				if(act!=null) {
					act.setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
					ptAct.setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
					((ActivityImpl) planElement).setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
				}
				act = (ActivityImpl) planElement;
				if(eStopId2!=null) {
					act.setLinkId(Id.createLinkId(eStopId2));
					eStopId2 = null;
				}
			}
			else if(planElement instanceof ActivityImpl) {
				ptAct = (ActivityImpl) planElement;
				if(eStopId!=null) {
					ptAct.setLinkId(Id.createLinkId(eStopId));
					eStopId2 = eStopId;
					eStopId = null;
				}
			}
			else if(planElement instanceof Leg && ((Leg)planElement).getMode().equals("pt")) {
				Route route = ((Leg)planElement).getRoute();
				if(route!=null) {
					ExperimentalTransitRoute eRoute = (ExperimentalTransitRoute) factory.createRoute(route.getStartLinkId(), route.getEndLinkId());
					eRoute.setStartLinkId(route.getStartLinkId());
					eRoute.setEndLinkId(route.getEndLinkId());
					eRoute.setRouteDescription(route.getRouteDescription());
					Id<TransitStopFacility> aStopId = eRoute.getAccessStopId();
					eStopId = eRoute.getEgressStopId();
					if(act!=null)
						act.setLinkId(Id.createLinkId(aStopId));
					ptAct.setLinkId(Id.createLinkId(aStopId));
				}
				else if(act!=null)
					act.setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
				act = null;
			}
		}
	}

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		((PopulationImpl) scenario.getPopulation()).setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		PopulationWriter plansWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		plansWriter.startStreaming(args[2]);
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(new PopulationLinksToStopLinks(scenario.getNetwork()));
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(plansWriter);
		plansReader.readFile(args[0]);
		plansWriter.closeStreaming();
	}
	
}
