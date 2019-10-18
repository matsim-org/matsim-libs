package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rehmann / VSP
 */

public class RaptorStopFinderTest {

    private Facility fromFac = new FakeFacility(new Coord(0, 0), Id.create("AA", Link.class)); // stop A
    private Facility toFac = new FakeFacility(new Coord(100000, 0), Id.create("XX", Link.class)); // stop X


    /** Empty Initial Search Radius
     * Tests the how the StopFinder reacts when there are no public transit stops within the Initial_Search_Radius.
     * The expected behaviour of the StopFinder is to find the closest public transit stop and set the new search
     * radius as the sum of the distance to the nearest stop and the Search_Extension_Radius. However, if the general
     * radius is smaller than this new search radius, then the StopFinder will  only search to the extents of the general
     * Radius.
     *
     * This functionality is tested for the two RaptorStopFinders: 1) DefaultStopFinder and 2) RandomAccessEgressModeRaptorStopFinder
     * For each RaptorStopFinder there is one test where StopFilterAttributes are not used to exlclude stops, and one test
     * where StopFilterAttributes are used. 
     */
    
    
    @Test
    public void testDefaultStopFinder_EmptyInitialSearchRadius() {
        /* General Radius includes no stops. Search_Extension_Radius is 0
        Expected: StopFinder will find no stops, since closest stop is outside of general radius. Therefore
        agent will use transit_walk to get from A to X.
         */

        {
            StopFinderFixture f0 = new StopFinderFixture(1., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(300); // Includes no stops
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(0);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 1, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.transit_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }
        /* General Radius includes stop B. Search_Extension_Radius is 0.
        Expected: Stop Finder will only find stop B. Lines C, D, and E are set to very fast, so as to check that only
        stop B is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(600., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // Includes stop B
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(0);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* General Radius includes B, C, D, and E
        Search_Extension_Radius includes B, C, D
        Expected: Stop D should be chosen, since line D is faster than lines B and C. Line E is super fast, but shouldn't
        be chosen since it is not within Search_Extension_Radius.
         */
        {
            StopFinderFixture f1 = new StopFinderFixture(20*60., 20.*60, 10*60., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(1100);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testDefaultStopFinder_EmptyInitialSearchRadius_StopFilterAttributes() {
        /* Initial_Search_Radius and General Radius contain only stop B.
        Stop B is not "walkAccessible"; all other stops are "walkAccessible" Search_Extension_Radius is 0
        Expected: StopFinder will find no stops, since closest accessible stop is outside of general radius. Therefore
        agent will use transit_walk to get from A to X.
         */

        {
            StopFinderFixture f0 = new StopFinderFixture(1., 1., 1., 1.);


            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }

            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // Includes stop B (not "walkAccessible")
            walkAccess.setInitialSearchRadius(600);  // Includes stop B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(0);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 1, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.transit_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* General Radius includes stop B. Search_Extension_Radius is 0.
        B is "walkAccessible"
        Expected: Stop Finder will only find stop B. Lines C, D, and E are set to very fast, so as to check that only
        stop B is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(600., 1., 1., 1.);

            String[] walkAccessibleStops = new String[]{"B", "X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // Includes stop B
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(0);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* Initial_Search_Radius includes B (not "walkAccessible"). General Radius includes stop B and C. Search_Extension_Radius is 0.
        All Stops are "walkAccessible" except for B.
        Expected: Stop Finder will only find stop C. Lines B, D, and E are set to very fast, so as to check that only
        stop C is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 600., 1., 1.);

            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(1100); // Includes stops B and C
            walkAccess.setInitialSearchRadius(600); // Includes stop B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(0);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* General Radius includes B, C, D, and E
        Initial_Search_Radius includes B (not "walkAccessible")
        Search_Extension_Radius includes B, C, D
        All stops are "walkAccessible" except for line B.
        Expected: Stop D should be chosen, since line D is faster than lines B and C. Line E is super fast, but shouldn't
        be chosen since it is not within Search_Extension_Radius. Line B is super fast, but shouldn't be chosen since it
        is not "walkAccessible".
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 20.*60, 10*60., 1.);

            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }

            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Includes stop B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(600); // Includes stop C and D
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testRandomAccessEgressModeRaptorStopFinder_EmptyInitialSearchRadius() {
        /* General Radius includes no stops. Search_Extension_Radius is 0
        Expected: StopFinder will find no stops, since closest stop is outside of general radius. Therefore
        agent will use transit_walk to get from A to X.
         */

        {
            StopFinderFixture f0 = new StopFinderFixture(1., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(300); // Includes no stops
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(0);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 1, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.transit_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }
        /* General Radius includes stop B. Search_Extension_Radius is 0.
        Expected: Stop Finder will only find stop B. Lines C, D, and E are set to very fast, so as to check that only
        stop B is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(600., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // Includes stop B
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(0);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* General Radius includes B, C, D, and E
        Search_Extension_Radius includes B, C, D
        Expected: Stop D should be chosen, since line D is faster than lines B and C. Line E is super fast, but shouldn't
        be chosen since it is not within Search_Extension_Radius.
         */
        {
            StopFinderFixture f1 = new StopFinderFixture(20*60., 20.*60, 10*60., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(1100);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testRandomAccessEgressModeRaptorStopFinder_EmptyInitialSearchRadius_StopFilterAttributes() {
        /* Initial_Search_Radius and General Radius contain only stop B.
        Stop B is not "walkAccessible"; all other stops are "walkAccessible" Search_Extension_Radius is 0
        Expected: StopFinder will find no stops, since closest accessible stop is outside of general radius. Therefore
        agent will use transit_walk to get from A to X.
         */

        {
            StopFinderFixture f0 = new StopFinderFixture(1., 1., 1., 1.);


            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }

            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // Includes stop B (not "walkAccessible")
            walkAccess.setInitialSearchRadius(600);  // Includes stop B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(0);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 1, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.transit_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* General Radius includes stop B. Search_Extension_Radius is 0.
        B is "walkAccessible"
        Expected: Stop Finder will only find stop B. Lines C, D, and E are set to very fast, so as to check that only
        stop B is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(600., 1., 1., 1.);

            String[] walkAccessibleStops = new String[]{"B", "X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // Includes stop B
            walkAccess.setInitialSearchRadius(300); // Includes no stops
            walkAccess.setSearchExtensionRadius(0);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* Initial_Search_Radius includes B (not "walkAccessible"). General Radius includes stop B and C. Search_Extension_Radius is 0.
        All Stops are "walkAccessible" except for B.
        Expected: Stop Finder will only find stop C. Lines B, D, and E are set to very fast, so as to check that only
        stop C is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 600., 1., 1.);

            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(1100); // Includes stops B and C
            walkAccess.setInitialSearchRadius(600); // Includes stop B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(0);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());

        }

        /* General Radius includes B, C, D, and E
        Initial_Search_Radius includes B (not "walkAccessible")
        Search_Extension_Radius includes B, C, D
        All stops are "walkAccessible" except for line B.
        Expected: Stop D should be chosen, since line D is faster than lines B and C. Line E is super fast, but shouldn't
        be chosen since it is not within Search_Extension_Radius. Line B is super fast, but shouldn't be chosen since it
        is not "walkAccessible".
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 20.*60, 10*60., 1.);

            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }

            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Includes stop B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(600); // Includes stop C and D
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    // ***********************************************************************************************************

    /** Half Full Initial Search Radius
     * Tests the how the StopFinder reacts when there is only one public transit stop within the Initial_Search_Radius.
     * In this case, the Initial_Search_Radius will always contain stop B, but no other stops.
     * The expected behaviour of the StopFinder define a new extended search radius, which is the sum of the distance
     * to stop B and the Search_Extension_Radius. However, if the general
     * radius is smaller than this new search radius, then the StopFinder will  only search to the extents of the general
     * Radius.
     *
     * This functionality is tested for the two RaptorStopFinders: 1) DefaultStopFinder and 2) RandomAccessEgressModeRaptorStopFinder
     * For each RaptorStopFinder there is one test where StopFilterAttributes are not used to exlclude stops, and one test
     * where StopFilterAttributes are used. 
     */
    @Test
    public void testDefaultStopFinder_HalfFullInitialSearchRadius() {
        /* Initial_Search_Radius includes B. Search_Extension_Radius is 0.
        General_Radius includes B, C, D and E
        Expected: Stop Finder will only find stop B, since the Search_Extension_Radius doesn't encompass more stops.
        Lines C, D, and E are set to very fast, so as to check that only stop B is included.
         */
        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(0);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }


        /* Initial_Search_Radius includes B. Search_Extension_Radius includes B and C.
        General_Radius includes B, C, D and E.
        Line C is faster than line B. Lines D and E are super fast,
        Expected: Stop Finder will chose stop C, since line C is faster than B, and the other stops are not included in
        extended search radius.
         */
        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 10. * 60, 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(600);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }


        /* Initial_Search_Radius includes B. Search_Extension_Radius includes B and C.
        General_Radius only includes B.
        B is slow, all other lines are super fast
        Expected: Stop Finder will chose stop B, since it is only stop included in general search radius.
         */

        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // only includes B
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(500); // includes C
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testDefaultStopFinder_HalfFullInitialSearchRadius_StopFilterAttributes() {
        /* Initial_Search_Radius includes B and C. Search_Extension_Radius is 0.
        General_Radius includes B, C, D and E
        Stop B is not "walkAccessible", but all others are.
        Expected: Stop Finder will only find stop C, since the Search_Extension_Radius doesn't encompass more stops.
        Lines B, D, and E are set to very fast, so as to check that only stop C is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 600., 1., 1.);
            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1100); // Should include stops C and B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(0);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }


        /* Initial_Search_Radius includes B. Search_Extension_Radius includes B, C, and D
        General_Radius includes B, C, D and E.
        Line C is faster than line B. Lines D and E are super fast,
        All stops are "walkAccessible" except for stop D
        Expected: Stop Finder will chose stop C, since line C is faster than B. D will be excluded since it doesn't fulfill
        the attribute. All other stops are not included in the extended search radius.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(20 * 60., 10. * 60, 1., 1.);
            String[] walkAccessibleStops = new String[]{"B","C","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(1100); // Should include stops C and D
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testRandomAccessEgressModeRaptorStopFinder_HalfFullInitialSearchRadius() {
        /* Initial_Search_Radius includes B. Search_Extension_Radius is 0.
        General_Radius includes B, C, D and E
        Expected: Stop Finder will only find stop B, since the Search_Extension_Radius doesn't encompass more stops.
        Lines C, D, and E are set to very fast, so as to check that only stop B is included.
         */
        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(0);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }


        /* Initial_Search_Radius includes B. Search_Extension_Radius includes B and C.
        General_Radius includes B, C, D and E.
        Line C is faster than line B. Lines D and E are super fast,
        Expected: Stop Finder will chose stop C, since line C is faster than B, and the other stops are not included in
        extended search radius.
         */
        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 10. * 60, 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(600);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }


        /* Initial_Search_Radius includes B. Search_Extension_Radius includes B and C.
        General_Radius only includes B.
        B is slow, all other lines are super fast
        Expected: Stop Finder will chose stop B, since it is only stop included in general search radius.
         */

        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 1., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(600); // only includes B
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(500); // includes C
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testRandomAccessEgressModeRaptorStopFinder_HalfFullInitialSearchRadius_StopFilterAttributes() {
        /* Initial_Search_Radius includes B and C. Search_Extension_Radius is 0.
        General_Radius includes B, C, D and E
        Stop B is not "walkAccessible", but all others are.
        Expected: Stop Finder will only find stop C, since the Search_Extension_Radius doesn't encompass more stops.
        Lines B, D, and E are set to very fast, so as to check that only stop C is included.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 600., 1., 1.);
            String[] walkAccessibleStops = new String[]{"C","D","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1100); // Should include stops C and B (not "walkAccessible")
            walkAccess.setSearchExtensionRadius(0);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }


        /* Initial_Search_Radius includes B. Search_Extension_Radius includes B, C, and D
        General_Radius includes B, C, D and E.
        Line C is faster than line B. Lines D and E are super fast,
        All stops are "walkAccessible" except for stop D
        Expected: Stop Finder will chose stop C, since line C is faster than B. D will be excluded since it doesn't fulfill
        the attribute. All other stops are not included in the extended search radius.
         */
        {
            StopFinderFixture f0 = new StopFinderFixture(20 * 60., 10. * 60, 1., 1.);
            String[] walkAccessibleStops = new String[]{"B","C","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(600); // Should include stops B
            walkAccess.setSearchExtensionRadius(1100); // Should include stops C and D
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    // ***********************************************************************************************************

    /** Full Initial Search Radius
     * Tests the how the StopFinder reacts when there are at least 2 stop within the Initial_Search_Radius. In the
     * following tests, the Initial_Search_Radius includes stops B and C.
     * The StopFinder should then not find stops outside of Initial_Search_Radius, even if the Search_Extension_Radius
     * is initialized. If the general Radius is smaller than the Initial_Search_Radius, then the StopFinder should only
     * search to the extents of the general radius.
     *
     * This functionality is tested for the two RaptorStopFinders: 1) DefaultStopFinder and 2) RandomAccessEgressModeRaptorStopFinder
     * For each RaptorStopFinder there is one test where StopFilterAttributes are not used to exlclude stops, and one test
     * where StopFilterAttributes are used. 
     */
    @Test
    public void testDefaultStopFinder_FullInitialSearchRadius() {


         /* Search_Extension_Radius includes D and E
         General_Radius includes B, C, D, E
         Line B is faster than C, while lines D and E are very fast
         Expected: B is chosen, since it is faster than C. D and E shouldn't be chosen, since the Initial_Search_Radius
         is already full.
          */
        {
            StopFinderFixture f0 = new StopFinderFixture(10 * 60., 20 * 60., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1200); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(2000);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }


        /* Search_Extension_Radius includes D and E
         General_Radius includes B, C, D, E
         Line C is faster than B, while lines D and E are very fast
         Expected: C is chosen, since it is faster than B. D and E shouldn't be chosen, since the Initial_Search_Radius
         is already full.
          */
        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 10 * 60., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1200); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(0);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testDefaultStopFinder_FullInitialSearchRadius_StopFilterAttributes() {


         /* Initial_Search_Radius includes B, C, and D
         Search_Extension_Radius includes E
         General_Radius includes B, C, D, E
         All stops are walkAccessible except for D.
         Line B is faster than C, while lines D and E are very fast
         Expected: B is chosen, since it is faster than C. D shouldn't be chosen since it is not "walkAccessible" and E
         shouldn't be chosen, since the Initial_Search_Radius is already full.
          */
        {
            StopFinderFixture f0 = new StopFinderFixture(10 * 60., 20 * 60., 1., 1.);
            String[] walkAccessibleStops = new String[]{"B","C","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1600); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(500);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }


          /* Initial_Search_Radius includes B, C, and D
         Search_Extension_Radius includes E
         General_Radius includes B, C, D, E
         All stops are walkAccessible except for D.
         Line C is faster than B, while lines D and E are very fast
         Expected: C is chosen, since it is faster than B. D shouldn't be chosen since it is not "walkAccessible" and E
         shouldn't be chosen, since the Initial_Search_Radius is already full.
          */
        {
            StopFinderFixture f0 = new StopFinderFixture(20 * 60., 10 * 60., 1., 1.);
            String[] walkAccessibleStops = new String[]{"B","C","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1600); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(500);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }
    }

    @Test
    public void testRandomAccessEgressModeRaptorStopFinder_FullInitialSearchRadius() {


         /* Search_Extension_Radius includes D and E
         General_Radius includes B, C, D, E
         Line B is faster than C, while lines D and E are very fast
         Expected: B is chosen, since it is faster than C. D and E shouldn't be chosen, since the Initial_Search_Radius
         is already full.
          */
        {
            StopFinderFixture f0 = new StopFinderFixture(10 * 60., 20 * 60., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1200); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(2000);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }


        /* Search_Extension_Radius includes D and E
         General_Radius includes B, C, D, E
         Line C is faster than B, while lines D and E are very fast
         Expected: C is chosen, since it is faster than B. D and E shouldn't be chosen, since the Initial_Search_Radius
         is already full.
          */
        {
            StopFinderFixture f1 = new StopFinderFixture(20 * 60., 10 * 60., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f1.scenario, 1.1, 1.3));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f1.scenario, 1000., 1.0));

            f1.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1200); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(0);
            f1.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f1.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f1.config), f1.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f1.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f1.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
        }

    }

    @Test
    public void testRandomAccessEgressModeRaptorStopFinder_FullInitialSearchRadius_StopFilterAttributes() {


         /* Initial_Search_Radius includes B, C, and D
         Search_Extension_Radius includes E
         General_Radius includes B, C, D, E
         All stops are walkAccessible except for D.
         Line B is faster than C, while lines D and E are very fast
         Expected: B is chosen, since it is faster than C. D shouldn't be chosen since it is not "walkAccessible" and E
         shouldn't be chosen, since the Initial_Search_Radius is already full.
          */
        {
            StopFinderFixture f0 = new StopFinderFixture(10 * 60., 20 * 60., 1., 1.);
            String[] walkAccessibleStops = new String[]{"B","C","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1600); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(500);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }


          /* Initial_Search_Radius includes B, C, and D
         Search_Extension_Radius includes E
         General_Radius includes B, C, D, E
         All stops are walkAccessible except for D.
         Line C is faster than B, while lines D and E are very fast
         Expected: C is chosen, since it is faster than B. D shouldn't be chosen since it is not "walkAccessible" and E
         shouldn't be chosen, since the Initial_Search_Radius is already full.
          */
        {
            StopFinderFixture f0 = new StopFinderFixture(20 * 60., 10 * 60., 1., 1.);
            String[] walkAccessibleStops = new String[]{"B","C","E","X"};
            for (String stop : walkAccessibleStops) {
                f0.scenario.getTransitSchedule().getFacilities().get(Id.create(stop, TransitStopFacility.class)).getAttributes().putAttribute("walkAccessible", "true") ;
            }
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 1000., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 1000., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1600); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(500);
            walkAccess.setStopFilterAttribute("walkAccessible");
            walkAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            RandomAccessEgressModeRaptorStopFinder stopFinder = new RandomAccessEgressModeRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("CC", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }
    }


    // ***********************************************************************************************************
    @Deprecated
    @Test
    public void testDefaultStopFinder_testMultipleModes() {

        // Test 7: Test Stop Filter Attributes
        // Initial_Search_Radius includes B and C and D
        // C and D have attribute "zoomerAccessible" as "true".
        // Search_Extension_Radius is 0
        // General_Radius includes B, C, D, E
        // B and D are faster than C
        // expected: D, since it is faster and it has correct attribute
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 20*60., 10*60., 1.);

            f0.scenario.getTransitSchedule().getFacilities().get(Id.create("C", TransitStopFacility.class)).getAttributes().putAttribute("zoomerAccessible", "true");
            f0.scenario.getTransitSchedule().getFacilities().get(Id.create("D", TransitStopFacility.class)).getAttributes().putAttribute("zoomerAccessible", "true");

            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 5, 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 5, 1.0));
            routingModules.put("zoomer",
                    new TeleportationRoutingModule("zoomer", f0.scenario, 1000., 1.));

            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("zoomer");
            modeParams.setMarginalUtilityOfTraveling(0.);
            f0.scenario.getConfig().planCalcScore().addModeParams(modeParams);

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet zoomerAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            zoomerAccess.setMode("zoomer");
            zoomerAccess.setMaxRadius(2000); // should not be limiting factor
            zoomerAccess.setInitialSearchRadius(1700); // Should include stops B and C and D
            zoomerAccess.setSearchExtensionRadius(0); // includes D (if neccessary)
            zoomerAccess.setStopFilterAttribute("zoomerAccessible");
            zoomerAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(zoomerAccess);

            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet nonNetworkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            nonNetworkAccess.setMode(TransportMode.non_network_walk);
            nonNetworkAccess.setMaxRadius(0); // should not be limiting factor
            nonNetworkAccess.setInitialSearchRadius(0); // Should include stops B and C and D
            nonNetworkAccess.setSearchExtensionRadius(0); // includes D (if neccessary)
            f0.srrConfig.addIntermodalAccessEgress(nonNetworkAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals("zoomer", leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }

        // Test 8: Test Stop Filter Attributes
        // Initial_Search_Radius includes B and C
        // C and D have attribute "zoomerAccessible" as "true".
        // Search_Extension_Radius is 600, should include D if it extends based on C (which is nearest stop with correct attribute)
        // General_Radius includes B, C, D, E
        // B and D are faster than C
        // expected: D, since it is faster and it has correct attribute
        {
            StopFinderFixture f0 = new StopFinderFixture(1., 20*60., 10*60., 1.);

            f0.scenario.getTransitSchedule().getFacilities().get(Id.create("C", TransitStopFacility.class)).getAttributes().putAttribute("zoomerAccessible", "true");
            f0.scenario.getTransitSchedule().getFacilities().get(Id.create("D", TransitStopFacility.class)).getAttributes().putAttribute("zoomerAccessible", "true");

            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 5, 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 5, 1.0));
            routingModules.put("zoomer",
                    new TeleportationRoutingModule("zoomer", f0.scenario, 1000., 1.));

            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("zoomer");
            modeParams.setMarginalUtilityOfTraveling(0.);
            f0.scenario.getConfig().planCalcScore().addModeParams(modeParams);

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet zoomerAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            zoomerAccess.setMode("zoomer");
            zoomerAccess.setMaxRadius(2000); // should not be limiting factor
            zoomerAccess.setInitialSearchRadius(1100); // Should include stops B and C and D
            zoomerAccess.setSearchExtensionRadius(600); // includes D (if neccessary)
            zoomerAccess.setStopFilterAttribute("zoomerAccessible");
            zoomerAccess.setStopFilterValue("true");
            f0.srrConfig.addIntermodalAccessEgress(zoomerAccess);

            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(0); // should not be limiting factor
            walkAccess.setInitialSearchRadius(0); // Should include stops B and C and D
            walkAccess.setSearchExtensionRadius(0); // includes D (if neccessary)
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

//            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet nonNetworkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
//            nonNetworkAccess.setMode(TransportMode.non_network_walk);
//            nonNetworkAccess.setRadius(0); // should not be limiting factor
//            nonNetworkAccess.setInitialSearchRadius(0); // Should include stops B and C and D
//            nonNetworkAccess.setSearchExtensionRadius(0); // includes D (if neccessary)
//            f0.srrConfig.addIntermodalAccessEgress(nonNetworkAccess);



            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals("zoomer", leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("DD", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
            Assert.assertEquals(TransportMode.non_network_walk, leg.getMode());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }






        // *** P A R T  2 : Walk & Bike Only Tests ***

        // Test 1: Initial_Search_Radius includes B and C, line B is faster than C
        // Search_Extension_Radius includes D and E
        // General_Radius includes B, C, D, E
        // D and E are very fast, but shouldn't be chosen, since Initial_Search_Radius already has 2 entries
        // expected: B
        {
            StopFinderFixture f0 = new StopFinderFixture(10*60., 20*60., 1., 1.);
            Map<String, RoutingModule> routingModules = new HashMap<>();
            routingModules.put(TransportMode.walk,
                    new TeleportationRoutingModule(TransportMode.walk, f0.scenario, 5., 1.0));
            routingModules.put(TransportMode.non_network_walk,
                    new TeleportationRoutingModule(TransportMode.non_network_walk, f0.scenario, 5., 1.0));
            routingModules.put(TransportMode.bike,
                    new TeleportationRoutingModule(TransportMode.bike, f0.scenario, 50., 1.0));

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            walkAccess.setMode(TransportMode.non_network_walk);
            walkAccess.setMaxRadius(10000000); // should not be limiting factor
            walkAccess.setInitialSearchRadius(1200); // Should include stops B and C
            walkAccess.setSearchExtensionRadius(2000);
            f0.srrConfig.addIntermodalAccessEgress(walkAccess);

            f0.srrConfig.setUseIntermodalAccessEgress(true);
            SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet bikeAccess = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
            bikeAccess.setMode(TransportMode.bike);
            bikeAccess.setMaxRadius(10000000); // should not be limiting factor
            bikeAccess.setInitialSearchRadius(1200); // Should include stops B and C
            bikeAccess.setSearchExtensionRadius(2000);
            f0.srrConfig.addIntermodalAccessEgress(bikeAccess);

            SwissRailRaptorData data = SwissRailRaptorData.create(f0.scenario.getTransitSchedule(), RaptorUtils.createStaticConfig(f0.config), f0.scenario.getNetwork());
            DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), routingModules);
            SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(f0.scenario.getConfig()),
                    new LeastCostRaptorRouteSelector(), stopFinder, null);

            List<Leg> legs = raptor.calcRoute(fromFac, toFac, 7 * 3600, f0.dummyPerson);
            for (Leg leg : legs) {
                System.out.println(leg);
            }

            Assert.assertEquals("wrong number of legs.", 3, legs.size());
            Leg leg = legs.get(0);
            Assert.assertEquals(TransportMode.bike, leg.getMode());
            Assert.assertEquals(Id.create("AA", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(1);
            Assert.assertEquals(TransportMode.pt, leg.getMode());
            Assert.assertEquals(Id.create("BB", Link.class), leg.getRoute().getStartLinkId());
            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());
            leg = legs.get(2);
//            Assert.assertEquals(TransportMode.bike, leg.getMode());
//            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getStartLinkId());
//            Assert.assertEquals(Id.create("XX", Link.class), leg.getRoute().getEndLinkId());


        }
    }

    private static class StopFinderFixture {

        final SwissRailRaptorConfigGroup srrConfig;
        final Config config;
        final Scenario scenario;
        final Person dummyPerson;
        Network network;
        NetworkFactory nf;
//        final Map<String, RoutingModule> routingModules;

        final double offsetB;
        double offsetC;
        final double offsetD;
        final double offsetE;

        public StopFinderFixture(double offsetB, double offsetC, double offsetD, double offsetE) {
            this.srrConfig = new SwissRailRaptorConfigGroup();
            this.config = ConfigUtils.createConfig(this.srrConfig);
            this.scenario = ScenarioUtils.createScenario(this.config);

            this.offsetB = offsetB;
            this.offsetC = offsetC;
            this.offsetD = offsetD;
            this.offsetE = offsetE;

            /* Scenario:

            (A)     (B)     (C)     (D)     (E)                                 (X)
                     \       \       \       \__________________________________/         ELine
                      \       \       \________________________________________/          DLine
                       \       \______________________________________________/           CLine
                        \____________________________________________________/            BLine


             The distance between stops A, B, C, D, and E is 500, respectively. The distance from A to X is 100000.
             */

            this.network = this.scenario.getNetwork();
            this.nf = network.getFactory();

            Node nodeA = nf.createNode(Id.create("A", Node.class), new Coord(0, 0));
            Node nodeB = nf.createNode(Id.create("B", Node.class), new Coord(500, 0));
            Node nodeC = nf.createNode(Id.create("C", Node.class), new Coord(1000, 0));
            Node nodeD = nf.createNode(Id.create("D", Node.class), new Coord(1500, 0));
            Node nodeE = nf.createNode(Id.create("E", Node.class), new Coord(2000, 0));
            Node nodeX = nf.createNode(Id.create("X", Node.class), new Coord(100000, 0));

            network.addNode(nodeA);
            network.addNode(nodeB);
            network.addNode(nodeC);
            network.addNode(nodeD);
            network.addNode(nodeE);
            network.addNode(nodeX);

            Link linkAA = nf.createLink(Id.create("AA", Link.class), nodeA, nodeA);
            Link linkAB = nf.createLink(Id.create("AB", Link.class), nodeA, nodeB);
            Link linkBA = nf.createLink(Id.create("BA", Link.class), nodeB, nodeA);
            Link linkBB = nf.createLink(Id.create("BB", Link.class), nodeB, nodeB);
            Link linkBX = nf.createLink(Id.create("BX", Link.class), nodeB, nodeX);
            Link linkXB = nf.createLink(Id.create("XB", Link.class), nodeX, nodeB);

            Link linkCC = nf.createLink(Id.create("CC", Link.class), nodeC, nodeC);
            Link linkCX = nf.createLink(Id.create("CX", Link.class), nodeC, nodeX);
            Link linkXC = nf.createLink(Id.create("XC", Link.class), nodeX, nodeC);

            Link linkDD = nf.createLink(Id.create("DD", Link.class), nodeD, nodeD);
            Link linkDX = nf.createLink(Id.create("DX", Link.class), nodeD, nodeX);
            Link linkXD = nf.createLink(Id.create("XD", Link.class), nodeX, nodeD);

            Link linkEE = nf.createLink(Id.create("EE", Link.class), nodeE, nodeE);
            Link linkEX = nf.createLink(Id.create("EX", Link.class), nodeE, nodeX);
            Link linkXE = nf.createLink(Id.create("XE", Link.class), nodeX, nodeE);

            Link linkXX = nf.createLink(Id.create("XX", Link.class), nodeX, nodeX);

            network.addLink(linkAA);
            network.addLink(linkAB);
            network.addLink(linkBA);
            network.addLink(linkBB);
            network.addLink(linkBX);
            network.addLink(linkXB);

            network.addLink(linkCC);
            network.addLink(linkCX);
            network.addLink(linkXC);

            network.addLink(linkDD);
            network.addLink(linkDX);
            network.addLink(linkXD);

            network.addLink(linkEE);
            network.addLink(linkEX);
            network.addLink(linkXE);

            network.addLink(linkXX);

            // ----

            TransitSchedule schedule = this.scenario.getTransitSchedule();
            TransitScheduleFactory sf = schedule.getFactory();

            TransitStopFacility stopB = sf.createTransitStopFacility(Id.create("B", TransitStopFacility.class), nodeB.getCoord(), false);
            TransitStopFacility stopC = sf.createTransitStopFacility(Id.create("C", TransitStopFacility.class), nodeC.getCoord(), false);
            TransitStopFacility stopD = sf.createTransitStopFacility(Id.create("D", TransitStopFacility.class), nodeD.getCoord(), false);
            TransitStopFacility stopE = sf.createTransitStopFacility(Id.create("E", TransitStopFacility.class), nodeE.getCoord(), false);
            TransitStopFacility stopX = sf.createTransitStopFacility(Id.create("X", TransitStopFacility.class), nodeX.getCoord(), false);

            stopB.setLinkId(linkBB.getId());
            stopC.setLinkId(linkCC.getId());
            stopD.setLinkId(linkDD.getId());
            stopE.setLinkId(linkEE.getId());
            stopX.setLinkId(linkXX.getId());

            schedule.addStopFacility(stopB);
            schedule.addStopFacility(stopC);
            schedule.addStopFacility(stopD);
            schedule.addStopFacility(stopE);
            schedule.addStopFacility(stopX);

            // B transit line
            TransitLine BLine = sf.createTransitLine(Id.create("BLine", TransitLine.class));

            NetworkRoute networkRouteBX = RouteUtils.createLinkNetworkRouteImpl(linkBB.getId(), new Id[]{linkBX.getId()}, linkBB.getId());
            List<TransitRouteStop> stopsBX = new ArrayList<>(2);
            stopsBX.add(sf.createTransitRouteStop(stopB, Time.getUndefinedTime(), 0.0));
            stopsBX.add(sf.createTransitRouteStop(stopX, offsetB, Time.getUndefinedTime()));
            TransitRoute BXRoute = sf.createTransitRoute(Id.create("lineBX", TransitRoute.class), networkRouteBX, stopsBX, "train");
            BXRoute.addDeparture(sf.createDeparture(Id.create("1", Departure.class), 8. * 3600));
            BLine.addRoute(BXRoute);

            schedule.addTransitLine(BLine);

            // C transit line
            TransitLine CLine = sf.createTransitLine(Id.create("CLine", TransitLine.class));

            NetworkRoute networkRouteCX = RouteUtils.createLinkNetworkRouteImpl(linkCC.getId(), new Id[]{linkCX.getId()}, linkCC.getId());
            List<TransitRouteStop> stopsCX = new ArrayList<>(3);
            stopsCX.add(sf.createTransitRouteStop(stopC, Time.getUndefinedTime(), 0.0));
            stopsCX.add(sf.createTransitRouteStop(stopX, offsetC, Time.getUndefinedTime()));
            TransitRoute CXRoute = sf.createTransitRoute(Id.create("lineCX", TransitRoute.class), networkRouteCX, stopsCX, "train");
            CXRoute.addDeparture(sf.createDeparture(Id.create("1", Departure.class), 8. * 3600));
            CLine.addRoute(CXRoute);

            schedule.addTransitLine(CLine);

            // D transit line
            TransitLine DLine = sf.createTransitLine(Id.create("DLine", TransitLine.class));

            NetworkRoute networkRouteDX = RouteUtils.createLinkNetworkRouteImpl(linkDD.getId(), new Id[]{linkDX.getId()}, linkDD.getId());
            List<TransitRouteStop> stopsDX = new ArrayList<>(2);
            stopsDX.add(sf.createTransitRouteStop(stopD, Time.getUndefinedTime(), 0.0));
            stopsDX.add(sf.createTransitRouteStop(stopX, offsetD, Time.getUndefinedTime()));
            TransitRoute DXRoute = sf.createTransitRoute(Id.create("lineDX", TransitRoute.class), networkRouteDX, stopsDX, "train");
            DXRoute.addDeparture(sf.createDeparture(Id.create("1", Departure.class), 8. * 3600));
            DLine.addRoute(DXRoute);


            schedule.addTransitLine(DLine);

            // E transit line
            TransitLine ELine = sf.createTransitLine(Id.create("ELine", TransitLine.class));

            NetworkRoute networkRouteEX = RouteUtils.createLinkNetworkRouteImpl(linkEE.getId(), new Id[]{linkEX.getId()}, linkEE.getId());
            List<TransitRouteStop> stopsEX = new ArrayList<>(2);
            stopsEX.add(sf.createTransitRouteStop(stopE, Time.getUndefinedTime(), 0.0));
            stopsEX.add(sf.createTransitRouteStop(stopX, offsetE, Time.getUndefinedTime()));
            TransitRoute EXRoute = sf.createTransitRoute(Id.create("lineEX", TransitRoute.class), networkRouteEX, stopsEX, "train");
            EXRoute.addDeparture(sf.createDeparture(Id.create("1", Departure.class), 8. * 3600));
            ELine.addRoute(EXRoute);

            schedule.addTransitLine(ELine);

            // ---

            this.dummyPerson = this.scenario.getPopulation().getFactory().createPerson(Id.create("dummy", Person.class));

        }
    }
}
