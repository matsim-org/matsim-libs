package playground.sebhoerl.avtaxi.framework;

import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.filter.capability.Operator;
import playground.sebhoerl.avtaxi.config.*;
import playground.sebhoerl.avtaxi.data.*;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.SingleFIFODispatcher;
import playground.sebhoerl.avtaxi.generator.AVGenerator;
import playground.sebhoerl.avtaxi.generator.PopulationDensityGenerator;
import playground.sebhoerl.avtaxi.passenger.AVRequestCreator;
import playground.sebhoerl.avtaxi.replanning.AVOperatorChoiceStrategy;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.routing.AVRouteFactory;
import playground.sebhoerl.avtaxi.routing.AVRoutingModule;
import playground.sebhoerl.avtaxi.schedule.AVOptimizer;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.sebhoerl.avtaxi.vrpagent.AVActionCreator;

import javax.inject.Named;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AVModule extends AbstractModule {
    final static public String AV_MODE = "av";
    final static Logger log = Logger.getLogger(AVModule.class);

	@Override
	public void install() {
        addRoutingModuleBinding(AV_MODE).to(AVRoutingModule.class);
        bind(ScoringFunctionFactory.class).to(AVScoringFunctionFactory.class).asEagerSingleton();
        addControlerListenerBinding().to(AVLoader.class);

        bind(AVOperatorChoiceStrategy.class);
        addPlanStrategyBinding("AVOperatorChoice").to(AVOperatorChoiceStrategy.class);

        // Bind the AV travel time to the DVRP estimated travel time
        bind(TravelTime.class).annotatedWith(Names.named(AVModule.AV_MODE))
                .to(Key.get(TravelTime.class, Names.named(VrpTravelTimeModules.DVRP_ESTIMATED)));

        bind(VehicleType.class).annotatedWith(Names.named(AVModule.AV_MODE)).toInstance(VehicleUtils.getDefaultVehicleType());

        bind(AVOperatorFactory.class);
        bind(AVRouteFactory.class);

        configureDispatchmentStrategies();
        configureGeneratorStrategies();
	}

	private void configureDispatchmentStrategies() {
        HashMap<String, Class<? extends AVDispatcher.AVDispatcherFactory>> dispatcherStrategies = new HashMap<>();
        bind(new TypeLiteral<Map<String, Class<? extends AVDispatcher.AVDispatcherFactory>>>() {}).toInstance(dispatcherStrategies);

        dispatcherStrategies.put("SingleFIFO", SingleFIFODispatcher.Factory.class);
        bind(SingleFIFODispatcher.Factory.class);
    }

    private void configureGeneratorStrategies() {
        HashMap<String, Class<? extends AVGenerator.AVGeneratorFactory>> generatorStrategies = new HashMap<>();
        bind(new TypeLiteral<Map<String, Class<? extends AVGenerator.AVGeneratorFactory>>>() {}).toInstance(generatorStrategies);

        generatorStrategies.put("PopulationDensity", PopulationDensityGenerator.Factory.class);
        bind(PopulationDensityGenerator.Factory.class);
    }

    @Provides
    RouteFactories provideRouteFactories(AVRouteFactory routeFactory) {
        RouteFactories factories = new RouteFactories();
        factories.setRouteFactory(AVRoute.class, routeFactory);
        return factories;
    }

	@Provides @Named(AVModule.AV_MODE)
    LeastCostPathCalculator provideLeastCostPathCalculator(Network network, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime) {
        return new Dijkstra(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }

	@Provides @Singleton
    Map<Id<AVOperator>, AVOperator> provideOperators(AVConfig config, AVOperatorFactory factory, Map<Id<AVOperator>, AVDispatcher> dispatchers) {
        Map<Id<AVOperator>, AVOperator> operators = new HashMap<>();

        for (AVOperatorConfig oc : config.getOperatorConfigs()) {
            AVDispatcher dispatcher = dispatchers.get(oc.getId());
            operators.put(oc.getId(), factory.createOperator(oc.getId(), oc, dispatcher));
        }

        return operators;
    }

    @Provides @Singleton
    AVConfig provideAVConfig(Config config, AVConfigGroup configGroup) {
        File basePath = new File(config.getContext().getPath()).getParentFile();
        File configPath = new File(basePath, configGroup.getConfigPath());

        AVConfig avConfig = new AVConfig();
        AVConfigReader reader = new AVConfigReader(avConfig);

        reader.readFile(configPath.getAbsolutePath());
        return avConfig;
    }

    @Provides @Singleton
    Map<Id<AVOperator>, AVGenerator> provideGenerators(Map<String, Class<? extends AVGenerator.AVGeneratorFactory>> factories, AVConfig config, Injector injector) {
        Map<Id<AVOperator>, AVGenerator> generators = new HashMap<>();

        for (AVOperatorConfig oc : config.getOperatorConfigs()) {
            AVGeneratorConfig gc = oc.getGeneratorConfig();
            String strategy = gc.getStrategyName();

            if (!factories.containsKey(strategy)) {
                throw new IllegalArgumentException("Generator strategy '" + strategy + "' is not registered.");
            }

            AVGenerator.AVGeneratorFactory factory = injector.getInstance(factories.get(strategy));
            AVGenerator generator = factory.createGenerator(gc);

            generators.put(oc.getId(), generator);
        }

        return generators;
    }

    @Provides @Singleton
    Map<Id<AVOperator>, AVDispatcher> provideDispatchers(Map<String, Class<? extends AVDispatcher.AVDispatcherFactory>> factories, AVConfig config, Injector injector) {
        Map<Id<AVOperator>, AVDispatcher> dispatchers = new HashMap<>();

        for (AVOperatorConfig oc : config.getOperatorConfigs()) {
            AVDispatcherConfig dc = oc.getDispatcherConfig();
            String strategy = dc.getStrategyName();

            if (!factories.containsKey(strategy)) {
                throw new IllegalArgumentException("Dispatcher strategy '" + strategy + "' is not registered.");
            }

            AVDispatcher.AVDispatcherFactory factory = injector.getInstance(factories.get(strategy));
            AVDispatcher dispatcher = factory.createDispatcher(dc);

            dispatchers.put(oc.getId(), dispatcher);
        }

        return dispatchers;
    }

    @Provides @Singleton
    public AVData provideData(Map<Id<AVOperator>, AVOperator> operators, Map<Id<AVOperator>, AVGenerator> generators) {
        AVData data = new AVData();

        for (AVOperator operator : operators.values()) {
            AVGenerator generator = generators.get(operator.getId());

            while (generator.hasNext()) {
                AVVehicle vehicle = generator.next();

                vehicle.setOpeartor(operator);
                operator.getDispatcher().addVehicle(vehicle);
                data.addVehicle(vehicle);
            }
        }

        return data;
    }
}
