package playground.wrashid.bsc.vbmh.controler;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;

import playground.wrashid.bsc.vbmh.vmEV.EVControl;
import playground.wrashid.bsc.vbmh.vmEV.EVControlerListener;
import playground.wrashid.bsc.vbmh.vmParking.ParkControl;
import playground.wrashid.bsc.vbmh.vmParking.ParkControlerListener;
import playground.wrashid.bsc.vbmh.vmParking.ParkScoringFactory;


public class SFControler1 {
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Los gehts");
		
		String parking_filename="input/SF_PLUS/VM/parkings_demo.xml";
		String pricing_filename="input/SF_PLUS/VM/parking_pricing_models_demo.xml";
		String evFilename = "input/SF_PLUS/VM/evs.xml";
		String parkHistoryFileName = "output/SF_PLUS/parkhistory/parkhistory"; 
		
		Config config = ConfigUtils.loadConfig(args[0]);
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		ParkControlerListener parklistener = new ParkControlerListener();
		parklistener.setParkHistoryOutputFileName(parkHistoryFileName);
		parklistener.getParkHandler().getParkControl().startup(parking_filename, pricing_filename, controler);
		controler.addControlerListener(parklistener);
		
		EVControlerListener evControlerListener = new EVControlerListener();
		evControlerListener.getEvHandler().getEvControl().startUp(evFilename, controler);
		parklistener.getParkHandler().getParkControl().setEvControl(evControlerListener.getEvHandler().getEvControl());
		controler.addControlerListener(evControlerListener);
		
		
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();
		ParkScoringFactory factory = new ParkScoringFactory(planCalcScoreConfigGroup, controler.getNetwork());
		controler.setScoringFunctionFactory(factory);
	
		
		
		
		
		controler.run();
	
		
	
	
	}

}
