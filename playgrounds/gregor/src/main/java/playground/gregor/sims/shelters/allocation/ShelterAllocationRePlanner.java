package playground.gregor.sims.shelters.allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.evacuation.base.Building;

public class ShelterAllocationRePlanner implements IterationStartsListener {

	//	private Population plans;
	private Dijkstra router;
	private ScenarioImpl sceanrio;
	private List<Building> buildings;
	private ArrayList<Person> agents;

	private static final double PSHELTER = 0.05;

	private boolean initialized = false;
	private int c = 0;
	private NetworkFactoryImpl routeFactory;

	public ShelterAllocationRePlanner(ScenarioImpl sc, TravelCost tc, TravelTime tt, List<Building> buildings) {
		//		this.plans = sc.getPopulation();
		this.router =  new Dijkstra(sc.getNetwork(),tc,tt);
		this.sceanrio = sc;
		this.buildings = buildings;
		this.agents = new ArrayList<Person>(sc.getPopulation().getPersons().values());
		this.routeFactory = (NetworkFactoryImpl) sc.getNetwork().getFactory();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() > 0) {
			run();		
		}
	}

	private void run() {
		if (!this.initialized) {
			init();
		}
		Random rand = MatsimRandom.getRandom();
		for (Person pers : this.agents) {
			if (rand.nextDouble() < PSHELTER) {
				doRePlanning(pers, rand);
			}
		}

	}

	private void init() {
		for (Building b : this.buildings) {
			if (b.isQuakeProof()) {
				if (!b.getId().toString().contains("super")) {
					this.c  += b.getShelterSpace();
				}
			}
		}
		this.initialized = true;
	}

	private void doRePlanning(Person pers, Random rand) {
		//		if (rand.nextDouble() < (double)this.agents.size()/(double)this.c) {
		//			doSwitchTwoAgents(pers,rand);
		//		} else {
		//			doShiftOneAgent(pers, rand);
		//		}
		doSwitchTwoAgents(pers,rand);
	}

	private void doShiftOneAgent(Person pers, Random rand) {
		// TODO Auto-generated method stub

	}

	private void doSwitchTwoAgents(Person pers1, Random rand) {
		Person pers2 = this.agents.get(rand.nextInt(this.agents.size()));
		double origScore1 = pers1.getSelectedPlan().getScore();
		double origScore2 = pers2.getSelectedPlan().getScore();
		Plan plan1 = pers1.getSelectedPlan();
		Plan plan2 = pers2.getSelectedPlan();
		Activity origAct11 = (Activity)plan1.getPlanElements().get(0);
		Activity origAct12 = (Activity)plan1.getPlanElements().get(2);
		Activity origAct21 = (Activity)plan2.getPlanElements().get(0);
		Activity origAct22 = (Activity)plan2.getPlanElements().get(2);

		Node origN11 = this.sceanrio.getNetwork().getLinks().get(origAct11.getLinkId()).getToNode();
		Node origN12 = this.sceanrio.getNetwork().getLinks().get(origAct12.getLinkId()).getFromNode();

		Node origN21 = this.sceanrio.getNetwork().getLinks().get(origAct21.getLinkId()).getToNode();
		Node origN22 = this.sceanrio.getNetwork().getLinks().get(origAct22.getLinkId()).getFromNode();

		Path path1 = this.router.calcLeastCostPath(origN11, origN22, origAct11.getEndTime());
		double newScore1 = path1.travelCost / -600;
		Path path2 = this.router.calcLeastCostPath(origN21, origN12, origAct21.getEndTime());
		double newScore2 = path2.travelCost / -600;

		if ((origScore1 + origScore2) < (newScore1 + newScore2)) {
//			System.out.println("old1:" + origScore1 + " old2:" + origScore2 + " new1:" + newScore1+ " new2:" + newScore2);
			Leg leg1 = (Leg)plan1.getPlanElements().get(1);
			NetworkRoute route1 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct11.getLinkId(), origAct22.getLinkId());
			route1.setLinkIds(origAct11.getLinkId(), NetworkUtils.getLinkIds(path1.links), origAct22.getLinkId());
			route1.setTravelTime((int) path1.travelTime);
			route1.setTravelCost(path1.travelCost);
			route1.setDistance(RouteUtils.calcDistance(route1, this.sceanrio.getNetwork()));
			leg1.setRoute(route1);

			Leg leg2 = (Leg)plan2.getPlanElements().get(1);
			NetworkRoute route2 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct21.getLinkId(), origAct12.getLinkId());
			route2.setLinkIds(origAct21.getLinkId(), NetworkUtils.getLinkIds(path2.links), origAct12.getLinkId());
			route2.setTravelTime((int) path2.travelTime);
			route2.setTravelCost(path2.travelCost);
			route2.setDistance(RouteUtils.calcDistance(route2, this.sceanrio.getNetwork()));
			leg2.setRoute(route2);
	
			((ActivityImpl)origAct12).setLinkId(route1.getEndLinkId());
			plan1.setScore(newScore1);
			((ActivityImpl)origAct22).setLinkId(route2.getEndLinkId());
			plan2.setScore(newScore2);
		}
	}







}
