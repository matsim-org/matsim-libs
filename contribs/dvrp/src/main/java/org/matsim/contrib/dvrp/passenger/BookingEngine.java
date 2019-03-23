package org.matsim.contrib.dvrp.passenger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.core.router.TripRouter;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

public final class BookingEngine implements MobsimEngine {
	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered. On the other hand, it is easy to add an engine such as this one; how much does it help to have another
	// layer of infrastructure?  Am currently leaning towards the second argument.  kai, mar'19

	private static final Logger log = Logger.getLogger(BookingEngine.class);

	private final Map<String, TripInfo.Provider> tripInfoProviders = new LinkedHashMap<>();
	private final Map<String, AgentPlanUpdater> agentPlanUpdaters = new LinkedHashMap<>();
	private final Population population;

	private Map<MobsimAgent, Optional<TripInfo>> tripInfoUpdatesMap = new ConcurrentHashMap<>();
	// yyyy not sure about possible race conditions here! kai, feb'19

	private Map<MobsimAgent, TripInfoRequest> tripInfoRequestMap = new ConcurrentHashMap<>();

	private final EditTrips editTrips;
	private EditPlans editPlans = null;

	private final TripRouter tripRouter;

	@Inject
	BookingEngine(TripRouter tripRouter, Scenario scenario) {
		this.tripRouter = tripRouter;
		this.editTrips = new EditTrips(tripRouter, scenario);
		this.population = scenario.getPopulation();
	}

	@Override
	public void onPrepareSim() {
		//FIXME create AgentPlanUpdaters with guice
		new DrtAgentPlanUpdater(this);
		agentPlanUpdaters.values().forEach(updater -> updater.init(editTrips, editPlans));
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.editPlans = new EditPlans(internalInterface.getMobsim(), tripRouter, editTrips);
		for (DepartureHandler departureHandler : internalInterface.getDepartureHandlers()) {
			if (departureHandler instanceof TripInfo.Provider) {
				String mode = ((TripInfo.Provider)departureHandler).getMode();
				this.tripInfoProviders.put(mode, (TripInfo.Provider)departureHandler);
			}
		}
	}

	public void registerAgentPlanUpdater(String mode, AgentPlanUpdater agentPlanUpdater) {
		agentPlanUpdaters.put(mode, agentPlanUpdater);
		agentPlanUpdater.init(editTrips, editPlans);
	}

	@Override
	public void doSimStep(double time) {
		//first process requests  and then infos --> trips without booking required can be processed in 1 time step
		//booking confirmation always comes later (e.g. next time step)
		processTripInfoRequests();
		processTripInfoUpdates();
	}

	public synchronized final void notifyChangedTripInformation(MobsimAgent agent, Optional<TripInfo> tripInfoUpdate) {
		// (we are in the mobsim, so we don't need to play around with IDs)
		tripInfoUpdatesMap.put(agent, tripInfoUpdate);
	}

	public synchronized final void notifyTripInfoRequestSent(MobsimAgent agent, TripInfoRequest tripInfoRequest) {
		log.warn("entering notifyTripInfoRequestSent with agentId=" + agent.getId());
		tripInfoRequestMap.put(agent, tripInfoRequest);
	}

	private void processTripInfoRequests() {
		for (Map.Entry<MobsimAgent, TripInfoRequest> entry : tripInfoRequestMap.entrySet()) {
			log.warn("processing tripInfoRequests for agentId=" + entry.getKey().getId());
			Map<TripInfo, TripInfo.Provider> allTripInfos = new LinkedHashMap<>();
			for (TripInfo.Provider provider : tripInfoProviders.values()) {
				List<TripInfo> tripInfos = provider.getTripInfos(entry.getValue());
				for (TripInfo tripInfo : tripInfos) {
					allTripInfos.put(tripInfo, provider);
				}
			}

			// TODO add info for mode that is in agent plan, if not returned by trip info provider

			decide(entry.getKey(), allTripInfos);
		}

		tripInfoRequestMap.clear();
	}

	private void decide(MobsimAgent agent, Map<TripInfo, TripInfo.Provider> allTripInfos) {
		log.warn("entering decide for agentId=" + agent.getId());

		this.population.getPersons().get(agent.getId()).getAttributes().putAttribute(AgentSnapshotInfo.marker, true);

		if (allTripInfos.isEmpty()) {
			return;
		}

		log.warn("020");

		// to get started, we assume that we are only getting one drt option back.
		// TODO: make complete
		TripInfo tripInfo = allTripInfos.keySet().iterator().next();

		if (tripInfo instanceof TripInfoWithRequiredBooking) {
			log.warn("030");

			tripInfoProviders.get(tripInfo.getMode())
					.bookTrip((MobsimPassengerAgent)agent, (TripInfoWithRequiredBooking)tripInfo);
			// yyyy can't we really not use the tripInfo handle directly as I had it before?  kai, mar'15

			//to reduce number of possibilities, I would simply assume that notification always comes later
			//
			// --> yes, with DRT it will always come in the next time step, I adapted code accordingly (michal)

			// wait for notification:
			((Activity)WithinDayAgentUtils.getCurrentPlanElement(agent)).setEndTime(Double.MAX_VALUE);
			editPlans.rescheduleActivityEnd(agent);

			final Person person = this.population.getPersons().get(agent.getId());
			if (person != null) {
				if (person.getAttributes().getAttribute(AgentSnapshotInfo.marker) != null) {
					log.warn("040");
				}
			}

		} else {
			notifyChangedTripInformation(agent, Optional.of(tripInfo));//no booking here
		}
	}

	private void processTripInfoUpdates() {
		for (Map.Entry<MobsimAgent, Optional<TripInfo>> entry : tripInfoUpdatesMap.entrySet()) {
			MobsimAgent agent = entry.getKey();
			Optional<TripInfo> tripInfo = entry.getValue();

			if (tripInfo.isPresent()) {
				TripInfo actualTripInfo = tripInfo.get();
				agentPlanUpdaters.get(actualTripInfo.getMode()).updateAgentPlan(agent, actualTripInfo);
			} else {
				TripInfoRequest request = null;
				//TODO get it from where ??? from TripInfo???
				//TODO agent should adapt trip info request given that the previous one got rejected??
				//TODO or it should skip the rejected option during "accept()"
				notifyTripInfoRequestSent(agent, request);//start over again in the next time step
			}
		}

		tripInfoUpdatesMap.clear();
	}

	public interface AgentPlanUpdater {
		void init(EditTrips editTrips, EditPlans editPlans);

		void updateAgentPlan(MobsimAgent agent, TripInfo tripInfo);
	}
}
