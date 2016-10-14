package playground.sebhoerl.avtaxi.framework;

import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.filter.capability.Operator;
import playground.sebhoerl.avtaxi.data.AVData;
import playground.sebhoerl.avtaxi.data.AVLoader;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVOperatorFactory;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcherFactory;
import playground.sebhoerl.avtaxi.dispatcher.SingleFIFODispatcher;
import playground.sebhoerl.avtaxi.dispatcher.SingleFIFODispatcherFactory;
import playground.sebhoerl.avtaxi.passenger.AVRequestCreator;
import playground.sebhoerl.avtaxi.routing.AVRoutingModule;
import playground.sebhoerl.avtaxi.schedule.AVOptimizer;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.sebhoerl.avtaxi.vrpagent.AVActionCreator;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

public class AVModule extends AbstractModule {
    final static public String AV_MODE = "av";

	@Override
	public void install() {
        addRoutingModuleBinding(AV_MODE).to(AVRoutingModule.class);
        bind(ScoringFunctionFactory.class).to(AVScoringFunctionFactory.class).asEagerSingleton();
        addControlerListenerBinding().to(AVLoader.class);

        // Bind the AV travel time to the DVRP estimated travel time
        bind(TravelTime.class).annotatedWith(Names.named(AVModule.AV_MODE))
                .to(Key.get(TravelTime.class, Names.named(VrpTravelTimeModules.DVRP_ESTIMATED)));

        bind(VehicleType.class).annotatedWith(Names.named(AVModule.AV_MODE)).toInstance(VehicleUtils.getDefaultVehicleType());

        bind(AVOperatorFactory.class);

        // Dispatchment strategies
        HashMap<String, Class<? extends AVDispatcher>> dispatcherStrategies = new HashMap<>();
        bind(new TypeLiteral<Map<String, Class<? extends AVDispatcher>>>() {}).toInstance(dispatcherStrategies);

        dispatcherStrategies.put("SingleFIFO", SingleFIFODispatcher.class);
        bind(SingleFIFODispatcher.class);
	}

	@Provides @Named(AVModule.AV_MODE)
    LeastCostPathCalculator provideLeastCostPathCalculator(Network network, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime) {
        return new Dijkstra(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }

	@Provides @Singleton
    Map<Id<AVOperator>, AVOperator> provideOperators(AVOperatorFactory factory) {
        System.err.println(factory.toString());
        Map<Id<AVOperator>, AVOperator> operators = new HashMap<>();

        operators.put(Id.create("op1", AVOperator.class), factory.createOperator("op1", "SingleFIFO"));

        return operators;
    }

    @Provides @Singleton
    public AVData provideData() {
        return new AVData();
    }
}
