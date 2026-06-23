package org.matsim.codeexamples.withinday.externalModeChoice;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import java.util.List;

import static org.matsim.codeexamples.withinday.externalModeChoice.RunExternalModeChoice.REINFORCEMENT_MODE;

public class RLModeChoiceListener implements MobsimBeforeSimStepListener {
    private static final Logger log = LogManager.getLogger(RLModeChoiceListener.class);

    @Inject
    TripRouter router;

    @Inject
    Scenario scenario;

    @Inject
    TimeInterpretation timeInterpretation;

    @Inject
    WithinDayModeChoice modeChoice;

    private EditTrips editTrips;

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        initEditTrips();

        // Pick all agents that end their activity
        QSim sim = (QSim) e.getQueueSimulation();
        sim.getAgents().values().stream()
                // filter all agents who are performing an activity and whose activity is going to end in this time step.
                .filter(p -> p.getState() == MobsimAgent.State.ACTIVITY)
                .filter(p -> p.getActivityEndTime() == e.getSimulationTime())
                .forEach(p -> replanNextTrip(p, sim));
    }

    private void replanNextTrip(MobsimAgent agent, QSim sim) {
        log.info("Replacing next trip for agent {} at time {}", agent.getId(), agent.getActivityEndTime());

        if (!(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity)) {
            throw new RuntimeException("For replanning the next trip, we expect the current plan element to be an activity, but it is not. Agent: " + agent.getId());
        }

        Integer currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
        TripStructureUtils.Trip oldNextTrip = EditTrips.findTripAtPlanElementIndex(agent, currentPlanElementIndex + 1);

        if (oldNextTrip.getLegsOnly().size() > 1) {
            log.warn("Replaning a next trip {} for agent {} that consists of more than one leg. This might work, but check if this is really what you want.", oldNextTrip, agent.getId());
        }

        if (!TripStructureUtils.identifyMainMode(oldNextTrip.getTripElements()).equals(REINFORCEMENT_MODE)) {
            // the new mode is not the reinforcement learning mode, so we don't need to replan.
            return;
        }

        // Mode choice here
        String newMode = modeChoice.chooseMode(agent, agent.getActivityEndTime());

        // Route next trip
        List<? extends PlanElement> newNextTrip = editTrips.replanFutureTrip(oldNextTrip, WithinDayAgentUtils.getModifiablePlan(agent), newMode, agent.getActivityEndTime());

        if (!sim.getScenario().getConfig().qsim().getMainModes().contains(newMode)) {
            // the new mode is not a main mode, so we don't need to add a vehicle to the simulation.
            return;
        }

        WithinDayAgentUtils.addVehicleToQSimIfNecessary(newNextTrip, scenario, sim);
    }

    // This method can be used to adjust the activity end time if needed.
    private void rescheduleActivityEnd(MobsimAgent agent, QSim sim) {
        EditPlans.rescheduleCurrentActivityEndtime(agent, 0., sim);
    }

    private void initEditTrips() {
        // lazy instantiation of EditTrips
        if (editTrips == null) {
            // internalInterface is null on purpose. This is only needed if current legs are replanned. But we are replacing future trips only (i.e. before activity ends).
            editTrips = new EditTrips(router, scenario, null, timeInterpretation);
        }
    }

    // You can define this interface as you like. This method signature is a suggestion only and really simple. You might want to
    // add more parameters. Also, you can change the return. You also might consider add functionality to adjust the activity end time.
    interface WithinDayModeChoice {
        String chooseMode(MobsimAgent agent, double time);
    }

    static class CarModeChoice implements WithinDayModeChoice {
        @Override
        public String chooseMode(MobsimAgent agent, double time) {
            return TransportMode.car;
        }
    }
}
