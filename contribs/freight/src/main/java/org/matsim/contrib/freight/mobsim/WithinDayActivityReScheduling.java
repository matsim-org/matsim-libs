package org.matsim.contrib.freight.mobsim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Tour.Start;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.mobsim.CarrierAgent.CarrierDriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.ActivityImpl;

class WithinDayActivityReScheduling implements MobsimListener, MobsimBeforeSimStepListener{

	private static Logger logger = Logger.getLogger(WithinDayActivityReScheduling.class);
	
	private FreightAgentSource freightAgentSource;
	
	private Set<Activity> encounteredActivities = new HashSet<Activity>();

	private InternalInterface internalInterface;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	WithinDayActivityReScheduling(FreightAgentSource freightAgentSource, InternalInterface internalInterface, CarrierAgentTracker carrierAgentTracker) {
		super();
		this.freightAgentSource = freightAgentSource;
		this.internalInterface = internalInterface;
		this.carrierAgentTracker = carrierAgentTracker;
	}

	/**
	 *This is adopted by ??? i think cdobler and might not be the preferred way of withinDayReplanning. We'll see.
	 */
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		Netsim mobsim = (Netsim) e.getQueueSimulation();
		mobsim.getScenario();
		
		Collection<MobsimAgent> agentsToReplan = freightAgentSource.getMobSimAgents();

		for (MobsimAgent pa : agentsToReplan) {
			doReplanning(pa, mobsim, e.getSimulationTime());
		}
		
	}

	private boolean doReplanning(MobsimAgent pa, Netsim mobsim, double time) {
		if (!(pa instanceof ExperimentalBasicWithindayAgent)) {
			logger.error("agent " + pa.getId() + "of wrong type; returning ... ");
			return false;
		}
		ExperimentalBasicWithindayAgent withindayAgent = (ExperimentalBasicWithindayAgent) pa;
		Plan plan = withindayAgent.getSelectedPlan();
		if (plan == null) {
			logger.info(" we don't have a selected plan; returning ... ");
			return false;
		}	
		
		if (withindayAgent.getCurrentPlanElement() instanceof Activity) {
			ActivityImpl act = (ActivityImpl) withindayAgent.getCurrentPlanElement();
			if(encounteredActivities.contains(act)){
				return false;
			}
			CarrierDriverAgent driver = carrierAgentTracker.getDriver(withindayAgent.getId());
			TourActivity plannedActivity = (TourActivity) driver.getPlannedTourElement(withindayAgent.getCurrentPlanElementIndex());
			if(plannedActivity instanceof Start){
				encounteredActivities.add(act);
				return false;
			}
			else {
				double newEndTime = Math.max(time, plannedActivity.getTimeWindow().getStart()) + plannedActivity.getDuration();
				logger.info("[agentId="+ withindayAgent.getId() + "][currentTime="+time+"][actDuration="+plannedActivity.getDuration()+
						"[plannedActEnd="+ act.getEndTime() + "][newActEnd="+newEndTime+"]");
				act.setEndTime(newEndTime);
				withindayAgent.calculateAndSetDepartureTime(act);
				internalInterface.rescheduleActivityEnd(withindayAgent);
				encounteredActivities.add(act);
			}
		} 	
		return true;
	}

	

}
