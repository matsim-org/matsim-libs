package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.SumScoringFunction;

public class RunPassengerAlongWithCarriers {

    public static void main(String[] args) {


        String configFile = "input/usecases/chessboard/passenger/config.xml";
        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(config);
        final Carriers carriers = new Carriers();
        new CarrierPlanXmlReaderV2(carriers).readFile("input/usecases/chessboard/freight/carrierPlans.xml");

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(types).readFile("input/usecases/chessboard/freight/vehicleTypes.xml");
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

        CarrierPlanStrategyManagerFactory strategyManagerFactory = createStrategyManagerFactory(types, controler);
        CarrierScoringFunctionFactory scoringFunctionFactory = createScoringFunctionFactory(scenario.getNetwork());

        CarrierModule carrierController = new CarrierModule(carriers, strategyManagerFactory, scoringFunctionFactory);

        controler.addOverridingModule(carrierController);
        prepareFreightOutputDataAndStats(scenario, controler.getEvents(), controler, carriers);

        controler.run();

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
            DriversActivityScoring actScoring = new DriversActivityScoring();
            sf.addScoringFunction(driverLegScoring);
            sf.addScoringFunction(actScoring);
            return sf;
        };
    }


    private static CarrierPlanStrategyManagerFactory createStrategyManagerFactory(final CarrierVehicleTypes types, final MatsimServices controler) {
        return () -> {
            final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<>();
            strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 0.95);
            strategyManager.addStrategy(new SelectBestPlanAndOptimizeItsVehicleRouteFactory(controler.getScenario().getNetwork(), types, controler.getLinkTravelTimes()).createStrategy(), null, 0.05);
            return strategyManager;
        };
    }

}
