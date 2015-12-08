package playground.wrashid.ABMT.vehicleShare;

import org.matsim.core.config.Config;

public class GlobalTESFParameters {

	// [start] parameters for multi year scenario
	public static boolean isSingYearScenario=false;
	public static int currentYear;
	public static int tesfSeed=2321;
	// [end] parameters for multi year scenario
	
	
	public static double tollAreaRadius;
	public static double tollPriceCV;
	public static double tollPriceEV;
	public static double morningTollStart;
	public static double morningTollEnd;	
	public static double eveningTollStart;
	public static double eveningTollEnd;

	public static double evDailyRange;
	
	public static double evDrivingCostPerMeter;
	public static double evTaxationPerMeter;

	public static double evFixedCostPerDay;
	public static double evTaxationPerDay;
	
	public static double cvDrivingCostPerMeter;
	public static double cvTaxationPerMeter;
	
	public static double cvFixedCostPerDay;
	public static double cvTaxationPerDay;
	
	public static double weightParameterPerDay;
	public static double weightParameterPerMeter;
	
	
	public static void init(Config config){
		
		tollAreaRadius=Double.parseDouble(config.getParam("TESF.main", "tollAreaRadius"));
		tollPriceCV=Double.parseDouble(config.getParam("TESF.main", "tollPriceCV"));
		tollPriceEV=Double.parseDouble(config.getParam("TESF.main", "tollPriceEV"));
		morningTollStart=Double.parseDouble(config.getParam("TESF.main", "morningTollStart"));		
		morningTollEnd=Double.parseDouble(config.getParam("TESF.main", "morningTollEnd"));		
		eveningTollStart=Double.parseDouble(config.getParam("TESF.main", "eveningTollStart"));		
		eveningTollEnd=Double.parseDouble(config.getParam("TESF.main", "eveningTollEnd"));
		
		evDailyRange=Double.parseDouble(config.getParam("TESF.main", "evDailyRange"));
		
		evDrivingCostPerMeter=Double.parseDouble(config.getParam("TESF.EVcosts", "drivingCostPerMeter"));		
		evTaxationPerMeter=Double.parseDouble(config.getParam("TESF.EVcosts", "taxationPerMeter"));		
		
		evFixedCostPerDay=Double.parseDouble(config.getParam("TESF.EVcosts", "fixedCostPerDay"));		
		evTaxationPerDay=Double.parseDouble(config.getParam("TESF.EVcosts", "taxationPerDay"));		
		
		cvDrivingCostPerMeter=Double.parseDouble(config.getParam("TESF.CVcosts", "drivingCostPerMeter"));
		cvTaxationPerMeter=Double.parseDouble(config.getParam("TESF.CVcosts", "taxationPerMeter"));

		cvFixedCostPerDay=Double.parseDouble(config.getParam("TESF.CVcosts", "fixedCostPerDay"));		
		cvTaxationPerDay=Double.parseDouble(config.getParam("TESF.CVcosts", "taxationPerDay"));
		
		weightParameterPerDay=Double.parseDouble(config.getParam("TESF.main", "weightPerDay"));
		weightParameterPerMeter=Double.parseDouble(config.getParam("TESF.main", "weightPerMeter"));
		
	}	
	
}
