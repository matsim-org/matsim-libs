package playground.singapore.ptsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

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
		Id eStopId = null, eStopId2 = null;
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
					act.setLinkId(eStopId2);
					eStopId2 = null;
				}
			}
			else if(planElement instanceof Activity) {
				ptAct = (Activity) planElement;
				if(eStopId!=null) {
					ptAct.setLinkId(eStopId);
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
					Id aStopId = eRoute.getAccessStopId();
					eStopId = eRoute.getEgressStopId();
					if(act!=null)
						act.setLinkId(aStopId);
					ptAct.setLinkId(aStopId);
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
//		final Population reader = (Population) scenario.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
		StreamingUtils.setIsStreaming(reader, true);
//		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		StreamingPopulationWriter plansWriter = new StreamingPopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		plansWriter.startStreaming(args[2]);
		reader.addAlgorithm(new PopulationLinksToStopLinks(scenario.getNetwork()));
		final PersonAlgorithm algo = plansWriter;
		reader.addAlgorithm(algo);
//		plansReader.readFile(args[0]);
		reader.readFile(args[0]);
		plansWriter.closeStreaming();
	}
	
}
