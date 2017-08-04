// code by jph
package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.router.FuturePathContainer;

abstract class VehicleDiversionDirective extends FuturePathDirective {
    final RoboTaxi vehicleLinkPair;
    final Link destination;

    VehicleDiversionDirective(final RoboTaxi vehicleLinkPair, final Link destination, FuturePathContainer futurePathContainer) {
        super(futurePathContainer);
        this.vehicleLinkPair = vehicleLinkPair;
        this.destination = destination;
    }
}
