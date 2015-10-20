package playground.wrashid.parkingSearch.withindayFW.controllers.test;

import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

public class TripRouterFactoryAdapter implements Provider<TripRouter> {

	public TripRouterFactoryAdapter(Provider<TripRouter> multimodalTripRouterFactory) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public TripRouter get() {
		// TODO Auto-generated method stub
		return null;
	}

}
