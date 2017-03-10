package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class DivertIfCurrentDestinationEmpty extends AbstractVehicleRequestLinkMatcher {

    Random random = new Random();

    @Override
    public final Map<VehicleLinkPair, Link> match( //
            Map<Link, List<AVRequest>> requests, Collection<VehicleLinkPair> vehicles) {

        final List<AVRequest> list = requests.values().stream() //
                .flatMap(List::stream).collect(Collectors.toList());

        final Map<VehicleLinkPair, Link> map = new HashMap<>();
        for (VehicleLinkPair vehicleLinkPair : vehicles) {
            final Link link = vehicleLinkPair.getCurrentDriveDestination();
            if (link == null || !requests.containsKey(link) || requests.get(link).isEmpty()) {
                if (list.isEmpty())
                    break;
                Link dest = list.remove(random.nextInt(list.size())).getFromLink();
                map.put(vehicleLinkPair, dest);
            }
        }
        return map;
    }

}
