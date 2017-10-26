package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** Empty Test Dispatcher, rebalances a vehicle every 30 mins and
 * performs a pickup every 30 mins if open reqeutss are present.
 * Not functional, use as startpoint to build your own dispatcher.
 * 
 * @author Claudio Ruch */
public class SPLICEDispatcher extends RebalancingDispatcher {
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

        // TestBedDispatcher implemenatation
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0 && 0 < getAVRequests().size()) {

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

}
