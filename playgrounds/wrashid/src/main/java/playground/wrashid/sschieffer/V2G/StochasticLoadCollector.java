package playground.wrashid.sschieffer.V2G;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DSC.DecentralizedChargingSimulation;
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DSC.GeneralSource;
import playground.wrashid.sschieffer.DSC.HubInfoDeterministic;
import playground.wrashid.sschieffer.DSC.HubInfoStochastic;
import playground.wrashid.sschieffer.DSC.LoadDistributionInterval;
import playground.wrashid.sschieffer.DSC.LoadFileReader;
import playground.wrashid.sschieffer.DSC.Schedule;


/**
 * makes stochastic schedules for agent vehicles, hubsources and hubs in general
 * <li> hub in general 3 hours, 2 hours down, 1 hour up
 * <li> hub source - currently null
 * <li> every agent has 1*30 min source or sink
 * 
 * @author Stella
 *
 */
public class StochasticLoadCollector {
	
	private ArrayList<HubInfoStochastic> myHubInfo;
	
	private HashMap<Integer, Schedule> stochasticHubLoadDistribution;
	
	private HashMap<Id, GeneralSource> stochasticHubSource;
	
	private static HashMap<Id, Schedule> agentSource;
	private DecentralizedChargingSimulation mySimulation;
	boolean testCase=false;
	
