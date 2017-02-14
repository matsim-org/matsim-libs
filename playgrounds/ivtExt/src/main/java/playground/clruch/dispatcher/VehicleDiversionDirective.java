package playground.clruch.dispatcher;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.router.FuturePathContainer;

abstract class VehicleDiversionDirective extends AbstractDirective {
    final VehicleLinkPair vehicleLinkPair;
    final Link destination;

    VehicleDiversionDirective(final VehicleLinkPair vehicleLinkPair, final Link destination, FuturePathContainer futurePathContainer) {
        super(futurePathContainer);
        this.vehicleLinkPair = vehicleLinkPair;
        this.destination = destination;
    }
}
