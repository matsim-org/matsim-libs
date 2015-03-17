package playground.pieter.distributed.plans;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;

/**
 * Created by fouriep on 1/30/15.
 * can't just extend PopulationFactoryImpl because its constructor is non-public.
 * Consequently, lots of code needs replicating because of casts to PopulationFactoryImpl by the triprouter etc.
 */
public class PopulationFactoryForPlanGenomes implements PopulationFactory {

    private final ModeRouteFactory routeFactory;

    PopulationFactoryForPlanGenomes(ModeRouteFactory routeFactory) {
        this.routeFactory = routeFactory;
    }

    @Override
    public Person createPerson(final Id<Person> id) {
        return new PersonForPlanGenomes(id);
    }

    @Override
    public Plan createPlan(){
        return new PlanImpl();
    }

    @Override
    public Activity createActivityFromCoord(final String actType, final Coord coord) {
        return new ActivityImpl(actType, coord);
    }

    @Override
    public Activity createActivityFromLinkId(final String actType, final Id<Link> linkId) {
        return new ActivityImpl(actType, linkId);
    }

    @Override
    public Leg createLeg(final String legMode) {
        return new LegImpl(legMode);
    }

    /**
     * @param transportMode the transport mode the route should be for
     * @param startLinkId the link where the route starts
     * @param endLinkId the link where the route ends
     * @return a new Route for the specified mode
     *
     * @see #setRouteFactory(String, org.matsim.core.population.routes.RouteFactory)
     */
    @Override
    public Route createRoute(final String transportMode, final Id<Link> startLinkId, final Id<Link> endLinkId) {
        return this.routeFactory.createRoute(transportMode, startLinkId, endLinkId);
    }

    /**
     * Registers a {@link org.matsim.core.population.routes.RouteFactory} for the specified mode. If <code>factory</code> is <code>null</code>,
     * the existing entry for this <code>mode</code> will be deleted. If <code>mode</code> is <code>null</code>,
     * then the default factory is set that is used if no specific RouteFactory for a mode is set.
     *
     */
    public void setRouteFactory(final String transportMode, final RouteFactory factory) {
        this.routeFactory.setRouteFactory(transportMode, factory);
    }

    public ModeRouteFactory getModeRouteFactory() {
        return this.routeFactory;
    }
}
