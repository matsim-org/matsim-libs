package playground.vbmh.controler;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import playground.vbmh.vmEV.EVControlerListener;
import playground.vbmh.vmParking.ParkControlerListener;
import playground.vbmh.vmParking.ParkScoringFactory;

import java.io.File;


public class SFControler1 {
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Los gehts");
		
		String parking_filename;
		String pricing_filename;
		String evFilename;
		String parkHistoryFileName; 
		
		Config config = ConfigUtils.loadConfig(args[0]);
		
		//System.out.println("OutputDir Extension?");
		//Scanner s = new Scanner(System.in);
		//String extension=s.next();
		
		String extension = args[1];
		System.out.println(extension);
		
		config.getModule("controler").addParam("outputDirectory", config.getModule("controler").getValue("outputDirectory")+extension);
		System.out.println(config.getModule("controler").getValue("outputDirectory"));

		//make directory
		try{
			File dir = new File(config.getModule("controler").getValue("outputDirectory"));
			dir.mkdir();
			dir = new File(config.getModule("controler").getValue("outputDirectory")+"/parkhistory");
			dir.mkdir();
			dir = new File(config.getModule("controler").getValue("outputDirectory")+"/Charts");
			dir.mkdir();
		}catch(Exception e){
			System.out.println("Verzeichniss wurde nicht angelegt");
		}
		//-----
				
		
		parking_filename=config.getModule("VM_park").getValue("inputParkingFile");
		pricing_filename=config.getModule("VM_park").getValue("inputPricingFile");
		evFilename=config.getModule("VM_park").getValue("inputEVFile");
		parkHistoryFileName = config.getModule("controler").getValue("outputDirectory")+"/parkhistory/parkhistory"; 
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		ParkControlerListener parklistener = new ParkControlerListener();
		parklistener.setParkHistoryOutputFileName(parkHistoryFileName);
		parklistener.getParkHandler().getParkControl().startup(parking_filename, pricing_filename, controler);
		controler.addControlerListener(parklistener);
		
		EVControlerListener evControlerListener = new EVControlerListener();
		evControlerListener.getEvHandler().getEvControl().startUp(evFilename, controler);
		parklistener.getParkHandler().getParkControl().setEvControl(evControlerListener.getEvHandler().getEvControl());
		controler.addControlerListener(evControlerListener);
		
		
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();
        ParkScoringFactory factory = new ParkScoringFactory(planCalcScoreConfigGroup, controler.getScenario().getNetwork());
		controler.setScoringFunctionFactory(factory);
	
		//Spezialpreis Test:
//		PricingModels pricing = parklistener.getParkHandler().getParkControl().getPricing();
//		pricing.removeModel(0);
//		specialTestModel testModel = new specialTestModel();
//		pricing.add(testModel);
		
		
		
		controler.run();
	
		
	
	
	}

}
