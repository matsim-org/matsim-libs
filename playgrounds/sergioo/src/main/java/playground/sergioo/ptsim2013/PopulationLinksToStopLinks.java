package playground.sergioo.ptsim2013;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.ScenarioUtils;
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
		Activity act = null, ptAct = null;
		Id<TransitStopFacility> eStopId = null, eStopId2 = null;
		ExperimentalTransitRouteFactory factory = new ExperimentalTransitRouteFactory();
		for (PlanElement planElement : plan.getPlanElements()) {
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				if(act!=null) {
					act.setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
					ptAct.setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
					((Activity) planElement).setLinkId(NetworkUtils.getNearestLink((network), act.getCoord()).getId());
				}
				act = (Activity) planElement;
				if(eStopId2!=null) {
					act.setLinkId(Id.createLinkId(eStopId2));
					eStopId2 = null;
				}
			}
			else if(planElement instanceof Activity) {
				ptAct = (Activity) planElement;
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
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
		StreamingDeprecated.setIsStreaming(((Population) scenario.getPopulation()), true);
		MatsimReader plansReader = new PopulationReader(scenario);
		StreamingPopulationWriter plansWriter = new StreamingPopulationWriter();
		plansWriter.startStreaming(args[2]);
		StreamingDeprecated.addAlgorithm(((Population) scenario.getPopulation()), new PopulationLinksToStopLinks(scenario.getNetwork()));
		final PersonAlgorithm algo = plansWriter;
		StreamingDeprecated.addAlgorithm(((Population) scenario.getPopulation()), algo);
		plansReader.readFile(args[0]);
		plansWriter.closeStreaming();
	}
	
}
