package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.VehicleEmploymentScoring;
import org.matsim.contrib.freight.usecases.chessboard.replanning.ReRouter;
import org.matsim.contrib.freight.usecases.chessboard.replanning.TimeAllocationMutator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

import java.io.File;

public class RunChessboard {

	public static void main(String[] args) {

        int nuRuns = 2;
        long lastSeed = 4711L;

        for(int i=0;i<nuRuns;i++) {
            String outputDir = "sschroeder/output/" + i + ".run/";
            createOutputDir(outputDir);

            String configFile = "sschroeder/input/usecases/chessboard/passenger/config.xml";
            Config config = ConfigUtils.loadConfig(configFile);
//            config.setQSimConfigGroup(new QSimConfigGroup());
            config.global().setRandomSeed(lastSeed);

            config.controler().setOutputDirectory(outputDir);
            Controler controler = new Controler(config);


            final Carriers carriers = new Carriers();
            new CarrierPlanXmlReaderV2(carriers).read("sschroeder/input/usecases/chessboard/freight/carrierPlans_1_2hTW.xml");

            CarrierVehicleTypes types = new CarrierVehicleTypes();
            new CarrierVehicleTypeReader(types).read("sschroeder/input/usecases/chessboard/freight/vehicleTypes.xml");
            new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

            CarrierPlanStrategyManagerFactory strategyManagerFactory = createStrategyManagerFactory(types, controler);
            CarrierScoringFunctionFactory scoringFunctionFactory = createScoringFunctionFactory(controler.getScenario().getNetwork());

            CarrierModule carrierController = new CarrierModule(carriers, strategyManagerFactory, scoringFunctionFactory);
            carrierController.setPhysicallyEnforceTimeWindowBeginnings(true);

            controler.addOverridingModule(carrierController);
            prepareFreightOutputDataAndStats(controler, carriers, outputDir);
            controler.setOverwriteFiles(true);

            controler.run();

            lastSeed = lastSeed + 23L;
        }

	}

    private static void createOutputDir(String outdir){
        File dir = new File(outdir);
        // if the directory does not exist, create it
        if (!dir.exists()){
            System.out.println("creating directory "+outdir);
            boolean result = dir.mkdir();
            if(result) System.out.println(outdir+" created");
        }
    }

    private static void prepareFreightOutputDataAndStats(Controler controler, final Carriers carriers, String outputDir) {
        final int statInterval = 1;
        final LegHistogram freightOnly = new LegHistogram(900);
        freightOnly.setPopulation(controler.getScenario().getPopulation());
        freightOnly.setInclPop(false);
        final LegHistogram withoutFreight = new LegHistogram(900);
        withoutFreight.setPopulation(controler.getScenario().getPopulation());

        CarrierScoreStats scores = new CarrierScoreStats(carriers, outputDir+"/carrier_scores", true);

		controler.getEvents().addHandler(withoutFreight);
		controler.getEvents().addHandler(freightOnly);
        controler.addControlerListener(scores);
		controler.addControlerListener(new IterationEndsListener() {

			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				if(event.getIteration() % statInterval != 0) return;
				//write plans
				String dir = event.getControler().getControlerIO().getIterationPath(event.getIteration());
				new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

//				//write stats
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
                VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
                DriversActivityScoring actScoring = new DriversActivityScoring();
                sf.addScoringFunction(driverLegScoring);
                sf.addScoringFunction(vehicleEmploymentScoring);
                sf.addScoringFunction(actScoring);
                return sf;
            }

        };
    }


	 private static CarrierPlanStrategyManagerFactory createStrategyManagerFactory(final CarrierVehicleTypes types, final Controler controler) {
	        return new CarrierPlanStrategyManagerFactory() {

	            @Override
	            public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {

	            	TravelDisutility travelDisutility = TravelDisutilities.createBaseDisutility(types, controler.getLinkTravelTimes());
					final LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(controler.getScenario().getNetwork(),
							travelDisutility, controler.getLinkTravelTimes()) ;

					final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<CarrierPlan, Carrier>() ;
					strategyManager.setMaxPlansPerAgent(5);
					{
						GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new ExpBetaPlanChanger<CarrierPlan, Carrier>(1.) ) ;
//						strategy.addStrategyModule(new ReRouter(router, controler.getNetwork(), controler.getLinkTravelTimes(), .1));
						strategyManager.addStrategy( strategy, null, 1.0 ) ;

					}
//					{
//						GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new ExpBetaPlanChanger<CarrierPlan, Carrier>(1.) ) ;
//						strategy.addStrategyModule(new ReRouter(router, controler.getNetwork(), controler.getLinkTravelTimes(), 1.));
//						strategyManager.addStrategy( strategy, null, 0.1) ;
//					}
					{
						GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new KeepSelected<CarrierPlan, Carrier>() ) ;
						strategy.addStrategyModule(new TimeAllocationMutator(1.));
						strategy.addStrategyModule(new ReRouter(router, controler.getScenario().getNetwork(), controler.getLinkTravelTimes(), 1.));
						strategyManager.addStrategy( strategy, null, 0.5) ;
					}
//					{
//						GenericPlanStrategyImpl<CarrierPlan,Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan,Carrier>( new KeepSelected<CarrierPlan,Carrier>() ) ;
//                        strategy.addStrategyModule(new ReScheduling(controler.getNetwork(),types,controler.getLinkTravelTimes(), "sschroeder/input/usecases/chessboard/vrpalgo/algorithm_v2.xml"));
//                        strategy.addStrategyModule(new ReRouter(router, controler.getNetwork(), controler.getLinkTravelTimes(), 1.));
//                        strategyManager.addStrategy( strategy, null, 0.1) ;
//					}
                    return strategyManager ;
	            }
	        };
	    }



	}
