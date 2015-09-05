package playground.vbmh.einzel_klassen_tests;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.vbmh.util.RemoveDuplicate;
import playground.vbmh.vmEV.EVControlerListener;
import playground.vbmh.vmParking.ParkControlerListener;
import playground.vbmh.vmParking.ParkScoringFactory;
import playground.vbmh.vmParking.Parking;
import playground.vbmh.vmParking.ParkingPricingModel;
import playground.vbmh.vmParking.ParkingSpot;
import playground.vbmh.vmParking.PricingModels;


public class Testcontroller {
	
	
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

		//Verzeichnisse erstellen
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
		parklistener.getParkHandler().getParkControl().startup("input/SF_PLUS/generalinput/parking_08pmsl.xml", "input/SF_PLUS/finalEXP/erste_min_5/pricing_erste_min_5.xml", controler);
		controler.addControlerListener(parklistener);
		
		EVControlerListener evControlerListener = new EVControlerListener();
		evControlerListener.getEvHandler().getEvControl().startUp(evFilename, controler);
		parklistener.getParkHandler().getParkControl().setEvControl(evControlerListener.getEvHandler().getEvControl());
		controler.addControlerListener(evControlerListener);
		
		
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();
        ParkScoringFactory factory = new ParkScoringFactory(planCalcScoreConfigGroup, controler.getConfig().scenario(), controler.getScenario().getNetwork());
		controler.setScoringFunctionFactory(factory);
	
		//Spezialpreis Test:
//		PricingModels pricing = parklistener.getParkHandler().getParkControl().getPricing();
//		pricing.removeModel(0);
//		specialTestModel testModel = new specialTestModel();
//		pricing.add(testModel);
		
		parklistener.getParkHandler().getParkControl().iterStart();
		System.out.println("teste preismodell");
		PricingModels preisemodelle = parklistener.getParkHandler().getParkControl().getPricing();
		ParkingPricingModel testmodel = preisemodelle.get_model(101);
		System.out.println("modell geladen");
		System.out.println(testmodel.calculateParkingPrice(3000, true));
		
		parklistener.getParkHandler().getParkControl().parkingMap.createSpots(preisemodelle);
		Collection<Parking> parkingListe = parklistener.getParkHandler().getParkControl().parkingMap.getPublicParkings(683614, 4823965, 10);
		for(Parking parking : parkingListe){
			System.out.println("Pruefe treffer");
			if(parking.id==9000006){
				System.out.println("Parking lot 6 gefunden");
				ParkingSpot spot = parking.checkForFreeSpotEVPriority();
				System.out.println("Spot charge: "+spot.isCharge());
				System.out.println(preisemodelle.calculateParkingPrice(3000, true, spot));
			}
		}
		System.out.println("Parking lot test abgeschlossen");
		
		System.out.println("Teste get public");
		LinkedList<ParkingSpot> listeZweinev = parklistener.getParkHandler().getParkControl().getPublicParkings(new CoordImpl(683614, 4823965), false);
		LinkedList<ParkingSpot> listeZwei = parklistener.getParkHandler().getParkControl().getPublicParkings(new CoordImpl(683614, 4823965), true);
		listeZwei.addAll(listeZweinev);
		RemoveDuplicate.RemoveDuplicate(listeZwei);
		for(ParkingSpot spot : listeZwei){
			//System.out.println("Pruefe treffer");
			if(spot.parking.id ==9000006){
				System.out.println("Parking lot 6 gefunden");
				System.out.println("Spot charge: "+spot.isCharge());
				System.out.println("Charging rate: "+spot.getChargingRate());
				System.out.println(preisemodelle.calculateParkingPrice(2460, true, spot));
			}
		}
		
		testmodel = preisemodelle.get_model(2);
		System.out.println(testmodel.calculateParkingPrice(-1800, false));
		
		
		
		
		
		
//		controler.run();
//		LegImpl leg = new LegImpl(null);
//		leg.getDepartureTime()
//		System.out.println((LegImpl)controler.getPopulation().getPersons().values().iterator().next().getSelectedPlan().getPlanElements().get(3));
//	
//		PopulationImpl pops = (PopulationImpl) controler.getPopulation();
//		ScenarioImpl scen = (ScenarioImpl) controler.getScenario();
//	
		
	
	}

}
