package freightKt.prepareCarrier;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;

class prepareCarrier {

	/**
	 * @author: Kturner
	 * Ziele:
	 *  1.) Analyse der Carrier aus dem Schroeder/Liedtke-Berlin-Szenario.
	 * TODO: 2.) Erstellung eines Carrier-Files mit der Kette der größten Nachfage (Beachte, dass dabei Frozen, dry, Fresh 
	 * 	jeweils eigene Carrier mit eigener Flotten zsammensetzung sind.)
	 * TODO: 3.) Links der Umweltzone einlesen (Maut-file) und die Nachfrage entsprechend auf UCC-Carrier mit ihren Depots und den Elektrofahrzeugen aufteilen.
	 */
	
	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/Case_KT/" ;
	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/";

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "network" ;
	private static final String VEHTYPES_NAME = "vehicleTypes" ;
	private static final String CARRIERS_NAME = "carrierLEH_v2_withFleet" ;
	private static final String CARRIERS_OUT_NAME = "carrier_1Retailer" ;
	private static final String TOLL_NAME = "toll_distance_test_kt";
	//Ende  Namesdefinition Berlin


//	//Beginn Namesdefinition KT Für Test-Szenario (Grid)
//	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Grid_Szenario/" ;
//	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Matsim/Demand/Grid/UCC2/" ;
//	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/" ;	
//
//	//Dateinamen ohne XML-Endung
//	private static final String NETFILE_NAME = "grid-network" ;
//	private static final String VEHTYPES_NAME = "grid-vehTypes_kt" ;
//	private static final String CARRIERS_NAME = "grid-carrier" ;
//	private static final String CARRIERS_OUT_NAME = "grid-algorithm" ;
//	private static final String TOLL_NAME = "grid-tollCordon";
//	//Ende Namesdefinition Grid


	private static final String NETFILE = INPUT_DIR + NETFILE_NAME + ".xml" ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPES_NAME + ".xml";
	private static final String CARRIERFILE = INPUT_DIR + CARRIERS_NAME + ".xml" ;
	private static final String CARRIEROUTFILE = INPUT_DIR + CARRIERS_OUT_NAME + ".xml";
	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";
	
	
	public static void main(String[] args) {
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERFILE) ;
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPEFILE) ;
		
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		
		createDir(new File(OUTPUT_DIR));
		
//		calcInfosPerCarrier(carriers);	//Step 1: Analyse der vorhandenen Carrier
		extractCarrier(carriers, "aldi"); //Step 2: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)
		
		System.out.println("### ENDE ###");
	}

	//Step 2: Extrahieren einzelner Retailer (alle, die mit dem RetailerNamen beginnen)
	private static void extractCarrier(Carriers carriers, String retailerName) {
		String carrierId;
		for (Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();
			if (carrierId.startsWith(retailerName)){			//Carriername beginnt mit Retailername
				Carriers tempCarriers = new Carriers();
				tempCarriers.addCarrier(carrier);
				new CarrierPlanXmlWriterV2(tempCarriers).write(OUTPUT_DIR + carrierId +".xml") ;
			}
			
		}
		
	}

	//Step 1: Analyse der vorhandenen Carrier
	private static void calcInfosPerCarrier(Carriers carriers) {
		
		File carrierInfoFile = new File(OUTPUT_DIR + "CarrierInformation.txt" );
		
		String carrierId;
		int nOfServices;
		int demand;
		int nOfDepots;
		ArrayList<String> depotLinks = new ArrayList<String>();
		ArrayList<String> vehTypes = new ArrayList<String>();
		
		for(Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();
			
			demand = 0; 
			nOfServices = 0;
			for (CarrierService service : carrier.getServices()){
				demand += service.getCapacityDemand();
				nOfServices++;
			}
			
			nOfDepots = 0;
			depotLinks.clear();
			vehTypes.clear();
			for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles()){
				String depotLink = vehicle.getLocation().toString();
				if (!depotLinks.contains(depotLink)){
					depotLinks.add(depotLink);
					nOfDepots++;
				}
				if (!vehTypes.contains(vehicle.getVehicleType().getId().toString())){
					vehTypes.add(vehicle.getVehicleType().getId().toString());
				}
			}
			
			WriteCarrierInfos carrierInfoWriter = new WriteCarrierInfos(carrierInfoFile, carrierId, nOfServices, demand, nOfDepots, vehTypes) ;
		}
		
	}
	
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + "erstellt: "+ file.mkdirs());	
	}

}
