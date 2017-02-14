package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.utils.ScheduleUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class PulseDispatcher extends UniversalDispatcher {
    public static final String IDENTIFIER = PulseDispatcher.class.getSimpleName();
    public static final int DEBUG_PERIOD = 60;
    public static final String DEBUG_AVVEHICLE = "av_av_op1_1";

    private final Network network;
    private final List<Link> links;
    int index = 0;

    Random random = new Random();

    public PulseDispatcher(//
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network) {
        super(config, travelTime, router, eventsManager);
        this.network = network;
        links = new ArrayList<>(this.network.getLinks().values());
        Collections.shuffle(links);
    }

    @Override
    public void redispatch(double now) {
        if (3600 < now && Math.round(now) % DEBUG_PERIOD == 0 && now < 90000) {
            System.out.println("==================== TIME " + now);
            System.out.println(getUniversalDispatcherStatusString());

            // BEGIN: debug info
            for (AVVehicle avVehicle : getFunctioningVehicles())
                if (avVehicle.getId().toString().equals(DEBUG_AVVEHICLE))
                    System.out.println(ScheduleUtils.scheduleOf(avVehicle));
            // END: debug info

            for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                if (random.nextInt(3) == 0) {
                    // System.out.println("- diversion -");
                    setVehicleDiversion(vehicleLinkPair, links.get(index));
                    ++index;
                    index %= links.size();
                }
            }
            
            System.out.println(getVehicleMaintainerStatusString());
        }
    }

    public void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // System.out.println("moved " + avVehicle.getId() + " -> " + linkTimePair.link.getId());
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
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            return new PulseDispatcher(config, travelTime, router, eventsManager, network);
        }
    }

}
