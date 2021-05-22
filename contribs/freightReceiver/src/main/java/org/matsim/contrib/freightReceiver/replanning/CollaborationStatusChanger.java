package org.matsim.contrib.freightReceiver.replanning;

import org.matsim.contrib.freightReceiver.ReceiverPlan;
import org.matsim.contrib.freightReceiver.ReceiverUtils;
import org.matsim.contrib.freightReceiver.collaboration.CollaborationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

/**
 * This is likely not useful anymore since it only updates the receiver's 
 * collaboration status, from the receiver's order status. 
 * 
 * FIXME The receiver should not have a collaboration status. Only its order/plan.
 * 
 * @author jwjoubert
 */
@Deprecated
public class CollaborationStatusChanger implements GenericPlanStrategyModule<ReceiverPlan> {

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlePlan(ReceiverPlan plan) {
		boolean status = (boolean) plan.getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS );
		plan.getReceiver().getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, status);
	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub
		
	}

}
