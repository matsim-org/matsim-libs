package playground.tschlenther.CarSharing.Cottbus;

import java.util.Arrays;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;

public class CarSharingConfigCreator {
	
	private static final String VEHICLELOCTAIONSINPUTFILE= "C:/Users/Tille/WORK/CarSharing/cottbus/input/VehicleLocations.txt";
	private static final String NETWORKFILE = "C:/Users/Tille/WORK/CarSharing/cottbus/input/network.xml";
	private static final String POPULATIONINPUT =  "C:/Users/Tille/WORK/CarSharing/cottbus/input/plansWithCarSharing_0015.xml";
	
	public Config createConfig(){
		Config config = ConfigUtils.createConfig();
//		CarsharingUtils.addConfigModules(config);
		
		config.network().setInputFile(NETWORKFILE);		
		config.plans().setInputFile(POPULATIONINPUT);
		
		String[] modes = new String[5];
		modes[0] = "onewaycarsharing";
		modes[1] = "twowaycarsharing";
		modes[2] = "freefloating";
		modes[3] = "car";
//		modes[4] = "pt";
		
		config.qsim().setMainModes(Arrays.asList(modes));
		config.qsim().setFlowCapFactor(0.015);
		config.qsim().setStorageCapFactor(0.01);
		
		PlanCalcScoreConfigGroup planCalcScore = config.planCalcScore();
		planCalcScore.getModes().get(TransportMode.other).setConstant((double) 0);

		ActivityParams home = new ActivityParams();
		home.setActivityType("home");
		home.setTypicalDuration(8*3600);
		planCalcScore.addActivityParams(home);
		
		ActivityParams work = new ActivityParams();
		work.setActivityType("work");
		work.setTypicalDuration(8*3600);
		work.setOpeningTime(7*3600);
		work.setClosingTime(20*3600);
		work.setLatestStartTime(11*3600);
		planCalcScore.addActivityParams(work);
		
		ActivityParams kg1 = new ActivityParams();
		kg1.setActivityType("kindergarten1");
		kg1.setTypicalDuration(5*60);
		kg1.setOpeningTime(7*3600);
		kg1.setClosingTime(9*3600);
		planCalcScore.addActivityParams(kg1);

		ActivityParams kg2 = new ActivityParams();
		kg2.setActivityType("kindergarten2");
		kg2.setTypicalDuration(5*60);
		kg2.setOpeningTime(13*3600);
		kg2.setClosingTime(18*3600);
		planCalcScore.addActivityParams(kg2);

		ActivityParams shopping = new ActivityParams();
		shopping.setActivityType("shopping");
		shopping.setTypicalDuration(3600);
		shopping.setOpeningTime(10*3600);
		shopping.setClosingTime(21*3600);
		planCalcScore.addActivityParams(shopping);
		
    	/*
    	 * CARSHARING-PRICES ACCORDING TO https://de.drive-now.com/#!/tarife
    	 */
    	
		
		OneWayCarsharingConfigGroup owconfigGroup = new OneWayCarsharingConfigGroup();
//		owconfigGroup.setsearchDistance("500000");
//		owconfigGroup.setvehiclelocations(VEHICLELOCTAIONSINPUTFILE);
//		owconfigGroup.setUseOneWayCarsharing(true);
//		owconfigGroup.setConstantOneWayCarsharing(""+ planCalcScore.getConstantCar());
//		owconfigGroup.setUtilityOfTravelling("" + planCalcScore.getTravelingOther_utils_hr());
//		owconfigGroup.setTimeFeeOneWayCarsharing("" + planCalcScore.getTraveling_utils_hr());
//		owconfigGroup.setDistanceFeeOneWayCarsharing("" + planCalcScore.getMonetaryDistanceCostRateCar());
//		owconfigGroup.setRentalPriceTimeOneWayCarsharing("-6.0");
//		owconfigGroup.setTimeParkingFeeOneWayCarsharing("-4.0");
    	config.addModule(owconfigGroup);
    	
    	FreeFloatingConfigGroup ffconfigGroup = new FreeFloatingConfigGroup();
    	ffconfigGroup.setConstantFreeFloating("0");
    	ffconfigGroup.setDistanceFeeFreeFloating("0");
    	ffconfigGroup.setTimeFeeFreeFloating("-0.0052");
    	ffconfigGroup.setTimeParkingFeeFreeFloating("-0.0025");
    	ffconfigGroup.setUseFeeFreeFloating(true);
		ffconfigGroup.setUtilityOfTravelling(""+ planCalcScore.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
    	ffconfigGroup.setvehiclelocations(VEHICLELOCTAIONSINPUTFILE);
    	ffconfigGroup.setSpecialTimeStart("0");
    	ffconfigGroup.setSpecialTimeEnd("0");
    	ffconfigGroup.setSpecialTimeFee("0");
    	config.addModule(ffconfigGroup);
    	
    	TwoWayCarsharingConfigGroup twconfigGroup = new TwoWayCarsharingConfigGroup();
    	twconfigGroup.setvehiclelocations(VEHICLELOCTAIONSINPUTFILE);
    	twconfigGroup.setConstantTwoWayCarsharing("0");
    	twconfigGroup.setUseTwoWayCarsharing(true);
    	twconfigGroup.setsearchDistance("50000");
    	twconfigGroup.setDistanceFeeTwoWayCarsharing("0");
    	twconfigGroup.setTimeFeeTwoWayCarsharing("-0.0052");
		twconfigGroup.setUtilityOfTravelling("" + planCalcScore.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
    	twconfigGroup.setRentalPriceTimeTwoWayCarsharing("-0.0052");
    	config.addModule(twconfigGroup);
    	
    	CarsharingConfigGroup configGroupAll = new CarsharingConfigGroup();
    	configGroupAll.setStatsWriterFrequency("1");
    	config.addModule(configGroupAll);
    	
		ControlerConfigGroup controler = config.controler();
		controler.setLastIteration(10);
		controler.setOutputDirectory("C:/Users/Tille/WORK/CarSharing/cottbus/output");
		controler.setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultStrategy.ReRoute.toString() );
			strat.setWeight( 0.1 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
			strat.setWeight( 0.9 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultStrategy.SubtourModeChoice.toString() );
			strat.setWeight( 0.1 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		
		String[] chainModes = new String[1];
		chainModes[0] = "car";
		config.subtourModeChoice().setChainBasedModes(chainModes);
		
		String[] subtourModes = new String[3];
		subtourModes[0] = "twowaycarsharing";
		subtourModes[1] = "freefloating";
		subtourModes[2] = "pt";
		config.subtourModeChoice().setModes(subtourModes);
		config.subtourModeChoice().setConsiderCarAvailability(true);

		
		return config;
	}
	
	
}
