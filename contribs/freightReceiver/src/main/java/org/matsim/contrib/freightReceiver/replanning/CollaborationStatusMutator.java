package org.matsim.contrib.freightReceiver.replanning;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.contrib.freightReceiver.ReceiverPlan;
import org.matsim.contrib.freightReceiver.collaboration.CollaborationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

/**
 * This is a class that changes a receiver's collaboration status during replanning.
 * @author wlbean
 *
 */

public final class CollaborationStatusMutator implements GenericPlanStrategyModule<ReceiverPlan> {
	private static final Logger log = LogManager.getLogger( CollaborationStatusMutator.class ) ;

	/*
	 * A class that changes a receiver's collaboration status.
	 */

public CollaborationStatusMutator(){

	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {

	}

	@Override
	public void handlePlan(ReceiverPlan receiverPlan) {
		log.warn("entering handlePlan" ) ;


		boolean newstatus;
		boolean grandMember = (boolean) receiverPlan.getReceiver().getAttributes().getAttribute( CollaborationUtils.ATTR_GRANDCOALITION_MEMBER );
		boolean status = (boolean) receiverPlan.getReceiver().getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS );


		if (grandMember == true){
			if (status == true){
				newstatus = false;
			} else {
				newstatus = true;
			}
		} else newstatus = status;

		receiverPlan.getReceiver().getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, newstatus);
		receiverPlan.getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, newstatus);

	}

	@Override
	public void finishReplanning() {

	}

}
