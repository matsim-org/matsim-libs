package playground.wrashid.ABMT.vehicleShare;

import java.io.PrintWriter;

import org.matsim.core.config.Config;

public class GlobalTESFParameters {

	public static double tollAreaRadius;
	public static double tollPriceCV;
	public static double tollPriceEV;
	public static double morningTollStart;
	public static double morningTollEnd;	
	public static double eveningTollStart;
	public static double eveningTollEnd;
	
	public static double evMaintainanceCostPerMeter;
	public static double evDrivingCostPerMeter;
	public static double evTaxationPerMeter;
	public static double evSubsidyPerMeter;

	public static double evPurchaseCostPerDay;
	public static double evInsuranceCostPerDay;
	public static double evBatteryCostPerDay;
	public static double evTariffCostPerDay;	
	
	public static double cvMaintainanceCostPerMeter;
	public static double cvDrivingCostPerMeter;
	public static double cvTaxationPerMeter;
	public static double cvSubsidyPerMeter;
	
	public static double cvPurchaseCostPerDay;
	public static double cvInsuranceCostPerDay;
	public static double cvTariffCostPerDay;	
	
	
	public static void init(Config config){
		
		tollAreaRadius=Double.parseDouble(config.getParam("TESF.main", "tollAreaRadius"));
		tollPriceCV=Double.parseDouble(config.getParam("TESF.main", "tollPriceCV"));
		tollPriceEV=Double.parseDouble(config.getParam("TESF.main", "tollPriceEV"));
		morningTollStart=Double.parseDouble(config.getParam("TESF.main", "morningTollStart"));		
		morningTollEnd=Double.parseDouble(config.getParam("TESF.main", "morningTollEnd"));		
		eveningTollStart=Double.parseDouble(config.getParam("TESF.main", "eveningTollStart"));		
		eveningTollEnd=Double.parseDouble(config.getParam("TESF.main", "eveningTollEnd"));
		
		evMaintainanceCostPerMeter=Double.parseDouble(config.getParam("TESF.EVcosts", "maintainanceCostPerMeter"));
		evDrivingCostPerMeter=Double.parseDouble(config.getParam("TESF.EVcosts", "drivingCostPerMeter"));		
		evTaxationPerMeter=Double.parseDouble(config.getParam("TESF.EVcosts", "taxationPerMeter"));		
		evSubsidyPerMeter=Double.parseDouble(config.getParam("TESF.EVcosts", "subsidyPerMeter"));
		
		evPurchaseCostPerDay=Double.parseDouble(config.getParam("TESF.EVcosts", "purchaseCostPerDay"));		
		evInsuranceCostPerDay=Double.parseDouble(config.getParam("TESF.EVcosts", "insuranceCostPerDay"));		
		evBatteryCostPerDay=Double.parseDouble(config.getParam("TESF.EVcosts", "batteryCostPerDay"));		
		evTariffCostPerDay=Double.parseDouble(config.getParam("TESF.EVcosts", "tariffCostPerDay"));		
		
		cvMaintainanceCostPerMeter=Double.parseDouble(config.getParam("TESF.CVcosts", "maintainanceCostPerMeter"));
		cvDrivingCostPerMeter=Double.parseDouble(config.getParam("TESF.CVcosts", "drivingCostPerMeter"));
		cvTaxationPerMeter=Double.parseDouble(config.getParam("TESF.CVcosts", "taxationPerMeter"));
		cvSubsidyPerMeter=Double.parseDouble(config.getParam("TESF.CVcosts", "subsidyPerMeter"));

		cvPurchaseCostPerDay=Double.parseDouble(config.getParam("TESF.CVcosts", "purchaseCostPerDay"));		
		cvInsuranceCostPerDay=Double.parseDouble(config.getParam("TESF.CVcosts", "insuranceCostPerDay"));		
		cvTariffCostPerDay=Double.parseDouble(config.getParam("TESF.CVcosts", "tariffCostPerDay"));			
		
	}	
	
}
