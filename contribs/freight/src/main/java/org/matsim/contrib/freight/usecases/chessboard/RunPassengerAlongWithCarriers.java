package org.matsim.contrib.freight.usecases.chessboard;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.modules.TimeAllocationMutator;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.VehicleEmploymentScoring;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

final class RunPassengerAlongWithCarriers {

	final static URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
	
	private Config config ;
	private Scenario scenario ;

	public static void main(String[] args) {
		new RunPassengerAlongWithCarriers().run();
	}

	public void run() {
		run(null,null) ;
	}

	public void run( Collection<AbstractModule> controlerModules, Collection<AbstractQSimModule> qsimModules ) {
		if ( scenario==null ) {
			prepareScenario() ;
		}

		Controler controler = new Controler(scenario);
		final Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers).readURL( IOUtils.extendUrl(url, "carrierPlans.xml" ) );

		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readURL( IOUtils.extendUrl(url, "vehicleTypes.xml" ) );
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

		CarrierPlanStrategyManagerFactory strategyManagerFactory = new MyCarrierPlanStrategyManagerFactory(types);
		CarrierScoringFunctionFactory scoringFunctionFactory = createScoringFunctionFactory(scenario.getNetwork());

		CarrierModule carrierController = new CarrierModule(carriers, strategyManagerFactory, scoringFunctionFactory);

		controler.addOverridingModule(carrierController);
		prepareFreightOutputDataAndStats(scenario, controler.getEvents(), controler, carriers);

		controler.run();
	}


	public final Config prepareConfig() {
		config = ConfigUtils.loadConfig(IOUtils.extendUrl(url, "config.xml"));
        config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
        config.global().setRandomSeed(4177);
        config.controler().setOutputDirectory("./output/");
		return config;
	}

	public final Scenario prepareScenario() {
		if ( config==null ) {
			prepareConfig() ;
		}
		scenario = ScenarioUtils.loadScenario( config ) ;
		return scenario ;
	}


	private static void prepareFreightOutputDataAndStats(Scenario scenario, EventsManager eventsManager, MatsimServices controler, final Carriers carriers) {
		final LegHistogram freightOnly = new LegHistogram(900);
		freightOnly.setPopulation(scenario.getPopulation());
		freightOnly.setInclPop(false);
		final LegHistogram withoutFreight = new LegHistogram(900);
		withoutFreight.setPopulation(scenario.getPopulation());

		CarrierScoreStats scores = new CarrierScoreStats(carriers, "output/carrier_scores", true);

		eventsManager.addHandler(withoutFreight);
		eventsManager.addHandler(freightOnly);
		controler.addControlerListener(scores);
		controler.addControlerListener((IterationEndsListener) event -> {
			//write plans
			String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
			new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

			//write stats
			freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
			freightOnly.reset(event.getIteration());

			withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
			withoutFreight.reset(event.getIteration());
		});
	}


	private static CarrierScoringFunctionFactory createScoringFunctionFactory(final Network network) {
		return carrier -> {
			SumScoringFunction sf = new SumScoringFunction();
			DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
			VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
			DriversActivityScoring actScoring = new DriversActivityScoring();
			sf.addScoringFunction(driverLegScoring);
			sf.addScoringFunction(vehicleEmploymentScoring);
			sf.addScoringFunction(actScoring);
			return sf;
		};
	}
	
	private static class MyCarrierPlanStrategyManagerFactory implements CarrierPlanStrategyManagerFactory {

        @Inject
        private Network network;

        @Inject
        private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

        @Inject
        private Map<String, TravelTime> modeTravelTimes;

        private final CarrierVehicleTypes types;

        public MyCarrierPlanStrategyManagerFactory(CarrierVehicleTypes types) {
            this.types = types;
        }

        @Override
        public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
            TravelDisutility travelDisutility = TravelDisutilities.createBaseDisutility(types, modeTravelTimes.get(TransportMode.car));
            final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network,
                    travelDisutility, modeTravelTimes.get(TransportMode.car));

            final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<>();
            strategyManager.setMaxPlansPerAgent(5);

            strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 0.95);
            {
            	GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<CarrierPlan, Carrier>());
                strategy.addStrategyModule(new TimeAllocationMutator());
                strategy.addStrategyModule(new ReRouteVehicles(router, network, modeTravelTimes.get(TransportMode.car), 1.));
                strategyManager.addStrategy(strategy, null, 0.5);
            }
            
//            strategyManager.addStrategy(new SelectBestPlanAndOptimizeItsVehicleRouteFactory(network, types, modeTravelTimes.get(TransportMode.car)).createStrategy(), null, 0.05);
            
            return strategyManager;
        }
    }

}
