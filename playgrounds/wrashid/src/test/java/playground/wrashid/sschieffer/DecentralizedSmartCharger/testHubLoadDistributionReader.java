package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;

import junit.framework.TestCase;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class testHubLoadDistributionReader extends TestCase{
	
	final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
	String configPath="test/input/playground/wrashid/sschieffer/config.xml";
	final Controler controler=new Controler(configPath);
	
	public testHubLoadDistributionReader(){
		
	}


	public void testHubLoadReader() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException, InterruptedException{
		
		LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubsTest();
		LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution=readStochasticLoad(deterministicHubLoadDistribution.size());
		LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readHubsTest();//readPricingHubDistribution(optimalPrice, suboptimalPrice);
		LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;
		
		HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
		HubLoadDistributionReader hubReader= new HubLoadDistributionReader(controler, 
				hubLinkMapping,//HubLinkMapping hubLinkMapping
				deterministicHubLoadDistribution,
				stochasticHubLoadDistribution,
				pricingHubDistribution,
				null,
				null,
				1.0) ;
	
		/*public HubLoadDistributionReader(Controler controler, 
				HubLinkMapping hubLinkMapping,
				LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution,
				LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution,
				LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution,
				LinkedListValueHashMap<Integer, Schedule> locationSourceMapping,
				LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping,
				double gasPrice) throws IOException, OptimizationException, InterruptedException{*/
		
		System.out.println("GasPrice: 1");
		System.out.println();
		System.out.println("Constant Schedule: 1 or 3 ");
		Schedule sConstant = hubReader.deterministicHubLoadDistributionPHEVAdjusted.getValue(1);
		sConstant.printSchedule();
		System.out.println();
		
		assertEquals(sConstant.getNumberOfEntries(),2);
		assertEquals(sConstant.timesInSchedule.get(0).getEndTime(), 62490.0);
		
		
		System.out.println("Linear: 5/62490 x");
		Schedule sLinear = hubReader.deterministicHubLoadDistributionPHEVAdjusted.getValue(2);
		sLinear.printSchedule();
		System.out.println();
		assertEquals(sLinear.getNumberOfEntries(),3);
		assertEquals(sLinear.timesInSchedule.get(0).getEndTime(), 62490.0/5);
		assertEquals(((LoadDistributionInterval)sLinear.timesInSchedule.get(0)).isOptimal(), true);
		
		
		System.out.println("Parabolic: 5 + 5/62490.0 x + x°2");
		Schedule sParabolic = hubReader.deterministicHubLoadDistributionPHEVAdjusted.getValue(3);
		sParabolic.printSchedule();
		
		assertEquals(sParabolic.getNumberOfEntries(),2);
		assertEquals(sParabolic.timesInSchedule.get(0).getEndTime(), 62490.0);
		assertEquals(((LoadDistributionInterval)sParabolic.timesInSchedule.get(0)).isOptimal(), false);
		
	}
	
	
	
	
	public LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTestConstant());
		hubLoadDistribution1.put(2, makeBullshitScheduleTestLinear());
		hubLoadDistribution1.put(3, makeBullshitScheduleTestParabolic());
		
		
		return hubLoadDistribution1;
		
	}
	
	
	
	
	
	public Schedule makeBullshitScheduleTestConstant() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{1.0};// 
		double[] bullshitCoeffs2 = new double[]{3.0};
		
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		//l1.makeXYSeries();
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		//l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		//bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	
	
	
	
	
	public Schedule makeBullshitScheduleTestLinear() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{0, 5/62490.0};// 
		double[] bullshitCoeffs2 = new double[]{3.0};
		
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		//l1.makeXYSeries();
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		//l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		//bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	
	
	
	
	
	public Schedule makeBullshitScheduleTestParabolic() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{5, 5/62490.0, 1};// 
		double[] bullshitCoeffs2 = new double[]{5, 5/62490.0, 1};
		
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		//l1.makeXYSeries();
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		//l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		//bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}

	
	
	
	public LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution(double optimal, double suboptimal) throws IOException{
		
		LinkedListValueHashMap<Integer, Schedule> pricing= readHubsTest();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
		
		for(Integer i: pricing.getKeySet()){
			for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
				// for every time interval for every hub
				if(pricing.getValue(i).timesInSchedule.get(j).isParking()){
					LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
					
					if(l.isOptimal()){
						l= new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pOpt, 
								true);
					}else{
						l= new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pSubopt, 
								false);
						
					}
				}
			}
		}
		return pricing;
	
		
	}
	
	
	
	
	
	public LinkedListValueHashMap<Integer, Schedule> readStochasticLoad(int num){
		
		LinkedListValueHashMap<Integer, Schedule> stochastic= new LinkedListValueHashMap<Integer, Schedule>();
		
		Schedule bullShitStochastic= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(0, 24*3600, p, true));
		for (int i=0; i<num; i++){
			stochastic.put(i+1, bullShitStochastic);
		}
		return stochastic;
	/*	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;*/
		
	}
	
}