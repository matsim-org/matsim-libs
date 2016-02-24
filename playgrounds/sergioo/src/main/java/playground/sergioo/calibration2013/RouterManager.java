package playground.sergioo.calibration2013;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouter;

import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWSImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTime;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTime;

public class RouterManager {
	
	final static int CAR_POSITION = 0;
	final static int PT_POSITION = 1;
	final static int WALK_POSITION = 2;
	final static int NUM_MODES = 3;
	
	private AtomicInteger numFinished;
	private Router[] threads;
	private SortedMap<Double, int[]> distances = new TreeMap<Double, int[]>();
	private int position = 0;
	private Scenario scenario;
	private TravelTime travelTime;
	private WaitTime waitTime;
	private StopStopTime stopStopTime;
	
	private synchronized void addDistance(double limit, int pos) {
		int[] nums = distances.get(limit);
		if(nums == null) {
			nums = new int[NUM_MODES];
			distances.put(limit, nums);
		}
		nums[pos]++;
	}
	
	private class Router extends Thread {
		private final List<Plan> plans = new ArrayList<Plan>();
		private final TransitRouter transitRouter;
		private final LeastCostPathCalculator router;
		private final Scenario scenario;
		
		public Router(Scenario scenario, TravelTime travelTime, WaitTime waitTime, StopStopTime stopStopTime) {
			final TravelDisutility disutility = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() ).createTravelDisutility(travelTime);
			transitRouter = new TransitRouterWSImplFactory(scenario, waitTime, stopStopTime).get();
			router = new DijkstraFactory().createPathCalculator(scenario.getNetwork(), disutility, travelTime);
			this.scenario = scenario;
		}
		@Override
		public void run() {
			for(Plan plan:plans) {
				new TreeMap<Double, int[]>();
				for(int i=0; i<plan.getPlanElements().size(); i++) {
					PlanElement planElement = plan.getPlanElements().get(i);
					if(planElement instanceof Leg) {
						double[] costs = new double[NUM_MODES];
						double[] distances = new double[NUM_MODES];
						Activity prev = (Activity) plan.getPlanElements().get(i-1);
						Activity next = (Activity) plan.getPlanElements().get(i+1);
						Path path = null;
						if(PersonUtils.getCarAvail(plan.getPerson()).equals("never")) {
							costs[CAR_POSITION] = Double.POSITIVE_INFINITY;
							distances[CAR_POSITION] = Double.POSITIVE_INFINITY;
						}
						else {
							path = router.calcLeastCostPath(scenario.getNetwork().getLinks().get(prev.getLinkId()).getToNode(), scenario.getNetwork().getLinks().get(next.getLinkId()).getToNode(), prev.getEndTime(), null, null);
							costs[CAR_POSITION] = path==null?Double.POSITIVE_INFINITY:path.travelCost;
							if(path!=null)
								for(Link link:path.links)
									distances[CAR_POSITION] += link.getLength();
							else
								distances[CAR_POSITION] = Double.POSITIVE_INFINITY;
						}
						path = ((TransitRouterVariableImpl)transitRouter).calcPathRoute(prev.getCoord(), next.getCoord(), prev.getEndTime(), null);
						double walkSpeed = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) / 
								scenario.getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
						costs[PT_POSITION] = path==null?Double.POSITIVE_INFINITY:path.travelCost;
						if(path!=null) {
							for(Link link:path.links)
								if(((TransitRouterNetworkLink)link).getRoute()!=null)
									distances[PT_POSITION] += RouteUtils.calcDistanceExcludingStartEndLink(((TransitRouterNetworkLink)link).getRoute().getRoute().getSubRoute(((TransitRouterNetworkLink)link).getFromNode().stop.getStopFacility().getLinkId(), ((TransitRouterNetworkLink)link).getToNode().stop.getStopFacility().getLinkId()), scenario.getNetwork());
								else if(((TransitRouterNetworkLink)link).getFromNode().getRoute()==null && ((TransitRouterNetworkLink)link).getToNode().getRoute()==null)
									distances[PT_POSITION] += CoordUtils.calcEuclideanDistance(((TransitRouterNetworkLink)link).getFromNode().getCoord(), ((TransitRouterNetworkLink)link).getToNode().getCoord());
							double startDistance = CoordUtils.calcEuclideanDistance(prev.getCoord(), path.nodes.get(0).getCoord());
							double endDistance = CoordUtils.calcEuclideanDistance(path.nodes.get(path.nodes.size()-1).getCoord(), next.getCoord());
							distances[PT_POSITION] += startDistance;
							distances[PT_POSITION] += endDistance;
							costs[PT_POSITION] += startDistance/walkSpeed * (0 - scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() /3600.0);
							costs[PT_POSITION] += endDistance/walkSpeed * (0 - scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() /3600.0);
						}
						else
							distances[PT_POSITION] = Double.POSITIVE_INFINITY;
						distances[WALK_POSITION] = CoordUtils.calcEuclideanDistance(prev.getCoord(), next.getCoord());
						costs[WALK_POSITION] = distances[WALK_POSITION]/walkSpeed * (0 - scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() /3600.0);
						int bestPosition = getBestPosition(costs);
						for(int p = 0; p<NUM_MODES; p++)
							if(costs[p]<costs[bestPosition])
								bestPosition = p;
						boolean in = false;
						LIMITS:
						for(double limit:GeneticAlgorithmMode.limits)
							if(distances[bestPosition]<limit) {
								addDistance(limit, bestPosition);
								in = true;
								break LIMITS;
							}
						if(!in)
							addDistance(Double.POSITIVE_INFINITY, bestPosition);
					}
				}
			}
			numFinished.incrementAndGet();
		}
		private int getBestPosition(double[] costs) {
			double den = 0;
			for(double cost:costs)
				den += Math.exp(-cost);
			double[] probs = new double[costs.length];
			for(int i=0; i<probs.length; i++)
				probs[i] = Math.exp(-costs[i])/den;
			double rand = Math.random(), sum = 0;
			for(int i=0; i<probs.length; i++) {
				sum+=probs[i];
				if(rand<sum)
					return i;
			}
			return -1;
		}
	
	}
	
	public RouterManager(int numTheads, Scenario scenario, TravelTime travelTime, WaitTime waitTime, StopStopTime stopStopTime) {
		threads = new Router[numTheads];
		for(int i=0; i<threads.length; i++)
			threads[i] = new Router(scenario, travelTime, waitTime, stopStopTime);this.scenario = scenario;
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		this.stopStopTime = stopStopTime;
	}
	public void addPlan(Plan plan) {
		threads[position].plans.add(plan);
		position++;
		if(position==threads.length)
			position = 0;
	}
	public SortedMap<Double, int[]> getDistribution() {
		numFinished = new AtomicInteger(0);
		for(int i=0; i<threads.length; i++)
			threads[i].start();
		while(numFinished.intValue()<threads.length)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		for(int i=0; i<threads.length; i++)
			threads[i] = new Router(scenario, travelTime, waitTime, stopStopTime);
		return distances;
	}
}
