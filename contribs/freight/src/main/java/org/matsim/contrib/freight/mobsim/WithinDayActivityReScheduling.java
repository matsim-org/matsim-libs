package org.matsim.contrib.freight.mobsim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;

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

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		Netsim mobsim = (Netsim) e.getQueueSimulation();
		mobsim.getScenario();
		
		Collection<MobsimAgent> agentsToReplan = freightAgentSource.getMobSimAgents();

		for (MobsimAgent pa : agentsToReplan) {
			doReplanning(pa, mobsim);
		}
		
	}

	private boolean doReplanning(MobsimAgent pa, Netsim mobsim) {
//		logger.info("now I probably can determine what agent " + pa.getId() + " can do next");
		
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
			if(act.getType().equals(FreightConstants.PICKUP)){
//				logger.info(pa.getId() + " conducts pickup " + withindayAgent.getCurrentPlanElement().toString());
//				logger.info("currentTime=" + Time.writeTime(mobsim.getSimTimer().getTimeOfDay()));
				if(withindayAgent.getId().toString().equals("freight_carrier1_veh_vehicle_c1")){
					if(encounteredActivities.contains(act)){
						return false;
					}
					CarrierShipment shipment = carrierAgentTracker.getAssociatedShipment(withindayAgent.getId(), act, withindayAgent.getCurrentPlanElementIndex());
					
					if(shipment != null){
						logger.info("HELL. Gefunden!");
						logger.info(shipment + " deliveryTW=" + shipment.getDeliveryTimeWindow() + " serviceTime=" + shipment.getDeliveryServiceTime());
					}
					logger.info("try to expand activity duration to " + Time.writeTime(8*3600));
					act.setEndTime(8*3600);
					withindayAgent.calculateAndSetDepartureTime(act);
					internalInterface.rescheduleActivityEnd(withindayAgent);
					encounteredActivities.add(act);
				}
			}
		} 	
		return true;
		
		
	}

}