	public StochasticLoadCollector(	DecentralizedChargingSimulation mySimulation, 
			ArrayList<HubInfoStochastic> myHubInfo) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		this.mySimulation=mySimulation;
		this.myHubInfo= myHubInfo;
		stochasticHubLoadDistribution=	new  HashMap<Integer, Schedule>();
		stochasticHubSource=new  HashMap<Id, GeneralSource>();
		agentSource= new HashMap<Id, Schedule>();
		
		
	}
	
	
	
	public StochasticLoadCollector(	DecentralizedChargingSimulation mySimulation) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		this.mySimulation=mySimulation;
		testCase=true;
		
		
	}
	
	
	
	public void setTestCase(boolean bla){
		testCase=bla;
	}

	
	public void setUp() throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException{
		if(testCase){
			 makeStochasticHubLoad();
			 agentSource=null;
			 stochasticHubSource=null;
			 //makeAgentVehicleSource();
			 //makeSourceHub();
			
		}else{
			for(int hub=0;hub< myHubInfo.size();hub++){
				
				//GENERAL LOAD
				readGeneralStochasticLoad(hub);
				
				//HUBLOADS
				readHubSources(hub);
				
				//AGENT VEHICLES
				readAgentVehicles(hub);		
			}
		}
		
	}
	
	
	private void readGeneralStochasticLoad(int hub) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		int hubId= myHubInfo.get(hub).getId();
		//GENERAL LOAD
		if(myHubInfo.get(hub).isTxtGeneralStochastic()){
			String file= myHubInfo.get(hub).geStochasticGeneralLoadTxt();
			LoadFileReader stochasticFreeLoad = new LoadFileReader(file,  
					96, 
					"stochastic free load for hub "+ hubId +" from file", 
					"stochastic free load for hub "+ hubId +" fitted");
			
			stochasticHubLoadDistribution.put(hubId, 
					LoadFileReader.makeSchedule(stochasticFreeLoad.getFittedFunction()));
			
			LoadFileReader.visualizeTwoHubSeries(mySimulation.outputPath+"V2G/stochastic free load for hub "+ hubId+".png","stochastic free load for hub "+ hubId, "Load [W]", 
					stochasticFreeLoad.getLoadFigureData(), stochasticFreeLoad.getFittedLoadFigureData());
		}else{
			// given as loadINtervals
			ArrayList<LoadDistributionInterval> list =myHubInfo.get(hub).getGeneralStochasticLoadIntervals();
			
			stochasticHubLoadDistribution.put(hubId, 
					makeScheduleFromArrayListLoadIntervals(list));
		}
		
		
	}
	
	
	
	public Schedule makeScheduleFromArrayListLoadIntervals(ArrayList<LoadDistributionInterval> list){
		Schedule stochasticSchedule= new Schedule();
		for(int i=0; i< list.size(); i++){
			stochasticSchedule.addTimeInterval(list.get(i));
		}
		stochasticSchedule.fillNonExistentTimesInLoadScheduleWithZeroLoads();
		return stochasticSchedule;
	}
	
	
	
	private void readHubSources(int hub) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		int hubId= myHubInfo.get(hub).getId();
		
		ArrayList<GeneralSource> stochasticGeneralSources= myHubInfo.get(hub).getStochasticGeneralSources();
		if (stochasticGeneralSources!= null){
			for(int i=0; i<stochasticGeneralSources.size(); i++){
				
				String file= stochasticGeneralSources.get(i).getInputLoad96Bins();
				
				Id linkId= stochasticGeneralSources.get(i).getlinkId();
				hubId = mySimulation.mySmartCharger.myHubLoadReader.getHubForLinkId(linkId);
				String name= stochasticGeneralSources.get(i).getName();
				
				LoadFileReader stochasticHubSourceLoad = new LoadFileReader(file,  
						96, 
						"stochastic load '"+name+"' at link "+ linkId.toString() +"at hub "+ hubId +" from file", 
						"stochastic load '"+name+"' at link "+ linkId.toString() +"at hub "+ hubId +" fitted");
				
				stochasticGeneralSources.get(i).setLoadSchedule
					(LoadFileReader.makeSchedule(stochasticHubSourceLoad.getFittedFunction()));
				
				stochasticHubSource.put(linkId, stochasticGeneralSources.get(i));
				
				LoadFileReader.visualizeTwoHubSeries(mySimulation.outputPath+"V2G/stochasticHubSourceAtLink"+ linkId.toString()+".png",
						"stochastic hub source at link "+linkId.toString()+ ": "+name, "Load [W]", 
						stochasticHubSourceLoad.getLoadFigureData(), stochasticHubSourceLoad.getFittedLoadFigureData());
				
			}
		}
	}
	
	
	
	
	/**
	 * reads in the agent vehicls from HubInfoStochastic
	 * either from txt file or from LoadDistributionIntervals
	 *  
	 * @param hub
	 * @throws ConvergenceException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private  void readAgentVehicles(int hub) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		if (myHubInfo.get(hub).isTxtVehicleStochastic()){
			HashMap <Id, String> vehicleLoads= myHubInfo.get(hub).geStochasticVehicleLoadTxt();
			
			for(Id id: vehicleLoads.keySet()){
				String file= vehicleLoads.get(id);
				LoadFileReader stochasticVehicleLoad = new LoadFileReader(file,  
						96, 
						"stochastic vehicle load "+ id.toString() +" from file", 
						"sstochastic vehicle load "+ id.toString() +" fitted");
				
				agentSource.put(id, 
						LoadFileReader.makeSchedule(stochasticVehicleLoad.getFittedFunction()));
				
				LoadFileReader.visualizeTwoHubSeries(mySimulation.outputPath+"V2G/stochastic vehicle load "+ id.toString() +".png","stochastic vehicle load "+ id.toString(), "Load [W]", 
						stochasticVehicleLoad.getLoadFigureData(), stochasticVehicleLoad.getFittedLoadFigureData());
				
			}
		}else{
			HashMap <Id, ArrayList<LoadDistributionInterval>> vehicleLoads= myHubInfo.get(hub).getStochasticVehicleSourcesIntervals();
			for(Id id: vehicleLoads.keySet()){
				ArrayList<LoadDistributionInterval> list= vehicleLoads.get(id);
				
				agentSource.put(id, 
						makeScheduleFromArrayListLoadIntervals(list));
				
				agentSource.get(id).visualizeLoadDistribution("stochastic vehicle load "+ id.toString() +" from input schedule",
						mySimulation.outputPath+"V2G/stochastic vehicle load "+ id.toString() +".png");
				
			}
		}
		
	}
	
	
	
	
	public HashMap<Integer, Schedule> getStochasticHubLoad(){
		return stochasticHubLoadDistribution;
	}
	
	public HashMap<Id, Schedule> getStochasticAgentVehicleSources(){
		return agentSource;
	}
	
	public HashMap<Id, GeneralSource> getStochasticHubSources(){
		return stochasticHubSource;
	}
	
	
	public void makeAgentVehicleSource(){
		
		// at random agents get 30 minutes source
		for(Id id : mySimulation.mySmartCharger.vehicles.getKeySet()){
			double secs= 0.5*3600;
			
			double buffer= DecentralizedSmartCharger.SECONDSPERDAY-secs;
			double startSec= Math.random()*buffer;
			
			if(Math.random()<0.5){
				
				Schedule bullShitPlus= new Schedule();
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitPlus.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, pPlus, true));
				
				bullShitPlus=bullShitPlus.fillNonExistentTimesInLoadScheduleWithZeroLoads();
				agentSource.put(id, bullShitPlus);	
			}else{
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, pMinus, false));
				bullShitMinus=bullShitMinus.fillNonExistentTimesInLoadScheduleWithZeroLoads();
				agentSource.put(id, bullShitMinus);	
			}
		}
		
	}
	
	
	
	public void makeStochasticHubLoad(){
		// 4 * 15 minutes = 1 hour
		// 
		double secs= 60*60.0;								
		double startSec= 0.0;
		
		Schedule bullShitStochastic= new Schedule();
		int numPpl= (int)(mySimulation.mySmartCharger.vehicles.getKeySet().size()*1.0);
		PolynomialFunction p1 = new PolynomialFunction(new double[] {numPpl*3500});
		PolynomialFunction p2 = new PolynomialFunction(new double[] {-numPpl*3500});
		PolynomialFunction p3 = new PolynomialFunction(new double[] {numPpl*3500});
		PolynomialFunction p4 = new PolynomialFunction(new double[] {numPpl*3500});
		
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(startSec, startSec+secs, p1, true));
		double newStart=startSec+(DecentralizedSmartCharger.SECONDSPERDAY/4);
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(newStart, newStart+secs, p2, true));
		newStart+=(DecentralizedSmartCharger.SECONDSPERDAY/4);
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(newStart, newStart+secs, p3, true));
		newStart+=(DecentralizedSmartCharger.SECONDSPERDAY/4);
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(newStart, newStart+secs, p4, true));
		
		bullShitStochastic=bullShitStochastic.fillNonExistentTimesInLoadScheduleWithZeroLoads();
		stochasticHubLoadDistribution.put(1, bullShitStochastic);
		
	}
	
	
	
	public void writeOutStochasticHubLoad(Schedule schedule){
		LoadFileReader.create15MinBinTextFromSchedule(schedule, 
				mySimulation.outputPath+"V2G/stochasticGeneralHubLoad"+mySimulation.controler.getPopulation().getPersons().size()+".txt");		
			
	}
	
	
	
	public void makeSourceHub(){
		Id link=null;
		for(Id linkId:mySimulation.controler.getNetwork().getLinks().keySet()){
			link=linkId;
			break;
		}
		Schedule bullShit= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShit.addTimeInterval(new LoadDistributionInterval(50000.0, 62490.0, p, true));
		
		bullShit= bullShit.fillNonExistentTimesInLoadScheduleWithZeroLoads();
		GeneralSource g= new GeneralSource("", link, "test",0.005);
		g.setLoadSchedule(bullShit);
		
		stochasticHubSource.put(link, g);		
		
		
	}
	
	
}
