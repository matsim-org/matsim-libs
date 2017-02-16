/**
 * 
 */
package org.matsim.contrib.av.intermodal;

import java.util.*;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import com.google.inject.Provider;

/**
 * @author nagel
 *
 */
final class MyOtherTaxiOptimizerProvider implements Provider<TaxiOptimizer> {

	private TaxiOptimizerContext optimContext;
	
	@Override
	public TaxiOptimizer get() {
		// what do we have in optimContext?
		// these are matsim core:
		Network network = optimContext.network ;
		MobsimTimer time = optimContext.timer ;
		TravelDisutility travelDisutility = optimContext.travelDisutility ;
		TravelTime travelTime = optimContext.travelTime ;
		// these not:
		TaxiScheduler scheduler = optimContext.scheduler ;
		Fleet fleet = optimContext.fleet ;
		
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
