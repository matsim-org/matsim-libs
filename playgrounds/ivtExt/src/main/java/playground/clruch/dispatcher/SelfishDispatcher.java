package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVVehicleAssignmentEvent;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SelfishDispatcher extends UniversalDispatcher {

	private final int dispatchPeriod;
	private final int updateRefPeriod; // implementation may not use this
	private Tensor printVals = Tensors.empty();

	// specific to SelfishDispatcher
	HashMap<AVVehicle, List<AVRequest>> requestsServed = new HashMap<>();
	HashMap<AVVehicle, Link> refPositions = new HashMap<>();

	private SelfishDispatcher( //
			AVDispatcherConfig avDispatcherConfig, //
			TravelTime travelTime, //
			ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
			EventsManager eventsManager, //
			Network network, AbstractRequestSelector abstractRequestSelector) {
		super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
		SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
		dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
		updateRefPeriod = safeConfig.getInteger("updateRefPeriod", Integer.MAX_VALUE);

		/*
		for (AVVehicle avVehicle : getAVList()) {
			requestsServed.put(avVehicle, new ArrayList<AVRequest>());
			refPositions.put(avVehicle, null);
		}
		System.out.println("number of AVs " + requestsServed.keySet().size() + " == " + refPositions.keySet().size());
		*/
	}

	@Override
	public void redispatch(double now) {
		final long round_now = Math.round(now);

		// match vehicles on same link as request
		new InOrderOfArrivalMatcher(this::setAcceptRequest) //
				.matchRecord(getStayVehicles(), getAVRequestsAtLinks(), requestsServed);
		// ensure all requests recorded properly
		/*
		GlobalAssert
				.that(requestsServed.values().stream().mapToInt(List::size).sum() == super.getTotalMatchedRequests());
		if (getTotalMatchedRequests() > 0) {
			System.out.println("matched requests total: " + super.getTotalMatchedRequests());
			System.out.println(
					"matched requests summed : " + requestsServed.values().stream().mapToInt(List::size).sum());

		}
		*/

		if (round_now % dispatchPeriod == 0) {
			if (round_now == 11890) {
				System.out.println("arrived at problem");
			}
		}
	}

	public static class Factory implements AVDispatcherFactory {
		@Inject
		@Named(AVModule.AV_MODE)
		private ParallelLeastCostPathCalculator router;

		@Inject
		@Named(AVModule.AV_MODE)
		private TravelTime travelTime;

		@Inject
		private EventsManager eventsManager;

		@Inject
		private Network network;

		@Override
		public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
			AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
			return new SelfishDispatcher( //
					config, travelTime, router, eventsManager, network, abstractRequestSelector);
		}
	}
}
