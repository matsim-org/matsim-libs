/**
 * 
 */
package org.matsim.contrib.av.intermodal;

import java.util.Collection;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nagel
 *
 */
final class MyOtherTaxiOptimizerFactory implements TaxiOptimizerFactory {

	@Override
	public TaxiOptimizer createTaxiOptimizer(final TaxiOptimizerContext optimContext, final ConfigGroup optimizerConfigGroup) {
		
		// what do we have in optimContext?
		// these are matsim core:
		Network network = optimContext.network ;
		MobsimTimer time = optimContext.timer ;
		TravelDisutility travelDisutility = optimContext.travelDisutility ;
		TravelTime travelTime = optimContext.travelTime ;
		// these not:
		TaxiScheduler scheduler = optimContext.scheduler ;
		TaxiData taxiData = optimContext.taxiData ;
		
		AbstractTaxiOptimizerParams params = null ; // yy need to find out how to get them but should be ok.
		Collection<TaxiRequest> unplannedRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR); // constructed like this in other examples. kai, jan'17
		boolean doUnscheduleAwaitingRequests = false ;
		TaxiOptimizer optimizer = new AbstractTaxiOptimizer(optimContext, params, unplannedRequests, doUnscheduleAwaitingRequests ) {
			@Override protected void scheduleUnplannedRequests() {
			}
		} ;
		return optimizer ;
	}

}
