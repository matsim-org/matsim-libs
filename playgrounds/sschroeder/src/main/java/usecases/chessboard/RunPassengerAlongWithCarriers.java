package usecases.chessboard;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierControlerListener;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

import usecases.analysis.CarrierScoreStats;
import usecases.analysis.LegHistogram;
import usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;

public class RunPassengerAlongWithCarriers {
	
	public static void main(String[] args) {
		
		createOutputDir();
		
		String configFile = "input/usecases/chessboard/passenger/config.xml" ;
		Config config = ConfigUtils.loadConfig(configFile);
		config.setQSimConfigGroup(new QSimConfigGroup());
		
		Controler controler = new Controler( config );
		
		final Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read("input/usecases/chessboard/freight/carrierPlans.xml");
		
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).read("input/usecases/chessboard/freight/vehicleTypes.xml");
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		
		CarrierPlanStrategyManagerFactory strategyManagerFactory = createStrategyManagerFactory(types);
		CarrierScoringFunctionFactory scoringFunctionFactory = createScoringFunctionFactory(carriers,controler.getNetwork());
		
		CarrierControlerListener carrierController = new CarrierControlerListener(carriers, strategyManagerFactory, scoringFunctionFactory);
		
		controler.addControlerListener(carrierController);
		prepareFreightOutputDataAndStats(controler, carriers);
		controler.setOverwriteFiles(true) ;
		
		controler.run() ;
		
	}

	private static void createOutputDir(){
		File dir = new File("output");
		// if the directory does not exist, create it
		if (!dir.exists()){
		    System.out.println("creating directory ./output");
		    boolean result = dir.mkdir();  
		    if(result) System.out.println("./output created");  
		}
	}
	
	private static void prepareFreightOutputDataAndStats(Controler controler, final Carriers carriers) {
		final LegHistogram freightOnly = new LegHistogram(900);
		freightOnly.setPopulation(controler.getPopulation());
		freightOnly.setInclPop(false);
		final LegHistogram withoutFreight = new LegHistogram(900);
		withoutFreight.setPopulation(controler.getPopulation());
		
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


	private static CarrierScoringFunctionFactory createScoringFunctionFactory(Carriers carriers, final Network network) {
		return new CarrierScoringFunctionFactory() {

			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				ScoringFunctionAccumulator sf = new ScoringFunctionAccumulator();
				DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
				DriversActivityScoring actScoring = new DriversActivityScoring();
				sf.addScoringFunction(driverLegScoring);
				sf.addScoringFunction(actScoring);
				return sf;
			}
			
		};
	}


	private static CarrierPlanStrategyManagerFactory createStrategyManagerFactory(final CarrierVehicleTypes types) {
		CarrierPlanStrategyManagerFactory stratManFactory = new CarrierPlanStrategyManagerFactory() {
			
			@Override
			public GenericStrategyManager<CarrierPlan> createStrategyManager(Controler controler) {
//				final CarrierReplanningStrategyManager strategyManager = new CarrierReplanningStrategyManager();
//				strategyManager.addStrategy(new SelectBestPlanStrategyFactory().createStrategy(), 0.95);
////				strategyManager.addStrategy(new KeepPlanSelectedStrategyFactory().createStrategy(), 0.8);
//				strategyManager.addStrategy(new SelectBestPlanAndOptimizeItsVehicleRouteFactory(controler.getNetwork(), types, controler.getLinkTravelTimes()).createStrategy(), 0.05);
//				return strategyManager;
				
				final GenericStrategyManager<CarrierPlan> strategyManager = new GenericStrategyManager<CarrierPlan>() ;
				{
					GenericPlanStrategyImpl<CarrierPlan> strategy = new GenericPlanStrategyImpl<CarrierPlan>( new BestPlanSelector<CarrierPlan>() ) ;
					strategyManager.addStrategy( strategy, null, 0.95 ) ;
				}
				{
					GenericPlanStrategy<CarrierPlan> strategy = 
							new SelectBestPlanAndOptimizeItsVehicleRouteFactory(controler.getNetwork(), types, controler.getLinkTravelTimes()).createStrategy() ;
					strategyManager.addStrategy( strategy, null, 0.05 ) ;
				}
				return strategyManager ;
			}
		};
		return stratManFactory;
	}

	}
