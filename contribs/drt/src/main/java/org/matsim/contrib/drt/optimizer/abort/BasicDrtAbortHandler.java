package org.matsim.contrib.drt.optimizer.abort;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbortHandler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import java.util.*;

/**
 * Rejected DRT requests will be teleported to the destination based on max travel time
 * <p>
 * Author: kai
 * */
public class BasicDrtAbortHandler implements AbortHandler, MobsimEngine {
	public static final String COMPONENT_NAME = "DrtAbortHandler";
	static final String walkAfterRejectMode = "walkAfterReject";
	private static final String delimiter = "============";
	@Inject
	Network network;
	@Inject
	Population population;
	@Inject
	MobsimTimer mobsimTimer;

	TravelTime travelTime;
	LeastCostPathCalculator router;
	private static final Logger log = LogManager.getLogger(BasicDrtAbortHandler.class );
	private InternalInterface internalInterface;
	private final List<MobsimAgent> agents = new ArrayList<>();

	List<String> drtModes = new ArrayList<>();
	Map<String, Double> alphas = new HashMap<>();
	Map<String, Double> betas = new HashMap<>();

	@Inject
	BasicDrtAbortHandler(Network network, Map<String, TravelTime> travelTimeMap, Config config) {
		travelTime = travelTimeMap.get(TransportMode.car);
		router = new SpeedyALTFactory().createPathCalculator(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

		for (DrtConfigGroup modalElement : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			drtModes.add(modalElement.mode);
			alphas.put(modalElement.mode, modalElement.maxTravelTimeAlpha);
			betas.put(modalElement.mode, modalElement.maxTravelTimeBeta);
		}
	}

	@Override
	public boolean handleAbort(MobsimAgent agent) {
		log.warn("need to handle abort of agent=" + agent);
		PopulationFactory pf = population.getFactory();

		if (drtModes.contains(agent.getMode())) {
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

			printPlan("\n current plan=", plan);

			int index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

			Id<Link> interactionLink = agent.getCurrentLinkId();

			double now = mobsimTimer.getTimeOfDay();

			Leg leg = (Leg) plan.getPlanElements().get(index);

			Id<Link> originallyPlannedDestinationLink = leg.getRoute().getEndLinkId();

			// (1) The current leg needs to be modified so that it ends at the current location.  And one should somehow tag this
			// as a failed drt trip.  (There is presumably already a drt rejected event, so this info could also be reconstructed.)
			{
				leg.setDepartureTime(now);
				leg.setTravelTime(0);
				leg.setRoute(pf.getRouteFactories().createRoute(GenericRouteImpl.class, interactionLink, interactionLink));
				// (startLinkId and endLinkId are _only_ in the route)
			}

			// (2) An interaction activity needs to be inserted.
			{
				Coord interactionCoord = network.getLinks().get(interactionLink).getCoord();
//					Activity activity = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix( interactionCoord, interactionLink, walkAfterRejectMode );
				Activity activity = pf.createActivityFromCoord(TripStructureUtils.createStageActivityType(walkAfterRejectMode), interactionCoord);
				activity.setMaximumDuration(1.);
				plan.getPlanElements().add(index + 1, activity);
				// (inserts at given position; pushes everything else forward)
			}

			// (3) There needs to be a new teleportation leg from here to there.
			{
//					Leg secondLeg = pf.createLeg( walkAfterRejectMode+"_"+agent.getMode() );
				Leg secondLeg = pf.createLeg(walkAfterRejectMode);
				secondLeg.setDepartureTime(now);

				double directTravelTime = VrpPaths.calcAndCreatePath
					(network.getLinks().get(interactionLink), network.getLinks().get(originallyPlannedDestinationLink), now, router, travelTime).getTravelTime();
				double estimatedTravelTime = alphas.get(agent.getMode()) * directTravelTime + betas.get(agent.getMode());
				secondLeg.setTravelTime(estimatedTravelTime);
				secondLeg.setRoute(pf.getRouteFactories().createRoute(GenericRouteImpl.class, interactionLink, originallyPlannedDestinationLink));
				plan.getPlanElements().add(index + 2, secondLeg);

			}

			// (4) reset the agent caches:
			WithinDayAgentUtils.resetCaches(agent);

			// (5) add the agent to an internal list, which is processed during doSimStep, which formally ends the current
			// (aborted) leg, and moves the agent forward in its state machine.
			agents.add(agent);

			printPlan("plan after splicing=", plan);
		}
		return true;
	}

	@Override
	public void doSimStep(double time) {
		for (MobsimAgent agent : agents) {
			agent.endLegAndComputeNextState(time);
			// (we haven't actually thrown an abort event, and are planning not to do this here.  We probably have thrown a drt
			// rejected event.  The "endLeg..." method will throw a person arrival event.)

			this.internalInterface.arrangeNextAgentState(agent);
		}
		agents.clear();
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	private static void printPlan(String x, Plan plan) {
		log.warn(delimiter);
		log.warn(x + plan);
		for (PlanElement planElement : plan.getPlanElements()) {
			log.warn(planElement);
		}
		log.warn(delimiter);
	}
}
