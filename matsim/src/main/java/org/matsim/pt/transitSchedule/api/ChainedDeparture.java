package org.matsim.pt.transitSchedule.api;

import org.matsim.api.core.v01.Id;

/**
 * Describes that a {@link Departure} is connected to another {@link Departure}.
 *
 * In such a case, passengers can stay in the vehicle at the end of the {@link TransitRoute}.
 * The chained departure begins at the end of the current one.
 *
 * This functionality can be used to model "Flügelung" (dividing or merging trains), "Durchbindung" (through service)
 * or circular routes.
 * By doing so, the router or score calculation knows that a passenger can remain in the same vehicle.
 *
 * @author rakow
 */
public interface ChainedDeparture {

    /**
     * @return the id of the transit line that is chained
     */
	Id<TransitLine> getChainedTransitLineId();

    /**
     * @return the id of the transit route that is chained
     */
	Id<TransitRoute> getChainedRouteId();

    /**
     * @return the id of the departure that is chained
     */
	Id<Departure> getChainedDepartureId();

}
