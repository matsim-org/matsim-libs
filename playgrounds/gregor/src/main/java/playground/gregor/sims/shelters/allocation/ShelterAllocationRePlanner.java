package playground.gregor.sims.shelters.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
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

	private static final Logger log = Logger.getLogger(ShelterAllocationRePlanner.class);


	private static final double FULL_RED_BOUNDERY = 600;
	//	private Population plans;
	private Dijkstra router;
	private ScenarioImpl scenario;
	private List<Building> buildings;
	private ArrayList<Person> agents;

	private double PSHELTER;
	//	private static final double EPSILON = 0.005;

	private boolean initialized = false;
	private int c = 0;
	private NetworkFactoryImpl routeFactory;
	private ShelterCounter shc;
	private TravelTime tt;


	private final boolean swNash;

	public ShelterAllocationRePlanner(ScenarioImpl sc, TravelCost tc, TravelTime tt, List<Building> buildings) {
		//		this.plans = sc.getPopulation();
		//		this.router =  new Dijkstra(sc.getNetwork(),tc,tt);
		//		this.sceanrio = sc;
		//		this.buildings = buildings;
		//		this.agents = new ArrayList<Person>(sc.getPopulation().getPersons().values());
		//		this.routeFactory = (NetworkFactoryImpl) sc.getNetwork().getFactory();
		this(sc,tc,tt,buildings,null,0);
	}

	public ShelterAllocationRePlanner(ScenarioImpl sc, TravelCost tc, TravelTime tt, List<Building> buildings,ShelterCounter shc, double pshelter) {
		this.router =  new Dijkstra(sc.getNetwork(),tc,tt);
		this.tt = tt;
		this.scenario = sc;
		this.buildings = new ArrayList<Building>();
		for (Building b : buildings) {
			if (b.isQuakeProof()) {
				this.buildings.add(b);
			}
		}
		this.agents = new ArrayList<Person>(sc.getPopulation().getPersons().values());
		this.routeFactory = (NetworkFactoryImpl) sc.getNetwork().getFactory();
		this.shc = shc;//
		this.PSHELTER = pshelter;
		this.swNash = !sc.getConfig().evacuation().isSocialCostOptimization();

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
//		if (event.getIteration() > 200) {
//			this.PSHELTER -= 1./(100 * 100.);
//		}
		
		if (event.getIteration() > 1) {
			run();		
		} else if (event.getIteration() > 0 && this.PSHELTER > 0) {
			shuffle();
		}
	}


	private void run() {


//		log.info("PSHELTER:" + this.PSHELTER);
		if (!this.initialized) {
			init();
		}
		Collections.shuffle(this.agents,MatsimRandom.getRandom());
		this.shc.reset(1,this.agents);

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
		//		double switchProb = (double)this.shc.getNumAgentsInShelter() / (double)this.c;

		if (this.shc != null) {
			if (rand.nextDouble() < 0.5) {
				doSwitchTwoAgents(pers,rand);
			} else {
				doShiftOneAgent(pers, rand);
			}
		} else {
			doSwitchTwoAgents(pers,rand);
		}

		//		doSwitchTwoAgents(pers,rand);
	}

	private void doShiftOneAgent(Person pers, Random rand) {

		//		if (pers.getId().toString().equals("10290")) {
		//			int i = 0;
		//			i++;
		//		}

		Id id = null;
		while (id == null) {
			Building b = this.buildings.get(rand.nextInt(this.buildings.size()));
			id = this.shc.tryToAddAgent(b);
		}
		Plan plan = pers.getSelectedPlan();
		((PersonImpl)pers).removeUnselectedPlans();


		if (Double.isNaN(plan.getScore())) {
			throw new RuntimeException("origScore1 is NaN agent:" + pers.getId());
		}

		Activity origAct1 = (Activity)plan.getPlanElements().get(0);
		Node origN1 = this.scenario.getNetwork().getLinks().get(origAct1.getLinkId()).getToNode();
		Node test = this.scenario.getNetwork().getLinks().get(id).getFromNode();


		Leg leg = (Leg)plan.getPlanElements().get(1);
		NetworkRoute origRoute1 = (NetworkRoute) leg.getRoute();
		double deltaTBefore = getTimeToWave(origAct1.getEndTime(), origRoute1.getLinkIds());
		boolean surviveBefore = deltaTBefore > 0;
		double scoreBefore = plan.getScore();

		Path path = this.router.calcLeastCostPath(origN1, test, origAct1.getEndTime());
		double scoreAfter = path.travelCost/-600;

		NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct1.getLinkId(), id);
		route.setLinkIds(origAct1.getLinkId(), NetworkUtils.getLinkIds(path.links), id);
		route.setTravelTime((int) path.travelTime);
		route.setTravelCost(path.travelCost);
		route.setDistance(RouteUtils.calcDistance(route, this.scenario.getNetwork()));

		double deltaAfter = getTimeToWave(origAct1.getEndTime(), route.getLinkIds());
		boolean surviveAfter = deltaAfter > 0;

		boolean shift = false;
		if (!surviveBefore && surviveAfter) {
			shift = true;
		} else if (surviveBefore && surviveAfter && scoreBefore < scoreAfter) {
			shift = true;
		}
		if (path.travelTime > 120 * 60) {
			shift = false;
		}
		//			if (origDeltaT1 < newDeltaT1) {
		//				shift = true;
		//			}else	if ((plan.getScore() < testScore) && (origDeltaT1 == newDeltaT1)) {
		//				shift = true;
		//			}
		//		}



		if (shift) {

			Activity origAct2 = (Activity)plan.getPlanElements().get(2);
			this.shc.rm(origAct2.getLinkId());

			leg.setRoute(route);
			((ActivityImpl)origAct2).setLinkId(id);
			plan.setScore(scoreAfter);
		} else {
			this.shc.rm(id);
		}
		if (Double.isNaN(plan.getScore())) {
			throw new RuntimeException("testScore is NaN");
		}

		//		id = shc.tryToAddAgent(b)
	}

	private void doSwitchTwoAgents(Person pers1, Random rand) {

		Person pers2 = this.agents.get(rand.nextInt(this.agents.size()));
		double score1Before = pers1.getSelectedPlan().getScore();
		double score2Before = pers2.getSelectedPlan().getScore();


		//DEBUG
		if (Double.isNaN(score1Before)) {
			throw new RuntimeException("origScore1 is NaN  agent:" + pers1.getId());
			//			origScore1 = -145;
		}
		if (Double.isNaN(score1Before)) {
			throw new RuntimeException("origScore2 is NaN  agent:" + pers2.getId());
			//			origScore2 = -145;
		}


		((PersonImpl)pers1).removeUnselectedPlans();
		((PersonImpl)pers2).removeUnselectedPlans();

		Plan plan1 = pers1.getSelectedPlan();
		Plan plan2 = pers2.getSelectedPlan();


		Activity origAct11 = (Activity)plan1.getPlanElements().get(0);
		Activity origAct12 = (Activity)plan1.getPlanElements().get(2);
		Activity origAct21 = (Activity)plan2.getPlanElements().get(0);
		Activity origAct22 = (Activity)plan2.getPlanElements().get(2);

		if (origAct12.getLinkId() == origAct22.getLinkId()) {
			return;
		}

		Node origN11 = this.scenario.getNetwork().getLinks().get(origAct11.getLinkId()).getToNode();
		Node origN12 = this.scenario.getNetwork().getLinks().get(origAct12.getLinkId()).getFromNode();

		Node origN21 = this.scenario.getNetwork().getLinks().get(origAct21.getLinkId()).getToNode();
		Node origN22 = this.scenario.getNetwork().getLinks().get(origAct22.getLinkId()).getFromNode();

		Leg leg1 = (Leg)plan1.getPlanElements().get(1);
		NetworkRoute origRoute1 = (NetworkRoute) leg1.getRoute();
		double delta1Before = getTimeToWave(origAct11.getEndTime(), origRoute1.getLinkIds());
		boolean survive1Before = delta1Before > 0;

		Path path1 = this.router.calcLeastCostPath(origN11, origN22, origAct11.getEndTime());
		NetworkRoute route1 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct11.getLinkId(), origAct22.getLinkId());
		route1.setLinkIds(origAct11.getLinkId(), NetworkUtils.getLinkIds(path1.links), origAct22.getLinkId());
		route1.setTravelTime((int) path1.travelTime);
		route1.setTravelCost(path1.travelCost);
		route1.setDistance(RouteUtils.calcDistance(route1, this.scenario.getNetwork()));

		double score1After = path1.travelCost / -600;
		double delta1After = getTimeToWave(origAct11.getEndTime(), route1.getLinkIds());
		boolean survive1After = delta1After > 0;
		boolean urgent1 = !survive1Before & survive1After;
		boolean invalid1 = survive1Before & !survive1After;

		Leg leg2 = (Leg)plan2.getPlanElements().get(1);
		NetworkRoute origRoute2 = (NetworkRoute) leg2.getRoute();
		double delta2Before = getTimeToWave(origAct21.getEndTime(), origRoute2.getLinkIds());
		boolean survive2Before = delta2Before > 0;


		Path path2 = this.router.calcLeastCostPath(origN21, origN12, origAct21.getEndTime());
		NetworkRoute route2 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct21.getLinkId(), origAct12.getLinkId());
		route2.setLinkIds(origAct21.getLinkId(), NetworkUtils.getLinkIds(path2.links), origAct12.getLinkId());
		route2.setTravelTime((int) path2.travelTime);
		route2.setTravelCost(path2.travelCost);
		route2.setDistance(RouteUtils.calcDistance(route2, this.scenario.getNetwork()));

		double score2After = path2.travelCost / -600;
		double delta2After = getTimeToWave(origAct21.getEndTime(), route2.getLinkIds());
		boolean survive2After = delta2After > 0;

		boolean urgent2 = !survive2Before & survive2After;
		boolean invalid2 = survive2Before & !survive2After;

		boolean switchAgents = switchAgents(invalid1,invalid2,urgent1,urgent2,score1Before,score1After,score2Before,score2After);




		if (path1.travelTime > 120 * 60 || path2.travelTime > 120 * 60) {
			switchAgents = false;
		}

		//		if (path1.travelTime < 60 * 120 && path2.travelTime < 60 *120) {
		//			//agent 1 would benefit  
		//			if (origDeltaT1 < newDeltaT1 || ( (origDeltaT1 == newDeltaT1) && (origScore1 < newScore1) ) ) {
		//	
		//				//agent 1 would also benefit
		//				if (origDeltaT2 < newDeltaT2 || ( (origDeltaT2 == newDeltaT2) && (origScore2 < newScore2) ) ) {
		//					switchAgents = true;
		//				}
		//				
		//			}
		//		}




		//		if ((origScore1 + origScore2) < (newScore1 + newScore2)  ) {
		if (switchAgents) {
			//			System.out.println("old1:" + origScore1 + " old2:" + origScore2 + " new1:" + newScore1+ " new2:" + newScore2);


			leg1.setRoute(route1);


			leg2.setRoute(route2);

			((ActivityImpl)origAct12).setLinkId(route1.getEndLinkId());
			plan1.setScore(score1After);
			((ActivityImpl)origAct22).setLinkId(route2.getEndLinkId());
			plan2.setScore(score2After);


		}
		if (Double.isNaN(score1After)) {
			throw new RuntimeException("newScore1 is NaN");
		}
		if (Double.isNaN(score2After)) {
			throw new RuntimeException("newScore2 is NaN");
		}	

		//		if (debug && (pers1.getId().toString().equals("3623") || pers2.getId().toString().equals("3623")) ) {
		//			int ii = 0;
		//			ii++;
		//		}

	}


	private boolean switchAgents(boolean invalid1, boolean invalid2,
			boolean urgent1, boolean urgent2, double score1Before,
			double score1After, double score2Before, double score2After) {

		boolean switchAgents = false;
		if (!(invalid1 || invalid2)) {
			if (urgent1 || urgent2) {
				switchAgents = true;

			}else {
				if (this.swNash && (score1Before < score1After) && (score2Before < score2After)) { //NASH
					switchAgents = true;
				}
				else if (!this.swNash && (score1Before + score2Before) < (score1After + score2After)) { //SO
					switchAgents = true;
				}		
			}

		}


		return switchAgents;
	}

	private double getTimeToWave(double startTime, List<Id> list) {
		//		double maxDeltaT = Double.POSITIVE_INFINITY;
		double currentTime =  startTime;
		double minDelta = FULL_RED_BOUNDERY;
		for (Id lId : list) {
			Link l = this.scenario.getNetwork().getLinks().get(lId);
			TreeMap<Double, NetworkChangeEvent> ce = ((TimeVariantLinkImpl)l).getChangeEvents();
			if (ce == null) {
				continue;
			}
			for (Entry<Double, NetworkChangeEvent>  entr : ce.entrySet()) {
				ChangeValue v = entr.getValue().getFreespeedChange();
				if (v != null && v.getValue() == 0.) {
					double delta = entr.getKey() - currentTime;

					//BEGIN DEBUG
					//					if (delta < 0) {
					//						throw new RuntimeException("if and only if delta < 0  then deltaT < 0");
					//					}
					//END DEBUG
					if (delta < minDelta) {
						minDelta = delta;
					}
					//					delta = Math.min(delta,FULL_RED_BOUNDERY);
					//					return delta;

				}
			}
			currentTime += this.tt.getLinkTravelTime(l, currentTime);
		}
		//The agent does not start inside the inundation area
		//This means the deltaT would have to be set to Double.POSITIVE_INFINITY however this would have 
		//unwanted side effects!
		return minDelta;
	}



	//debug
	private void shuffle() {
		if (!this.initialized) {
			init();
		}
		Collections.shuffle(this.agents,MatsimRandom.getRandom());
		this.shc.reset(1,this.agents);
		//		for (Person pers : this.agents) {
		//			((PersonImpl)pers).removeUnselectedPlans();	
		//			this.shc.testAdd(((ActivityImpl)pers.getSelectedPlan().getPlanElements().get(2)).getLinkId());
		//		}
		Random rand = MatsimRandom.getRandom();
		for (Person pers : this.agents) {


			doRePlanningShuffle(pers, rand);

		}
	}

	private void doRePlanningShuffle(Person pers, Random rand) {
		if (this.shc != null) {
			if (rand.nextDouble() < 0.5) {
				doSwitchTwoAgentsRandom(pers,rand);
			} else {
				doShiftOneAgentRandom(pers,rand);
			}
		} 

	}

	private void doShiftOneAgentRandom(Person pers, Random rand) {
		Id id = null;
		while (id == null) {
			Building b = this.buildings.get(rand.nextInt(this.buildings.size()));
			id = this.shc.tryToAddAgent(b);
		}
		Plan plan = pers.getSelectedPlan();
		((PersonImpl)pers).removeUnselectedPlans();

		Activity origAct1 = (Activity)plan.getPlanElements().get(0);
		Node origN1 = this.scenario.getNetwork().getLinks().get(origAct1.getLinkId()).getToNode();
		Node test = this.scenario.getNetwork().getLinks().get(id).getFromNode();

		Path path = this.router.calcLeastCostPath(origN1, test, origAct1.getEndTime());
		double testScore = path.travelCost/-600;
		Leg leg = (Leg)plan.getPlanElements().get(1);

		if (path.travelTime < 120*60) {
			Activity origAct2 = (Activity)plan.getPlanElements().get(2);
			this.shc.rm(origAct2.getLinkId());
			NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct1.getLinkId(), id);
			route.setLinkIds(origAct1.getLinkId(), NetworkUtils.getLinkIds(path.links), id);
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, this.scenario.getNetwork()));
			leg.setRoute(route);
			((ActivityImpl)origAct2).setLinkId(id);
			plan.setScore(testScore);
		} else {
			this.shc.rm(id);
		}



	}

	private void doSwitchTwoAgentsRandom(Person pers1, Random rand) {
		Person pers2 = this.agents.get(rand.nextInt(this.agents.size()));

		((PersonImpl)pers1).removeUnselectedPlans();
		((PersonImpl)pers2).removeUnselectedPlans();

		Plan plan1 = pers1.getSelectedPlan();
		Plan plan2 = pers2.getSelectedPlan();

		//	if (debug && (pers1.getId().toString().equals("3623") || pers2.getId().toString().equals("3623")) ) {
		//		int ii = 0;
		//		ii++;
		//	}

		Activity origAct11 = (Activity)plan1.getPlanElements().get(0);
		Activity origAct12 = (Activity)plan1.getPlanElements().get(2);
		Activity origAct21 = (Activity)plan2.getPlanElements().get(0);
		Activity origAct22 = (Activity)plan2.getPlanElements().get(2);

		Node origN11 = this.scenario.getNetwork().getLinks().get(origAct11.getLinkId()).getToNode();
		Node origN12 = this.scenario.getNetwork().getLinks().get(origAct12.getLinkId()).getFromNode();

		Node origN21 = this.scenario.getNetwork().getLinks().get(origAct21.getLinkId()).getToNode();
		Node origN22 = this.scenario.getNetwork().getLinks().get(origAct22.getLinkId()).getFromNode();

		Path path1 = this.router.calcLeastCostPath(origN11, origN22, origAct11.getEndTime());
		double newScore1 = path1.travelCost / -600;
		Path path2 = this.router.calcLeastCostPath(origN21, origN12, origAct21.getEndTime());
		double newScore2 = path2.travelCost / -600;



		if (path1.travelTime < 120*60 && path2.travelTime < 120*60) {
			//		System.out.println("old1:" + origScore1 + " old2:" + origScore2 + " new1:" + newScore1+ " new2:" + newScore2);
			NetworkRoute route1 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct11.getLinkId(), origAct22.getLinkId());
			route1.setLinkIds(origAct11.getLinkId(), NetworkUtils.getLinkIds(path1.links), origAct22.getLinkId());
			route1.setTravelTime((int) path1.travelTime);
			route1.setTravelCost(path1.travelCost);
			route1.setDistance(RouteUtils.calcDistance(route1, this.scenario.getNetwork()));
			Leg leg1 = (Leg)plan1.getPlanElements().get(1);
			leg1.setRoute(route1);

			NetworkRoute route2 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, origAct21.getLinkId(), origAct12.getLinkId());
			route2.setLinkIds(origAct21.getLinkId(), NetworkUtils.getLinkIds(path2.links), origAct12.getLinkId());
			route2.setTravelTime((int) path2.travelTime);
			route2.setTravelCost(path2.travelCost);
			route2.setDistance(RouteUtils.calcDistance(route2, this.scenario.getNetwork()));
			Leg leg2 = (Leg)plan2.getPlanElements().get(1);
			leg2.setRoute(route2);

			((ActivityImpl)origAct12).setLinkId(route1.getEndLinkId());
			plan1.setScore(newScore1);
			((ActivityImpl)origAct22).setLinkId(route2.getEndLinkId());
			plan2.setScore(newScore2);
		}



	}



}
