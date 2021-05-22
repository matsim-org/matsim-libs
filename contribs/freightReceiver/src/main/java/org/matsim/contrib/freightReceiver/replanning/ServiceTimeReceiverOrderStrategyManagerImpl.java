package org.matsim.contrib.freightReceiver.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freightReceiver.Receiver;
import org.matsim.contrib.freightReceiver.ReceiverPlan;
import org.matsim.contrib.freightReceiver.collaboration.ServiceTimeMutator;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;

/**
 * This class implements a receiver reorder strategy that changes the delivery unloading time of its orders.
 * 
 * @author wlbean
 *
 */
public final class ServiceTimeReceiverOrderStrategyManagerImpl implements ReceiverOrderStrategyManagerFactory{
	private static final Logger log = Logger.getLogger(ServiceTimeReceiverOrderStrategyManagerImpl.class) ;

	@Inject Scenario sc;
	
	ServiceTimeReceiverOrderStrategyManagerImpl(){
	}

	@Override
	public GenericStrategyManager<ReceiverPlan, Receiver> createReceiverStrategyManager() {
		final GenericStrategyManager<ReceiverPlan, Receiver> stratMan = new GenericStrategyManager<>();
		stratMan.setMaxPlansPerAgent(5);
		
		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 10.) );
			stratMan.addStrategy(strategy, null, 0.5);
//			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), strategy, null, 0.0);

		}
		
		/*
		 * Increase service duration with specified duration (mutationTime) until specified maximum service time (mutationRange) is reached. 
		 */
		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> increaseStrategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> increaseStrategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanSelector<>( 10. ) );
			increaseStrategy.addStrategyModule(new ServiceTimeMutator(Time.parseTime("01:00:00"), Time.parseTime("04:00:00"), true));
			// (ends up with service time that is <= 4hrs)
			stratMan.addStrategy(increaseStrategy, null, 0.15);
			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), increaseStrategy, null, 0.0);
		}
		
		/* 
		 * Decreases service duration with specified duration (mutationTime) until specified minimum service time (mutationRange) is reached. 
		 */
		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> decreaseStrategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> decreaseStrategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanSelector<>( 10. ) );
			decreaseStrategy.addStrategyModule(new ServiceTimeMutator(Time.parseTime("01:00:00"), Time.parseTime("01:00:00"), false));
			// (ends up with service time that is >= 1hrs)
			stratMan.addStrategy(decreaseStrategy, null, 0.15);
			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), decreaseStrategy, null, 0.0);
		}
		
		/* Replanning for grand coalition receivers.*/
//		{
////			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanSelector<>( 10. ) );
//			strategy.addStrategyModule(new CollaborationStatusMutator());
//			stratMan.addStrategy(strategy, null, 0.2);
//			stratMan.addChangeRequest((int) Math.round((sc.getConfig().controler().getLastIteration())*0.9), strategy, null, 0.0);
//		}
//		log.error("yyyyyy the above needs to be restored again.") ;
		// yyyyyy I have switched off the coalition mutator!
		
		return stratMan;
	}

}
