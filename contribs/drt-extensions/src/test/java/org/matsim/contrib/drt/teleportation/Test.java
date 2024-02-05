package org.matsim.contrib.drt.teleportation;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRouteCreator;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtModeRoutingModule;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.net.URL;
import java.util.*;

class Test{

	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils();

	@org.junit.jupiter.api.Test void test1() {

		URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "mielec" ), "empty_config.xml" );
		Config config = ConfigUtils.loadConfig( url );

		config.network().setInputFile( "network.xml" );
		config.plans().setInputFile( "plans_only_drt_1.0.xml.gz" );

		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setLastIteration( 0 );

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory( config );
		ScenarioUtils.loadScenario( scenario );

		Controler controler = new Controler( scenario );
		controler.addOverridingModule( new AbstractDvrpModeModule("drt"){
			@Override public void install(){
				DrtConfigGroup drtCfg = ConfigUtils.addOrGetModule( this.getConfig(), DrtConfigGroup.class );

				this.addRoutingModuleBinding( "drt" ).toProvider( new DrtEstimatingRoutingModuleProvider( "drt" ) );

				modalMapBinder(DvrpRoutingModuleProvider.Stage.class, RoutingModule.class).addBinding(
															  DvrpRoutingModuleProvider.Stage.MAIN)
													  .toProvider(new DvrpModeRoutingModule.DefaultMainLegRouterProvider(getMode()) );// not singleton
				// this seems to bind an enum.  maybe more heavyweight than necessary?

				bindModal( DefaultMainLegRouter.RouteCreator.class ).toProvider(
						new DrtRouteCreatorProvider(drtCfg) );// not singleton
				// this is used in DvrpModeRoutingModule (recruited by the DvrpRoutingModuleProvider above)

				bindModal( DrtStopNetwork.class ).toProvider(new DrtStopNetworkProvider(getConfig(), drtCfg) ).asEagerSingleton();
				// yyyy possibly not used for door2door; try to move inside the corresponding switch statement below.  kai, feb'24

				switch( drtCfg.operationalScheme ){
					case door2door -> bindModal( DvrpRoutingModule.AccessEgressFacilityFinder.class ).toProvider(
																	 modalProvider( getter -> new DecideOnLinkAccessEgressFacilityFinder( getter.getModal( Network.class ) ) ) )
															 .asEagerSingleton();
					case stopbased, serviceAreaBased -> {
						bindModal( DvrpRoutingModule.AccessEgressFacilityFinder.class ).toProvider( modalProvider(
															       getter -> new ClosestAccessEgressFacilityFinder( drtCfg.maxWalkDistance,
																	       getter.get( Network.class ),
																	       QuadTrees.createQuadTree( getter.getModal( DrtStopNetwork.class ).getDrtStops().values() ) ) ) )
													       .asEagerSingleton();
					}
					default -> throw new IllegalStateException( "Unexpected value: " + drtCfg.operationalScheme );
				}

			}
		} );

		controler.run();

	}

	private static class DrtEstimatingRoutingModule implements RoutingModule {
		private static final Logger logger = LogManager.getLogger( DrtEstimatingRoutingModule.class );

		public interface AccessEgressFacilityFinder {
			Optional<Pair<Facility, Facility>> findFacilities( Facility fromFacility, Facility toFacility,
									   Attributes tripAttributes );
		}

		private final AccessEgressFacilityFinder stopFinder;
		private final String mode;
		private final RoutingModule mainRouter;
		private final RoutingModule accessRouter;
		private final RoutingModule egressRouter;
		private final TimeInterpretation timeInterpretation;

		public DrtEstimatingRoutingModule( RoutingModule mainRouter, RoutingModule accessRouter, RoutingModule egressRouter,
					  AccessEgressFacilityFinder stopFinder, String mode, TimeInterpretation timeInterpretation ) {
			this.mainRouter = mainRouter;
			this.stopFinder = stopFinder;
			this.mode = mode;
			this.accessRouter = accessRouter;
			this.egressRouter = egressRouter;
			this.timeInterpretation = timeInterpretation;
		}

		@Override
		public List<? extends PlanElement> calcRoute(RoutingRequest request) {
			final Facility fromFacility = request.getFromFacility();
			final Facility toFacility = request.getToFacility();
			final double departureTime = request.getDepartureTime();
			final Person person = request.getPerson();

			Optional<Pair<Facility, Facility>> stops = stopFinder.findFacilities(
					Objects.requireNonNull(fromFacility, "fromFacility is null" ),
					Objects.requireNonNull(toFacility, "toFacility is null"), request.getAttributes());
			if (stops.isEmpty()) {
				logger.debug("No access/egress stops found, agent will use fallback mode as leg mode (usually "
							     + TransportMode.walk
							     + ") and routing mode "
							     + mode
							     + ". Agent Id:\t"
							     + person.getId());
				return null;
			}

			Facility accessFacility = stops.get().getLeft();
			Facility egressFacility = stops.get().getRight();
			if (accessFacility.getLinkId().equals(egressFacility.getLinkId())) {
				logger.debug("Start and end stop are the same, agent will use fallback mode as leg mode (usually "
							     + TransportMode.walk
							     + ") and routing mode "
							     + mode
							     + ". Agent Id:\t"
							     + person.getId());
				return null;
			}

			List<PlanElement> trip = new ArrayList<>();

			double now = departureTime;

			// access (sub-)trip:
			List<? extends PlanElement> accessTrip = accessRouter.calcRoute(
					DefaultRoutingRequest.of(fromFacility, accessFacility, now, person, request.getAttributes() ) );
			if (!accessTrip.isEmpty()) {
				trip.addAll(accessTrip);
				now = timeInterpretation.decideOnElementsEndTime(accessTrip, now).seconds();

				// interaction activity:
				trip.add(createDrtStageActivity(accessFacility, now));
			}

			// dvrp proper leg:
			List<? extends PlanElement> drtLeg = mainRouter.calcRoute(
					DefaultRoutingRequest.of(accessFacility, egressFacility, now, person, request.getAttributes()));
			trip.addAll(drtLeg);
			now = timeInterpretation.decideOnElementsEndTime(drtLeg, now).seconds();

			// egress (sub-)trip:
			List<? extends PlanElement> egressTrip = egressRouter.calcRoute(
					DefaultRoutingRequest.of(egressFacility, toFacility, now, person, request.getAttributes()));
			if (!egressTrip.isEmpty()) {
				// interaction activity:
				trip.add(createDrtStageActivity(egressFacility, now));

				trip.addAll(egressTrip);
			}

			return trip;
		}

		private Activity createDrtStageActivity( Facility stopFacility, double now ) {
			return PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(stopFacility.getCoord(),
					stopFacility.getLinkId(), mode );
		}
	}

	private static class DrtEstimatingRoutingModuleProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtEstimatingRoutingModule> {
		public enum Stage {ACCESS, MAIN, EGRESS}

		@Inject
		@Named(TransportMode.walk)
		private RoutingModule walkRouter;

		@Inject private TimeInterpretation timeInterpretation;

		@Inject private TripRouter tripRouter;

		public DrtEstimatingRoutingModuleProvider(String mode) {
			super(mode, DvrpModes::mode );
		}

		@Override
		public DrtEstimatingRoutingModule get() {

			RoutingModule drtRoutingModule = tripRouter.getRoutingModule( "drt" );
//			drtRoutingModule.c

			Map<DvrpRoutingModuleProvider.Stage, RoutingModule> stageRouters = getModalInstance( new TypeLiteral<Map<DvrpRoutingModuleProvider.Stage, RoutingModule>>() {
			} );
			RoutingModule mainRouter = Objects.requireNonNull(stageRouters.get( DvrpRoutingModuleProvider.Stage.MAIN ),
					"Main mode router must be explicitly bound");
			RoutingModule accessRouter = stageRouters.getOrDefault( DvrpRoutingModuleProvider.Stage.ACCESS, walkRouter );
			RoutingModule egressRouter = stageRouters.getOrDefault( DvrpRoutingModuleProvider.Stage.EGRESS, walkRouter );

			return new DrtEstimatingRoutingModule(mainRouter, accessRouter, egressRouter,
					getModalInstance( DrtEstimatingRoutingModule.AccessEgressFacilityFinder.class ), getMode(), timeInterpretation);
		}

	}

	private static class DrtRouteCreatorProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtRouteCreator> {
		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		private final DrtConfigGroup drtCfg;

		private DrtRouteCreatorProvider(DrtConfigGroup drtCfg) {
			super(drtCfg.getMode(), DvrpModes::mode);
			this.drtCfg = drtCfg;
			leastCostPathCalculatorFactory = new SpeedyALTFactory();
		}

		@Override
		public DrtRouteCreator get() {
			var travelTime = getModalInstance( TravelTime.class );
			return new DrtRouteCreator(drtCfg, getModalInstance(Network.class), leastCostPathCalculatorFactory,
					travelTime, getModalInstance( TravelDisutilityFactory.class ));
		}
	}

	private static class DrtStopNetworkProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtStopNetwork> {

		private final DrtConfigGroup drtCfg;
		private final Config config;

		private DrtStopNetworkProvider(Config config, DrtConfigGroup drtCfg) {
			super(drtCfg.getMode(), DvrpModes::mode);
			this.drtCfg = drtCfg;
			this.config = config;
		}

		@Override
		public DrtStopNetwork get() {
			switch (drtCfg.operationalScheme) {
				case door2door:
					return ImmutableMap::of;
				case stopbased:
					return createDrtStopNetworkFromTransitSchedule(config, drtCfg);
				case serviceAreaBased:
					return createDrtStopNetworkFromServiceArea(config, drtCfg, getModalInstance(Network.class));
				default:
					throw new RuntimeException("Unsupported operational scheme: " + drtCfg.operationalScheme);
			}
		}
	}

	private static DrtStopNetwork createDrtStopNetworkFromServiceArea(Config config, DrtConfigGroup drtCfg,
									  Network drtNetwork) {
		final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
				ConfigGroup.getInputFileURL(config.getContext(), drtCfg.drtServiceAreaShapeFile ) );
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = drtNetwork.getLinks()
											.values()
											.stream()
											.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(),
													preparedGeometries))
											.map( DrtStopFacilityImpl::createFromLink )
											.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}

	private static DrtStopNetwork createDrtStopNetworkFromTransitSchedule(Config config, DrtConfigGroup drtCfg) {
		URL url = ConfigGroup.getInputFileURL(config.getContext(), drtCfg.transitStopFile);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(url );
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = scenario.getTransitSchedule()
										      .getFacilities()
										      .values()
										      .stream()
										      .map(DrtStopFacilityImpl::createFromFacility)
										      .collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}


}
