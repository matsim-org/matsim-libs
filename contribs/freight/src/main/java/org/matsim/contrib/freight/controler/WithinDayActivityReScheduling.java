package org.matsim.contrib.freight.controler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.freight.carrier.Tour.Start;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.controler.CarrierAgent.CarrierDriverAgent;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

/*
 * Physically enforces beginnings of time windows for freight activities, i.e. freight agents
 * wait before closed doors until they can deliver / pick up their goods.
 * 
 * Required because there is no way to encode this in a MATSim plan. There is no actual
 * within-day replanning taking place. What we would need is an ActivityDurationInterpretation
 * which allows this. Then this could go away.
 * 
 */
class WithinDayActivityReScheduling implements MobsimListener, MobsimBeforeSimStepListener {
	public static final String COMPONENT_NAME=WithinDayActivityReScheduling.class.getSimpleName() ;

	private static Logger logger = Logger.getLogger(WithinDayActivityReScheduling.class);
	
	private FreightAgentSource freightAgentSource;
	
	private Set<Activity> encounteredActivities = new HashSet<Activity>();

	private CarrierAgentTracker carrierAgentTracker;

	@Inject
	WithinDayActivityReScheduling(FreightAgentSource freightAgentSource, CarrierAgentTracker carrierAgentTracker) {
		this.freightAgentSource = freightAgentSource;
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		Collection<MobsimAgent> agentsToReplan = freightAgentSource.getMobSimAgents();
		for (MobsimAgent pa : agentsToReplan) {
			doReplanning(pa, e.getSimulationTime(), e.getQueueSimulation());
		}
	}

	private void doReplanning( MobsimAgent mobsimAgent, double time, Mobsim mobsim ) {
		PlanAgent planAgent = (PlanAgent) mobsimAgent;
		Id<Person> agentId = planAgent.getCurrentPlan().getPerson().getId();
		PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(mobsimAgent);
		if (currentPlanElement instanceof Activity) {
			Activity act = (Activity) currentPlanElement;
			if (encounteredActivities.contains(act)) {
				return;
			}
			CarrierDriverAgent driver = carrierAgentTracker.getDriver(agentId);
			TourActivity plannedActivity = (TourActivity) driver.getPlannedTourElement(WithinDayAgentUtils.getCurrentPlanElementIndex(mobsimAgent));
			if (plannedActivity instanceof Start){
				encounteredActivities.add(act);
			} else {
				double newEndTime = Math.max(time, plannedActivity.getTimeWindow().getStart()) + plannedActivity.getDuration();
//				logger.info("[agentId="+ agentId + "][currentTime="+Time.writeTime(time)+"][actDuration="+plannedActivity.getDuration()+
//						"[timeWindow="+ plannedActivity.getTimeWindow() + "][plannedActEnd="+ Time.writeTime(act.getEndTime()) + "][newActEnd="+Time.writeTime(newEndTime)+"]");
				act.setMaximumDuration(Time.getUndefinedTime());
				act.setEndTime(newEndTime);
//				WithinDayAgentUtils.calculateAndSetDepartureTime(mobsimAgent, act);
				WithinDayAgentUtils.resetCaches( mobsimAgent );
				WithinDayAgentUtils.rescheduleActivityEnd(mobsimAgent,mobsim);
				encounteredActivities.add(act);
			}
		}
	}
}
