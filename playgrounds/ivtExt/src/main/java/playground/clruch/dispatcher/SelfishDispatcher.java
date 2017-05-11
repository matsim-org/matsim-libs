package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class SelfishDispatcher extends UniversalDispatcher {

	private final int dispatchPeriod;

	// specific to SelfishDispatcher
	private final int updateRefPeriod;         // implementation may not use this
	private final int weiszfeldMaxIter;        // max iterations for Weiszfeld's algorithm
	private final double weiszfeldTol;         // convergence tolerance for Weiszfeld's algorithm
	private boolean vehiclesInitialized = false;
	private final HashMap<AVVehicle, List<AVRequest>> requestsServed = new HashMap<>();
	private final HashMap<AVVehicle, Link> refPositions = new HashMap<>();
	private final Network network;
	private final QuadTree<AVRequest> pendingRequestsTree;
	private final QuadTree<Link> networkLinksTree;
	private final HashSet<AVRequest> openRequests = new HashSet<>(); // two data structures are used to enable fast "contains" searching
	private final double[] networkBounds;

	private SelfishDispatcher(                                               //
			AVDispatcherConfig avDispatcherConfig,                           //
			AVGeneratorConfig generatorConfig,                               //
			TravelTime travelTime,                                           //
			ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
			EventsManager eventsManager,                                     //
			Network networkIn, AbstractRequestSelector abstractRequestSelector) {

		super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);

		// Load parameters from av.xml
		SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
		dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);

		updateRefPeriod = safeConfig.getInteger("updateRefPeriod", 3600);
		weiszfeldMaxIter = safeConfig.getInteger("weiszfeldMaxIter", 1000);
		weiszfeldTol = safeConfig.getDouble("weiszfeldTol", 1.0);

        network = networkIn;
        // minx,
        // miny,
        // maxx,
        // maxy
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        pendingRequestsTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
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
						requestsServed.values().stream().mapToInt(List::size).sum() == getTotalMatchedRequests());

				// update ref positions periodically
				if (round_now % updateRefPeriod == 0)
					updateRefPositions();

				// if requests present, send every vehicle to closest customer
                if (getAVRequests().size() > 0) {
                    getDivertableVehicles().stream() //
                            .forEach(v -> setVehicleDiversion(v, findClosestRequest(v, getAVRequests())));
                }

				// send remaining vehicles to their reference position
				// getDivertableVehicles().stream().forEach(v -> setVehicleDiversion(v, ));
				for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
					GlobalAssert.that(refPositions.containsKey(vehicleLinkPair.avVehicle));
					Link link = refPositions.get(vehicleLinkPair.avVehicle);
					GlobalAssert.that(link!=null);
					setVehicleDiversion(vehicleLinkPair, link);
				}
			}
		}
	}

	private QuadTree<Link> buildNetworkQuadTree() {
		QuadTree<Link> networkQuadTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
		Collection<? extends Link> networkLinks = network.getLinks().values();
		for (Link link : networkLinks) {
			GlobalAssert.that(link != null);
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
                boolean qtSucc = pendingRequestsTree.put( //
                        toMatchRequestCoord.getX(), //
                        toMatchRequestCoord.getY(), //
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
		GlobalAssert.that(!getMaintainedVehicles().isEmpty());
		GlobalAssert.that(0 < networkLinksTree.size());
		for (AVVehicle avVehicle : getMaintainedVehicles()) {
			Link initialGuess = refPositions.get(avVehicle);               // initialize with previous Weber link
			Link weberLink = weberWeiszfeld(requestsServed.get(avVehicle), initialGuess);
			GlobalAssert.that(weberLink != null);
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
	private Link weberWeiszfeld(List<AVRequest> avRequests, Link initialGuess) {
		if (avRequests.isEmpty()) {
			return initialGuess;
		}
        double weberX = initialGuess.getFromNode().getCoord().getX();
		double weberY = initialGuess.getFromNode().getCoord().getY();
        int count = 0;
        while (count <= weiszfeldMaxIter) {
        	double X = 0.0;
        	double Y = 0.0;
        	double normalizer = 0.0;
            for (AVRequest avRequest : avRequests) {
            	Coord point = avRequest.getFromLink().getCoord();
				double distance = Math.sqrt( Math.pow(point.getX() - weberX, 2.0) + Math.pow(point.getY() - weberY, 2.0) );
				if (distance != 0) {
					X += point.getX() / distance;
					Y += point.getY() / distance;
					normalizer += 1.0 / distance;
				} else {
					X = point.getX();
					Y = point.getY();
					normalizer = 1.0;
					break;
				}
			}
			X /= normalizer;
			Y /= normalizer;
			double change = Math.sqrt(Math.pow(X - weberX, 2.0) + Math.pow(Y - weberY, 2.0));
			weberX = X;
			weberY = Y;
			if (change < weiszfeldTol) {
				break;
			}
            count++;
        }
        GlobalAssert.that((weberX >= networkBounds[0]) &&
                          (weberY >= networkBounds[1]) &&
                          (weberX <= networkBounds[2]) &&
                          (weberY <= networkBounds[3]) );   // Weber point must be inside the network

        Link weberLink = networkLinksTree.getClosest(weberX, weberY);
        GlobalAssert.that(weberLink != null);
        
        return weberLink;
    }

	/**
	 * initializes the vehicle Lists which are used for vehicle specific
	 * tracking
	 */
	private void initializeVehicles() {
	    Collection<? extends Link> networkLinksCol = network.getLinks().values();     // collection of all links in the network
        List<Link> networkLinks = new ArrayList<>();                                  // create a list from this collection
        for (Link link : networkLinksCol) {
            networkLinks.add(link);
        }
		for (AVVehicle avVehicle : getMaintainedVehicles()) {
			requestsServed.put(avVehicle, new ArrayList<AVRequest>());
			Collections.shuffle(networkLinks);
			refPositions.put(avVehicle, networkLinks.get(0));    // assign a random link from the network to every AV
		}
		vehiclesInitialized = true;
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
