package playground.wrashid.sschieffer.DecentralizedSmartCharger.mess;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.Schedule;

public class StellasMockDetermisticLoadAndPricingCollector {
	
	
	private LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution=
		new  LinkedListValueHashMap<Integer, Schedule>();
	
	private LinkedListValueHashMap<Integer, Schedule> hubPricingDistribution=
		new  LinkedListValueHashMap<Integer, Schedule>();
	
	public StellasMockDetermisticLoadAndPricingCollector(){
		setUpHubLoadTest();
	}
	
	
	public LinkedListValueHashMap<Integer, Schedule> getDeterminisitcHubLoad(){
		return hubLoadDistribution;
	}
	
	
	public LinkedListValueHashMap<Integer, Schedule> getDeterminisitcPriceDistribution(){
		return hubPricingDistribution;
	}
	
	
	
	
	
	public void  setUpHubLoadTest(){
		
		double energyPricePerkWh=0.25;
		double standardConnectionElectricityJPerSecond= 3500; 
		double optimalPrice=energyPricePerkWh*1/1000*1/3600*standardConnectionElectricityJPerSecond;//0.25 CHF per kWh		
		double suboptimalPrice=optimalPrice*3; // cost/second  
		
		
		//**********************
		// DEFINE HUBS 
		//**********************
		HubLoadSchedule h1= new HubLoadSchedule("Hub 1");
		
		h1.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[]{100*3500, 500*3500/(62490.0), 0}), 
				true);
		h1.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0}), 
				false);
		
		hubLoadDistribution.put(1, h1.getHubLoadSchedule());
		//**********************
		
		HubLoadSchedule p1= new HubLoadSchedule("Hub Prices 1");
		
		p1.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[] {optimalPrice}),
				true);
		
		p1.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[] {suboptimalPrice}), 
				false);
		
		hubPricingDistribution.put(1, p1.getHubLoadSchedule());
		
		
		
		//**********************
		// DEFINE HUBS 
		//**********************
		HubLoadSchedule h2= new HubLoadSchedule("Hub 2");
		
		h2.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[]{100*3500, 500*3500/(62490.0), 0}), 
				true);
		h2.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0}), 
				false);
		
		hubLoadDistribution.put(2, h2.getHubLoadSchedule());
		
		HubLoadSchedule p2= new HubLoadSchedule("Hub Prices 2");
		
		p2.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[] {optimalPrice}),
				true);
		
		p2.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[] {suboptimalPrice}), 
				false);
		
		hubPricingDistribution.put(2, p2.getHubLoadSchedule());
		//**********************
		
		//**********************
		// DEFINE HUBS 
		HubLoadSchedule h3= new HubLoadSchedule("Hub 3");
		
		h3.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[]{100*3500, 500*3500/(62490.0), 0}), 
				true);
		h3.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0}), 
				false);
		
		hubLoadDistribution.put(3, h3.getHubLoadSchedule());
		
		
		HubLoadSchedule p3= new HubLoadSchedule("Hub Prices 3");
		
		p3.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[] {optimalPrice}),
				true);
		
		p3.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[] {suboptimalPrice}), 
				false);
		
		hubPricingDistribution.put(3, p3.getHubLoadSchedule());
		//**********************
		
		//**********************
		// DEFINE HUBS 
		HubLoadSchedule h4= new HubLoadSchedule("Hub 4");
		
		h4.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[]{100*3500, 500*3500/(62490.0), 0}), 
				true);
		h4.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0}), 
				false);
		
		hubLoadDistribution.put(4, h4.getHubLoadSchedule());
		//**********************
		
		HubLoadSchedule p4= new HubLoadSchedule("Hub Prices 4");
		
		p4.addLoadInterval(
				0.0, //start
				62490.0, //end
				new PolynomialFunction(new double[] {optimalPrice}),
				true);
		
		p4.addLoadInterval(				
				62490.0, 
				DecentralizedSmartCharger.SECONDSPERDAY,
				new PolynomialFunction(new double[] {suboptimalPrice}), 
				false);
		
		hubPricingDistribution.put(4, p4.getHubLoadSchedule());
		
	}
}
