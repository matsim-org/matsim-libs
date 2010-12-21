package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import playground.wrashid.PSF.PSS.PSSControler;

public class EventsBasedDumbChargingMain {

	public static void main(String[] args) {
		// run on slanger4
		runEventsBasedPSSControler("H:\\data\\experiments\\ARTEMIS\\zh\\dumb charging\\input\\config-event-file-based.xml");
	}
	
	public static void runEventsBasedPSSControler(String configPath){
		PSSControler pssControler=new PSSControlerDumbCharging(configPath, null);
		pssControler.runMATSimIterations();
	}
}
