package playground.wrashid.bsc.vbmh.controler;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;

import playground.wrashid.bsc.vbmh.vm_parking.Park_Controler_Listener;
import playground.wrashid.bsc.vbmh.vm_parking.Park_Scoring_Factory;


public class DerControler {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Los gehts");
		
		String parking_filename="input/parkings_demo.xml";
		String pricing_filename="input/parking_pricing_models_demo.xml";
		
		Config config = ConfigUtils.loadConfig(args[0]);
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		Park_Controler_Listener parklistener = new Park_Controler_Listener();
		parklistener.park_handler.park_control.startup(parking_filename, pricing_filename, controler);
		controler.addControlerListener(parklistener);
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();
		Park_Scoring_Factory factory = new Park_Scoring_Factory(planCalcScoreConfigGroup, controler.getNetwork());
		controler.setScoringFunctionFactory(factory);
		
		controler.run();
	
		
	
	
	}

}
