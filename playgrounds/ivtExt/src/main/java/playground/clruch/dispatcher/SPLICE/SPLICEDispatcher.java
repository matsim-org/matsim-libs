package playground.clruch.dispatcher.SPLICE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.NetworkDistanceFunction;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

<<<<<<< HEAD
/** Empty Test Dispatcher, rebalances a vehicle every 30 mins and
 * performs a pickup every 30 mins if open requests are present.
 * Not functional, use as startpoint to build your own dispatcher.
=======
/**
 * Splice Dispatcher based on Pavone and Frazzoli papers doi:
 * 10.1109/TAC.2013.2259993, 10.1109/CDC.2011.6161406
>>>>>>> c73459dd897a310fdd0fa54725d2853d8e94b1ac
 * 
 * Modify AV file and idsc properties:
 * CURRENTLY WORKING WITH 1 VEHICLE ONLY and small population i.e. 20..
 * 
 * @author Nicolo Omezzano
 */
public class SPLICEDispatcher extends RebalancingDispatcher {
<<<<<<< HEAD
    private final int rebalancingPeriod;
    private final int nicoloFactor;
    private final Network network;
    private final Random random = new Random(1334);

    private SPLICEDispatcher(//
            Config config, AVDispatcherConfig avconfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network) {
        super(config, avconfig, travelTime, router, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avconfig);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 120);
        nicoloFactor = safeConfig.getInteger("theNicoloFactor", -1);
        this.network = network;
    }

    @Override
    public void redispatch(double now) {

        System.out.println("nicoloFactor =" + nicoloFactor);

        // TestBedDispatcher implementation
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0 && 0 < getAVRequests().size()) {
            
            // TASK 1: Compute a stacker crane tour from requests. 
            
            StackerCraneTour sct = new StackerCraneTour();
            
            
            
            // TASK 2: have only one vehicle and continuously let it run on the stacker crane tour 
            // updated every 60 mins. 
            
            // Input: GlobalAssert.that(getRoboTaxis().size() == 1);
            
            
            
            
            
            
            // 1 compute the Stacker Crane Tour
            
            // Euclidean Bipartite matching
            // --> Look at the GlobalBipartiteMatchingDispatcher
            
            // connect
            
            // REWIRE to get away the subtours
            
            
            // 2 Break up tour for number of cars
            
            // 3 assing each car a request on his tour chunk
            
            // 4 do the actual pickup
            
            
            
            
            
            

            // chose a link dpending on theNicoloFactor and then send all taxis to that link
            // every rebalcingPeriod
            Iterator<Link> iterator = (Iterator<Link>) network.getLinks().values().iterator();
            int i = 0;
            Link rebalanceTo = null;
            while (iterator.hasNext() && i < random.nextInt(100)) {
                rebalanceTo = iterator.next();
                ++i;
            }
            GlobalAssert.that(rebalanceTo != null);
            
            for(RoboTaxi robotaxi : getDivertableRoboTaxis()){
                setRoboTaxiRebalance(robotaxi, rebalanceTo);
                
            }
            

            // // rebalance a RoboTaxi
            // RoboTaxi robotaxi = getDivertableRoboTaxis().iterator().next();

            // if (round_now % 1800 == 0) {
            // setRoboTaxiRebalance(robotaxi, rebalanceTo);
            // }
            //
            // // generate a pickup
            // Collection<AVRequest> avRequests = getAVRequests();
            // RoboTaxi robotaxiPickup = getDivertableRoboTaxis().iterator().next();
            // if (!avRequests.isEmpty() && round_now % 1800 == 0) {
            // setRoboTaxiPickup(robotaxiPickup, getAVRequests().iterator().next());
            // }
        }
    }

    @Override
    protected String getInfoLine() {
        return super.getInfoLine();
        // return String.format("%s AT=%5d", //
        // super.getInfoLine());
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
        public AVDispatcher createDispatcher(Config config, AVDispatcherConfig avconfig, AVGeneratorConfig generatorConfig) {
            return new SPLICEDispatcher(config, avconfig, travelTime, router, eventsManager, network);
        }
    }
