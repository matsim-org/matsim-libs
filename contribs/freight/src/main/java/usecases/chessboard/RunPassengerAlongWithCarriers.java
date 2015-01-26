package usecases.chessboard;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierControlerListener;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import usecases.analysis.CarrierScoreStats;
import usecases.analysis.LegHistogram;
import usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;

import java.io.File;

public class RunPassengerAlongWithCarriers {

    public static void main(String[] args) {

        createOutputDir();

        String configFile = "input/usecases/chessboard/passenger/config.xml";
        Config config = ConfigUtils.loadConfig(configFile);

        Controler controler = new Controler(config);
        controler.setOverwriteFiles(true);
        final Carriers carriers = new Carriers();
        new CarrierPlanXmlReaderV2(carriers).read("input/usecases/chessboard/freight/carrierPlans.xml");

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(types).read("input/usecases/chessboard/freight/vehicleTypes.xml");
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

        CarrierPlanStrategyManagerFactory strategyManagerFactory = createStrategyManagerFactory(types, controler);
        CarrierScoringFunctionFactory scoringFunctionFactory = createScoringFunctionFactory(controler.getScenario().getNetwork());

        CarrierControlerListener carrierController = new CarrierControlerListener(carriers, strategyManagerFactory, scoringFunctionFactory);

        controler.addControlerListener(carrierController);
        prepareFreightOutputDataAndStats(controler, carriers);

        controler.run();

    }

    private static void createOutputDir() {
        File dir = new File("output");
        // if the directory does not exist, create it
        if (!dir.exists()) {
            System.out.println("creating directory ./output");
            boolean result = dir.mkdir();
            if (result) System.out.println("./output created");
        }
    }

    private static void prepareFreightOutputDataAndStats(Controler controler, final Carriers carriers) {
        final LegHistogram freightOnly = new LegHistogram(900);
        freightOnly.setPopulation(controler.getScenario().getPopulation());
        freightOnly.setInclPop(false);
        final LegHistogram withoutFreight = new LegHistogram(900);
        withoutFreight.setPopulation(controler.getScenario().getPopulation());

        CarrierScoreStats scores = new CarrierScoreStats(carriers, "output/carrier_scores", true);

        controler.getEvents().addHandler(withoutFreight);
        controler.getEvents().addHandler(freightOnly);
        controler.addControlerListener(scores);
        controler.addControlerListener(new IterationEndsListener() {

            @Override
            public void notifyIterationEnds(IterationEndsEvent event) {
                //write plans
                String dir = event.getControler().getControlerIO().getIterationPath(event.getIteration());
                new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

                //write stats
                freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
                freightOnly.reset(event.getIteration());

                withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
                withoutFreight.reset(event.getIteration());
            }
        });
    }


    private static CarrierScoringFunctionFactory createScoringFunctionFactory(final Network network) {
        return new CarrierScoringFunctionFactory() {

            @Override
            public ScoringFunction createScoringFunction(Carrier carrier) {
                SumScoringFunction sf = new SumScoringFunction();
                DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
                DriversActivityScoring actScoring = new DriversActivityScoring();
                sf.addScoringFunction(driverLegScoring);
                sf.addScoringFunction(actScoring);
                return sf;
            }

        };
    }


    private static CarrierPlanStrategyManagerFactory createStrategyManagerFactory(final CarrierVehicleTypes types, final Controler controler) {
        return new CarrierPlanStrategyManagerFactory() {

            @Override
            public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {

                final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<CarrierPlan, Carrier>();
                {
                    GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>(new BestPlanSelector<CarrierPlan, Carrier>());
                    strategyManager.addStrategy(strategy, null, 0.95);
                }
                {
                    GenericPlanStrategy<CarrierPlan, Carrier> strategy =
                            new SelectBestPlanAndOptimizeItsVehicleRouteFactory(controler.getScenario().getNetwork(), types, controler.getLinkTravelTimes()).createStrategy();
                    strategyManager.addStrategy(strategy, null, 0.05);
                }
                return strategyManager;
            }
        };
    }

}
