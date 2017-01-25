/**
 * 
 */
package org.matsim.contrib.av.intermodal;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author nagel
 *
 */
final class MyTaxiOptimizerFactory implements TaxiOptimizerFactory {

	@Override
	public TaxiOptimizer createTaxiOptimizer(final TaxiOptimizerContext optimContext, final ConfigGroup optimizerConfigGroup) {
		TaxiOptimizer optimizer = new TaxiOptimizer(){
			@Override public void nextLinkEntered(DriveTask driveTask) {
				// I guess this is how taxis notify of their progress to the dispatch?  kai, jan'17
				
				// TODO Auto-generated method stub
			}
			@Override public void requestSubmitted(Request request) {
				// I guess this is how requests are inserted into the optimizer?  kai, jan'17
				
				// TODO Auto-generated method stub
			}
			@Override public void nextTask(Schedule<? extends Task> schedule) {
				// what is this?  Presumably, a "schedule", which is attached to a single "taxi", here asks to be extended?? kai, jan'17
				
				// TODO Auto-generated method stub
			}
			@Override public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
				// TODO Auto-generated method stub
			}
		} ;
		return optimizer ;
	}

}