=======
	private final int dispatchingPeriod;
	private final Network network;
	private final NetworkDistanceFunction ndf;
	private List<AVRequest> scTAVrequests = new ArrayList<AVRequest>();
	private Iterator<AVRequest> scTAVreqIterator = new ArrayList<AVRequest>().iterator();

	// constructor
	private SPLICEDispatcher(//
			Config config, AVDispatcherConfig avconfig, //
			TravelTime travelTime, //
			ParallelLeastCostPathCalculator router, //
			EventsManager eventsManager, //
			Network network) {
		// NetworkDistanceFunction ndf) {
		// List<AVRequest> scTAVrequests) {
		super(config, avconfig, travelTime, router, eventsManager);
		SafeConfig safeConfig = SafeConfig.wrap(avconfig);
		dispatchingPeriod = safeConfig.getInteger("dispatchingPeriod", 480);
		this.network = network;
		this.ndf = new NetworkDistanceFunction(network);
		// this.scTAVrequests = scTAVrequests;
	}

	@Override
	public void redispatch(double now) {

		// TestBedDispatcher implementation
		final long round_now = Math.round(now);
		
		// If there are more than x requests
		if (scTAVrequests.isEmpty() && 12 < getAVRequests().size()) {

			System.out.println("Current time is " + round_now + ". Found " + getAVRequests().size() + " requests!!!");
			// List<AVRequest> scTAVrequests = new ArrayList<AVRequest>();
			StackerCraneTour sct = new StackerCraneTour(scTAVrequests, ndf);

			// Get stacker crane tour of requests
			scTAVrequests = sct.calculate(getAVRequests());
			
			//Currently Working with 1 vehicle only
			GlobalAssert.that(getRoboTaxis().size() == 1);
			// Set iterator from AVlist
			scTAVreqIterator = scTAVrequests.iterator();

		}

		// If i have a current subtour open and I have available taxis
		if (!scTAVrequests.isEmpty() && !getRoboTaxiSubset(AVStatus.STAY).isEmpty()) {
			// Get taxis
			RoboTaxi robotaxiPickup = getDivertableRoboTaxis().iterator().next();
			// Set taxi to do pickup
			setRoboTaxiPickup(robotaxiPickup, scTAVreqIterator.next());

			// Get current pickup register
			Map<RoboTaxi, AVRequest> pickupPairs = new HashMap<RoboTaxi, AVRequest>();
			pickupPairs = getPickupRoboTaxis();
			System.out.println(round_now + " :Current pickup register: " + pickupPairs);

			// If iterated through every request clear subtour. 
			if (!scTAVreqIterator.hasNext()) {
				scTAVrequests.clear();

			}

			// TASK 1: Compute a stacker crane tour from requests.
			// TASK 2: have only one vehicle and continuously let it run on the stacker
			// crane tour
			// 1 compute the Stacker Crane Tour

			// Euclidean Bipartite matching
			// --> Look at the GlobalBipartiteMatchingDispatcher
			// connect
			// REWIRE to get away the subtours
			
			
			// TO DO --  multiple car case
			// 2 Break up tour for number of cars
			// 3 assing each car a request on his tour chunk
			// 4 do the actual pickup

			// // rebalance a RoboTaxi
			// RoboTaxi robotaxi = getDivertableRoboTaxis().iterator().next();

			// if (round_now % 1800 == 0) {
			// setRoboTaxiRebalance(robotaxi, rebalanceTo);
			// }

		}
	}

	@Override
	protected String getInfoLine() {
		return super.getInfoLine();
		// return String.format("%s AT=%5d", //
		// super.getInfoLine());
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

		// @Inject
		// private NetworkDistanceFunction ndf;
		////
		// @Inject
		// private List<AVRequest> scTAVrequests;

		@Override
		public AVDispatcher createDispatcher(Config config, AVDispatcherConfig avconfig,
				AVGeneratorConfig generatorConfig) {
			return new SPLICEDispatcher(config, avconfig, travelTime, router, eventsManager, network);// ,
																										// scTAVrequests);
																										// //,ndf,scTAVrequests);
		}
	}
>>>>>>> c73459dd897a310fdd0fa54725d2853d8e94b1ac

}
