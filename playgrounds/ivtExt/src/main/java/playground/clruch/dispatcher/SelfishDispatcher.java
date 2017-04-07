package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

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

import static java.lang.Math.sqrt;

public class SelfishDispatcher extends UniversalDispatcher {

	private final int dispatchPeriod;
	private Tensor printVals = Tensors.empty();
	private final int numberofVehicles;

	// specific to SelfishDispatcher
	private final int updateRefPeriod; 			// implementation may not use this
	private final int weiszfeldMaxIter; 		// max iterations for Weiszfeld's algorithm
	private final double weiszfeldTol; 			// convergence tolerance for Weiszfeld's algorithm
	private boolean vehiclesInitialized = false;
	private HashMap<AVVehicle, List<AVRequest>> requestsServed = new HashMap<>();
	private HashMap<AVVehicle, Link> refPositions = new HashMap<>();
	private final Network network;
	private final QuadTree<AVRequest> pendingRequestsTree;
	private final QuadTree<Link> networkLinksTree;
	private final HashSet<AVRequest> openRequests = new HashSet<>(); // two data structures are used to enable fast "contains" searching

	private SelfishDispatcher( //
			AVDispatcherConfig avDispatcherConfig, //
			AVGeneratorConfig generatorConfig, //
			TravelTime travelTime, //
			ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
			EventsManager eventsManager, //
			Network networkIn, AbstractRequestSelector abstractRequestSelector) {
		super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
		SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
		dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
		updateRefPeriod = safeConfig.getInteger("updateRefPeriod", Integer.MAX_VALUE);
		numberofVehicles = (int) generatorConfig.getNumberOfVehicles();
		weiszfeldMaxIter = safeConfig.getInteger("weiszfeldMaxIter", 1000);
		weiszfeldTol = safeConfig.getDouble("weiszfeldTol", 1.0);
		network = networkIn;
		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values()); // minx,
																					// miny,
																					// maxx,
																					// maxy
		pendingRequestsTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		networkLinksTree = buildNetworkQuadTree();
	}

	@Override
	public void redispatch(double now) {
		final long round_now = Math.round(now);
		if (!vehiclesInitialized) {
			initializeVehicles();
		} else {
			if (round_now % dispatchPeriod == 0) {
				// add new open requests to list
				addOpenRequests(getAVRequests());
				GlobalAssert.that(openRequests.size() == pendingRequestsTree.size());

				// match vehicles on same link as request
				new InOrderOfArrivalMatcher(this::setAcceptRequest) //
						.matchRecord(getStayVehicles(), getAVRequestsAtLinks(), requestsServed, openRequests,
								pendingRequestsTree);

				// ensure all requests recorded properly
				GlobalAssert.that(
						requestsServed.values().stream().mapToInt(List::size).sum() == super.getTotalMatchedRequests());

				// update ref positions periodically
				if (round_now % updateRefPeriod == 0)
					updateRefPositions();

				// if requests present, send every vehicle to closest customer
				if(getAVRequests().size()>0){
					getDivertableVehicles().stream()
					.forEach(v -> setVehicleDiversion(v, findClosestRequest(v, getAVRequests())));
				}


				// send remaining vehicles to their reference position
				getDivertableVehicles().stream().forEach(v -> setVehicleDiversion(v, refPositions.get(v.avVehicle)));

			}
		}
	}

	private QuadTree<Link> buildNetworkQuadTree() {
		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values()); // minx, miny, maxx, maxy
		QuadTree<Link> networkQuadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		Collection<? extends Link> networkLinks = network.getLinks().values();
		for (Link link : networkLinks) {
			Coord linkCoord = link.getFromNode().getCoord();
			boolean putSuccess = networkQuadTree.put(linkCoord.getX(), linkCoord.getY(), link);
			GlobalAssert.that(putSuccess);
		}
		return networkQuadTree;
	}

	/** 
	 * @param avRequests ensures that new open requests are added to a list with all open requests
	 */
	private void addOpenRequests(Collection<AVRequest> avRequests) {
		for (AVRequest avRequest : avRequests) {
			if (!openRequests.contains(avRequest)) {
				Coord toMatchRequestCoord = avRequest.getFromLink().getFromNode().getCoord();
				boolean orSucc = openRequests.add(avRequest);
				boolean qtSucc = pendingRequestsTree.put(toMatchRequestCoord.getX(), toMatchRequestCoord.getY(),
						avRequest);
				GlobalAssert.that(orSucc == qtSucc && orSucc == true);
			}
		}

	}

	/**
	 * @param vehicleLinkPair
	 *            some vehicle link pair
	 * @param avRequests
	 *            list of currently open AVRequests
	 * @return the Link with fromNode closest to vehicleLinkPair
	 *         divertableLocation fromNode
	 */
	private Link findClosestRequest(VehicleLinkPair vehicleLinkPair, Collection<AVRequest> avRequests) {
		Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
		return pendingRequestsTree.getClosest(vehicleCoord.getX(), vehicleCoord.getY()).getFromLink();
	}

	/**
	 * update the reference positions of all AVs
	 */
	private void updateRefPositions() {
		for (AVVehicle avVehicle : refPositions.keySet()) {
			// Coord initialGuess = refPositions.get(avVehicle).getFromNode().getCoord(); // initialize with previous Weber link
			Coord initialGuess = new Coord(0.0,0.0);
			Coord weberCoord = weberWeiszfeld(requestsServed.get(avVehicle), initialGuess);
			Link weberLink = networkLinksTree.getClosest(weberCoord.getX(), weberCoord.getY());
			refPositions.put(avVehicle, weberLink);
		}
	}

    /**
     *
     * @param avRequests
     *          list of requests served so far
     * @param initialGuess
     *          2D-point initial guess for the iterative algorithm
     * @return 2D Weber point of past requests approximated by Weiszfeld's algorithm
     */
	private Coord weberWeiszfeld(List<AVRequest> avRequests, Coord initialGuess) {
        double weberX = initialGuess.getX();
		double weberY = initialGuess.getY();
        int count = 0;
        while (count <= weiszfeldMaxIter) {
        	double X = 0.0;
        	double Y = 0.0;
        	double normalizer = 0.0;
            for (AVRequest avRequest : avRequests) {
            	Coord point = avRequest.getFromLink().getCoord();
				double distance = Math.sqrt( Math.pow(point.getX() - weberX, 2.0) + Math.pow(point.getY() - weberY, 2.0) );
				X += point.getX()/distance;
				Y += point.getY()/distance;
				normalizer += 1.0/distance;
			}
			X /= normalizer;
            Y /= normalizer;
            double change = Math.sqrt( Math.pow(X - weberX, 2.0) + Math.pow(Y - weberY, 2.0) );
            weberX = X;
            weberY = Y;
			if (change < weiszfeldTol) {
				break;
			}
            count++;
        }
        return new Coord(weberX, weberY);
    }

	/**
	 * initializes the vehicle Lists which are used for vehicle specific
	 * tracking
	 */
	private void initializeVehicles() {
		for (AVVehicle avVehicle : getAVList()) {
			requestsServed.put(avVehicle, new ArrayList<AVRequest>());
			refPositions.put(avVehicle, null);
		}
		if (requestsServed.keySet().size() == numberofVehicles) {
			updateRefPositions();
			vehiclesInitialized = true;
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
					config, generatorConfig, travelTime, router, eventsManager, network, abstractRequestSelector);
		}
	}
}
