package org.matsim.contrib.freight.mobsim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.CarrierShipment.TimeWindow;
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
	
	private MobsimAgent.State lastState = null;
	
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

//		if(withindayAgent.getId().toString().equals("freight_carrier1_veh_vehicle_c1")){
//			if(lastState == null){
//				lastState = withindayAgent.getState();
//				logger.info(withindayAgent.getId() + " has state " + withindayAgent.getState()  + " time " + time);
//			}
//			if(!(lastState.toString().equals(withindayAgent.getState().toString()))){
//				lastState = withindayAgent.getState();
//				logger.info(withindayAgent.getId() + " has state " + withindayAgent.getState() + " time " + time);
//			}
//		}
//				
//		
		
		if (withindayAgent.getCurrentPlanElement() instanceof Activity) {
			ActivityImpl act = (ActivityImpl) withindayAgent.getCurrentPlanElement();
			if(encounteredActivities.contains(act)){
				return false;
			}
			if(act.getType().equals(FreightConstants.PICKUP)){	
				CarrierShipment shipment = carrierAgentTracker.getAssociatedShipment(withindayAgent.getId(), act, withindayAgent.getCurrentPlanElementIndex());
				assert shipment != null : "shipment must not be null";
				double endTime = determineActEnd(time,shipment.getPickupServiceTime(),shipment.getPickupTimeWindow());
				if(act.getEndTime() != endTime) logger.info(withindayAgent.getId() + " changed pickupEndTime (" + act.getEndTime() +","+endTime+")");
				act.setEndTime(endTime);
				withindayAgent.calculateAndSetDepartureTime(act);
				internalInterface.rescheduleActivityEnd(withindayAgent);
			}
			else if(act.getType().equals(FreightConstants.DELIVERY)){
				CarrierShipment shipment = carrierAgentTracker.getAssociatedShipment(withindayAgent.getId(), act, withindayAgent.getCurrentPlanElementIndex());
				assert shipment != null : "shipment must not be null";
				double endTime = determineActEnd(time,shipment.getDeliveryServiceTime(),shipment.getDeliveryTimeWindow());
				if(act.getEndTime() != endTime) logger.info(withindayAgent.getId() + " changed deliveryEndTime (" + act.getEndTime() +","+endTime+")");
				act.setEndTime(endTime);
				withindayAgent.calculateAndSetDepartureTime(act);
				internalInterface.rescheduleActivityEnd(withindayAgent);
				encounteredActivities.add(act);
			}
			encounteredActivities.add(act);

		} 	
		return true;
	}

	private double determineActEnd(double time, double actServiceTime, TimeWindow actTimeWindow) {
		double operationStartTime = Math.max(time, actTimeWindow.getStart());
		return operationStartTime + actServiceTime;
	}

	

}
