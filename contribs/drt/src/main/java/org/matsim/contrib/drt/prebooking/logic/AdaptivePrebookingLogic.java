package org.matsim.contrib.drt.prebooking.logic;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.prebooking.PrebookingManager;
import org.matsim.contrib.drt.prebooking.logic.helpers.PrebookingQueue;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Preconditions;

/**
 * This is a prebooking logic that, whenever an agent starts an activity, checks
 * whether there is a DRT leg coming up before the next main (non-stage)
 * activity. If so, the upcoming leg is prebooked in advance with the
 * submissionSlack parameter defining how much in advance.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AdaptivePrebookingLogic implements PrebookingLogic, ActivityStartEventHandler, MobsimScopeEventHandler {
	private final QSim qsim;
	private final PrebookingManager prebookingManager;
	private final PrebookingQueue prebookingQueue;
	private final TimeInterpretation timeInterpretation;

	private final String mode;

	private final double submissionSlack;

	private AdaptivePrebookingLogic(String mode, QSim qsim, PrebookingManager prebookingManager,
			PrebookingQueue prebookingQueue, TimeInterpretation timeInterpretation, double submissionSlack) {
		this.prebookingManager = prebookingManager;
		this.prebookingQueue = prebookingQueue;
		this.qsim = qsim;
		this.mode = mode;
		this.timeInterpretation = timeInterpretation;
		this.submissionSlack = submissionSlack;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		MobsimAgent agent = qsim.getAgents().get(event.getPersonId());

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(event.getTime());

		/*
		 * Now we starting to traverse the remaining plan to see if we find a DRT leg
		 * that comes after the currently started activity. If so, we prebook it with
		 * the configured submission slack. We start searching one we reach the next
		 * non-stage activity type.
		 */

		for (int i = planElementIndex + 1; i < plan.getPlanElements().size(); i++) {
			PlanElement element = plan.getPlanElements().get(i);

			if (element instanceof Activity) {
				Activity activity = (Activity) element;

				if (!TripStructureUtils.isStageActivityType(activity.getType())) {
					break; // only consider legs coming directly after the ongoing activity
				}
			} else if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (mode.equals(leg.getMode())) {
					double departureTime = timeTracker.getTime().seconds();

					if (prebookingManager.getRequestId(leg) == null) {
						// only book legs that are not already prebooked

						double submissionTime = Math.max(event.getTime(), departureTime - submissionSlack);
						if (submissionTime > departureTime) {
							prebookingQueue.schedule(submissionTime, agent, leg, departureTime);
						}
					}
				}
			}

			timeTracker.addElement(element);
		}
	}

	static public AbstractDvrpModeQSimModule createModule(DrtConfigGroup drtConfig, double subissionSlack) {
		return new AbstractDvrpModeQSimModule(drtConfig.getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(AdaptivePrebookingLogic.class).toProvider(modalProvider(getter -> {
					Preconditions.checkState(drtConfig.getPrebookingParams().isPresent());

					return new AdaptivePrebookingLogic(drtConfig.getMode(), getter.get(QSim.class),
							getter.getModal(PrebookingManager.class), getter.getModal(PrebookingQueue.class),
							getter.get(TimeInterpretation.class), subissionSlack);
				}));
				addMobsimScopeEventHandlerBinding().to(modalKey(AdaptivePrebookingLogic.class));
			}
		};
	}

	static public void install(Controler controller, DrtConfigGroup drtConfig, double subissionSlack) {
		controller.addOverridingQSimModule(createModule(drtConfig, subissionSlack));
	}
}
