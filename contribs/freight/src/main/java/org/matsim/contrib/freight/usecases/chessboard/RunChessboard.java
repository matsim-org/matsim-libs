package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.ReRouteVehicles;
import org.matsim.contrib.freight.controler.TimeAllocationMutator;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.VehicleEmploymentScoring;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

public final class RunChessboard {

    private Config config ;
    private Scenario scenario ;

    public static void main(String[] args){
        new RunChessboard().run();
    }

    public void run() {
        run(null,null) ;
    }

    public void run( Collection<AbstractModule> controlerModules, Collection<AbstractQSimModule> qsimModules ) {
        if ( scenario==null ) {
            prepareScenario() ;
        }

        Carriers carriers = FreightUtils.getOrCreateCarriers(scenario);
        CarrierVehicleTypes types = FreightUtils.getCarrierVehicleTypes(scenario);

        Controler controler = new Controler(scenario);

        if ( controlerModules!=null ){
            for( AbstractModule abstractModule : controlerModules ){
                controler.addOverridingModule( abstractModule ) ;
            }
        }
        if ( qsimModules!=null ) {
            for( AbstractQSimModule qsimModule : qsimModules ){
                controler.addOverridingQSimModule( qsimModule ) ;
            }
        }


        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new CarrierModule());
                bind(CarrierPlanStrategyManagerFactory.class).toInstance( new MyCarrierPlanStrategyManagerFactory(types) );
                bind(CarrierScoringFunctionFactory.class).toInstance( new MyCarrierScoringFunctionFactory() );
            }
        });
        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                final CarrierScoreStats scores = new CarrierScoreStats(carriers, config.controler().getOutputDirectory() +"/carrier_scores", true);
                final int statInterval = 1;
                final LegHistogram freightOnly = new LegHistogram(900);
                freightOnly.setInclPop(false);
                binder().requestInjection(freightOnly);
                final LegHistogram withoutFreight = new LegHistogram(900);
                binder().requestInjection(withoutFreight);

                addEventHandlerBinding().toInstance(withoutFreight);
                addEventHandlerBinding().toInstance(freightOnly);
                addControlerListenerBinding().toInstance(scores);
                addControlerListenerBinding().toInstance(new IterationEndsListener() {

                    @Inject
                    private OutputDirectoryHierarchy controlerIO;

                    @Override
                    public void notifyIterationEnds(IterationEndsEvent event) {
                        if (event.getIteration() % statInterval != 0) return;
                        //write plans
                        String dir = controlerIO.getIterationPath(event.getIteration());
                        new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

                        //write stats
                        freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
                        freightOnly.reset(event.getIteration());

                        withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
                        withoutFreight.reset(event.getIteration());
                    }
                });
            }
        });
        controler.run();

    }

    public final Scenario prepareScenario() {
        if ( config==null ) {
            prepareConfig() ;
        }
        scenario = ScenarioUtils.loadScenario( config ) ;
        FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
        return scenario ;
    }

    public final Config prepareConfig(){
        final URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
        final URL configURL = IOUtils.extendUrl(url, "config.xml");
        config = ConfigUtils.loadConfig(configURL  );
        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setCarriersFile("carrierPlans.xml");
        freightConfigGroup.setCarriersVehicleTypesFile("vehicleTypes.xml");
        config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
        config.global().setRandomSeed(4177);
        config.controler().setOutputDirectory("./output/");
        return config;
    }

    private static void createOutputDir(String outdir){
        File dir = new File(outdir);
        // if the directory does not exist, create it
        if (!dir.exists()){
            System.out.println("creating directory "+outdir);
            boolean result = dir.mkdirs();
            if(result) System.out.println(outdir+" created");
        }
    }


    private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {

        @Inject
        private Network network;

        @Override
        public ScoringFunction createScoringFunction(Carrier carrier) {
            SumScoringFunction sf = new SumScoringFunction();
            DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
            VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
            DriversActivityScoring actScoring = new DriversActivityScoring();
            sf.addScoringFunction(driverLegScoring);
            sf.addScoringFunction(vehicleEmploymentScoring);
            sf.addScoringFunction(actScoring);
            return sf;
        }

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
            {
                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger<CarrierPlan, Carrier>(1.));
                //						strategy.addStrategyModule(new ReRouter(router, services.getNetwork(), services.getLinkTravelTimes(), .1));
                strategyManager.addStrategy(strategy, null, 1.0);

            }
            //					{
            //						GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new ExpBetaPlanChanger<CarrierPlan, Carrier>(1.) ) ;
            //						strategy.addStrategyModule(new ReRouter(router, services.getNetwork(), services.getLinkTravelTimes(), 1.));
            //						strategyManager.addStrategy( strategy, null, 0.1) ;
            //					}
            {
                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<CarrierPlan, Carrier>());
                strategy.addStrategyModule(new TimeAllocationMutator());
                strategy.addStrategyModule(new ReRouteVehicles(router, network, modeTravelTimes.get(TransportMode.car), 1.));
                strategyManager.addStrategy(strategy, null, 0.5);
            }
            //					{
            //						GenericPlanStrategyImpl<CarrierPlan,Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan,Carrier>( new KeepSelected<CarrierPlan,Carrier>() ) ;
            //                        strategy.addStrategyModule(new ReScheduling(services.getNetwork(),types,services.getLinkTravelTimes(), "sschroeder/input/usecases/chessboard/vrpalgo/algorithm_v2.xml"));
            //                        strategy.addStrategyModule(new ReRouter(router, services.getNetwork(), services.getLinkTravelTimes(), 1.));
            //                        strategyManager.addStrategy( strategy, null, 0.1) ;
            //					}
            return strategyManager;
        }
    }
}
