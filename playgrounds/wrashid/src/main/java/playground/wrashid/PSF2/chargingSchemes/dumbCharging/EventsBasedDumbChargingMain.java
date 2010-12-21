package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import playground.wrashid.PSF.PSS.PSSControler;

public class EventsBasedDumbChargingMain {

	public static void main(String[] args) {
		runEventsBasedPSSControler("H:\\data\\experiments\\ARTEMIS\\input\\pss\\zh\\config-event-file-based.xml");
	}
	
	public static void runEventsBasedPSSControler(String configPath){
		PSSControler pssControler=new PSSControlerDumbCharging(configPath, null);
		pssControler.runMATSimIterations();
	}
}
