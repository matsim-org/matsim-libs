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
		
		String parking_filename;
		String pricing_filename;
		String evFilename;
		String parkHistoryFileName; 
		
		Config config = ConfigUtils.loadConfig(args[0]);
		parking_filename=config.getModule("VM_park").getValue("inputParkingFile");
		pricing_filename=config.getModule("VM_park").getValue("inputPricingFile");
		evFilename=config.getModule("VM_park").getValue("inputEVFile");
		parkHistoryFileName = config.getModule("controler").getValue("outputDirectory")+"/parkhistory/parkhistory"; 
		
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
