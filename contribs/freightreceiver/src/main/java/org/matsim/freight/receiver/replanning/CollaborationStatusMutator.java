package org.matsim.freight.receiver.replanning;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

/**
 * This is a class that changes a receiver's collaboration status during replanning.
 *
 * @author wlbean, jwjoubert
 */

final class CollaborationStatusMutator implements GenericPlanStrategyModule<ReceiverPlan> {
	private static final Logger log = LogManager.getLogger(CollaborationStatusMutator.class);

	CollaborationStatusMutator() {
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void handlePlan(ReceiverPlan receiverPlan) {
		log.warn("entering handlePlan");

		boolean newStatus;
		boolean grandMember = (boolean) receiverPlan.getReceiver().getAttributes().getAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER);
		boolean collaborationStatus = (boolean) receiverPlan.getReceiver().getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS);

		if (grandMember) {
			newStatus = !collaborationStatus;
		} else newStatus = collaborationStatus;

		receiverPlan.getReceiver().getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, newStatus);
		receiverPlan.getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, newStatus);
	}

	@Override
	public void finishReplanning() {
	}

}
