package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G.V2G;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.PlanXMLGenerator;


/**
 * change the input  configName Strings
 * to get a summary about total time taken for computation of Decentralized Smart Charger
 * @author Stella
 *
 */
public class TimeTesting {

	
	public static void main(String[] args) throws IOException {
		
		final String configName= "config_plans10.xml";
		String configPath="test/input/playground/wrashid/sschieffer/"+configName;
		final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
		
		final double standardChargingSlotLength=15*60;
		
		final double electrification= 1.0; 
		// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
		final double ev=0.0; 
		
		
		final double bufferBatteryCharge=0.0;
		
		final double compensationPerKWHRegulationUp=0.15;
		final double compensationPerKWHRegulationDown=0.15;
		final double compensationPERKWHFeedInVehicle=0.15;
	
		final double xPercentDown=0.0;
		final double xPercentDownUp=1.0;
		
		
		
		
		DecentralizedSmartCharger myDecentralizedSmartCharger;
		
		final TestSimulationSetUp mySimulation = new TestSimulationSetUp(
				configPath, 
				electrification, 
				ev 
				);
		
		Controler controler= mySimulation.getControler();
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					/*PlanXMLGenerator plans= new PlanXMLGenerator(1000);
					plans.setAgents(10);
					plans.setAgents(10000);
					plans.setAgents(20000);*/
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					
					myDecentralizedSmartCharger.setDebug(false);
					myDecentralizedSmartCharger.run();
					
					myDecentralizedSmartCharger.writeSummaryDSC("DSC"+configName);
					
					/*
					 * V2G
					 */
					mySimulation.setUpStochasticLoadDistributions();
					
					myDecentralizedSmartCharger.setStochasticSources(
							mySimulation.getStochasticLoadSchedule(), 
							null, 
							mySimulation.getAgentStochasticLoadSources());
					
					mySimulation.setUpAgentSchedules(
							myDecentralizedSmartCharger, 
							compensationPerKWHRegulationUp, 
							compensationPerKWHRegulationDown,
							compensationPERKWHFeedInVehicle,
							xPercentDown, 
							xPercentDownUp);
					
					myDecentralizedSmartCharger.setAgentContracts(mySimulation.getAgentContracts());
					
					V2G myV2G= new V2G(myDecentralizedSmartCharger);
					myDecentralizedSmartCharger.setV2G(myV2G);
					myDecentralizedSmartCharger.initializeAndRunV2G(
							xPercentDown, xPercentDownUp);
					
					myDecentralizedSmartCharger.writeSummaryV2G("V2G"+configName);
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();

	}

}
