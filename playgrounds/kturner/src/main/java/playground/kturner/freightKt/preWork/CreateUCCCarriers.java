package playground.kturner.freightKt.preWork;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jsprit.core.problem.vehicle.VehicleType;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;

class CreateUCCCarriers {

	/*
	 * !!!ACHTUNG: MEthoden sind veraltet: siehe stattdessen; freigtKt.UccCarrierCreator
	 * @author: Kturner
	 * Ziele:
	 *  1.) Analyse der Carrier aus dem Schroeder/Liedtke-Berlin-Szenario.
	 *  2.) Erstellung eines Carrier-Files mit der Kette der größten Nachfage (Beachte, dass dabei Frozen, dry, Fresh) 
	 * 	jeweils eigene Carrier mit eigener Flotten zsammensetzung sind.) -> Hier aldi
	 *  3.) Links der Umweltzone einlesen (Maut-file) und die Nachfrage entsprechend auf UCC-Carrier mit ihren Depots und den Elektrofahrzeugen aufteilen.
	 */

	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/Case_KT/" ;
	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/";

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "network" ;
	private static final String VEHTYPES_NAME = "vehicleTypes" ;
	private static final String CARRIERS_NAME = "carrierLEH_v2_withFleet" ;
	private static final String CARRIERS_2EXTRACT = "aldi" ;			//Retailer Name, der herausselektiert werden soll.
	private static final String TOLL_NAME = "toll_city_kt";
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
	private static final String CARRIEROUTFILE = OUTPUT_DIR + CARRIERS_2EXTRACT + ".xml";
	private static final String CARRIER_SPLIT_OUTFILE = OUTPUT_DIR + "carrier_" + CARRIERS_2EXTRACT + "_splitted.xml";
	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";
	
	static Carriers carriers = new Carriers() ;
	static CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;

	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));

		//Carrier-Stuff
//		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERFILE) ;
//		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

//		calcInfosPerCarrier(carriers);	//Step 1: Analyse der vorhandenen Carrier
		Carriers extractedCarriers = extractCarrier(carriers, CARRIERS_2EXTRACT); //Step 2a: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)		
		new CarrierPlanXmlWriterV2(extractedCarriers).write(CARRIEROUTFILE) ;
		Carriers splittedCarriers = createUCCCarrier(extractedCarriers, TOLLFILE);	//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
		splittedCarriers = renameVehId(splittedCarriers); 							//Step4: VehId je Carrier einzigartig machen, da sonst weitere Vorkommen ingnoriert werden (und somit nicht alle Depots genutzt werden).
		new CarrierPlanXmlWriterV2(splittedCarriers).write(CARRIER_SPLIT_OUTFILE);
		
		System.out.println("### ENDE ###");
	}
	




	//Carrier Step 1: Analyse der vorhandenen Carrier
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

	//Carrier Step 2: Extrahieren einzelner Retailer (alle, die mit dem RetailerNamen beginnen)
	private static Carriers extractCarrier(Carriers carriers, String retailerName) {
		String carrierId;
		Carriers tempCarriers = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();
			if (carrierId.startsWith(retailerName)){			//Carriername beginnt mit Retailername
				tempCarriers.addCarrier(carrier);
			}
		}
		return tempCarriers;
	}

	//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
	private static Carriers createUCCCarrier(Carriers carriers,
			String tollfile2) {
		
		Carriers splittedCarriers = new Carriers(); // Carrierfile, welches beide Carrier enthält: sowohl UCC, als auch non UCC
		
		//Read tollfile
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
//			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
//			rpConfig.setTollLinksFile("F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/toll_distance_test_kt.xml");
			rpReader.readFile(TOLLFILE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		Set<Id<Link>> tolledLinkIds = scheme.getTolledLinkIds();  //Link-Ids des MautSchemas
		Set<CarrierService> serviceToRemove= new HashSet<CarrierService>(); 	//Liste der zum UCC-Carrier übertragenen Services -> wird später aus normalen Carrier entfernt
		
		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier uccCarrier = CarrierImpl.newInstance(Id.create("UCC_"+carrier.getId() , Carrier.class));
			
			for (CarrierService service: carrier.getServices()) {
				if (tolledLinkIds.contains(service.getLocationLinkId())){	//Service liegt in der Maut-Zone (=Umweltzone)
					uccCarrier.getServices().add(service);		//Füge Service zum UCC_Carrier hinzu
					serviceToRemove.add(service);
				}
			}
			
			for (CarrierService service: serviceToRemove){ //neue Schleife, da sonst innerhalb der Schleife das Set modifiziert wird..
				carrier.getServices().remove(service);	//und lösche ihn aus dem normalen Carrier raus
			}
			
			splittedCarriers.addCarrier(carrier); //bisherigen Carrier reinschreiben, darf auch ohne Service sein, da ggf während Laufzeit nachfrage erhält (Depot -> UCC).
			
			if (!uccCarrier.getServices().isEmpty()){		//keinen UCC ohne Nachfrage übernehmen.
				addVehicles(uccCarrier);
				uccCarrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
				splittedCarriers.addCarrier(uccCarrier);
			}
		}
		
		return splittedCarriers;
	}
	
	//Step3b: Fahrzeuge der UCC zuordnen
	//TODO: Links als Array mit übergeben -> flexibler gestalten.
	private static void addVehicles(Carrier uccCarrier) {
		
		Set<Id<Link>> uccDepotsLinkIds = new HashSet<Id<Link>>();
		//Für Berlin-Szenario:
		uccDepotsLinkIds.add(Id.createLinkId("6874"));	//Nord: Industriegebiet Berliner Großmarkt
		uccDepotsLinkIds.add(Id.createLinkId("3058"));  //Süd: Gewerbegebiet  Bessermerstraße
		uccDepotsLinkIds.add(Id.createLinkId("5468"));	//Ost: Gewerbegebiet Herzbergstr /Siegfriedstr.
		
		double uccOpeningTime = 8*3600.0;	// 08:00:00 Uhr
		double uccClosingTime = 21*3600.0;	// 21:00:00 Uhr
		
		if (uccCarrier.getId().toString().endsWith("TIEFKUEHL")){
			
			for (Id<Link> linkId : uccDepotsLinkIds ){
			uccCarrier.getCarrierCapabilities().getCarrierVehicles().add( CarrierVehicle.Builder.newInstance(Id.create("light8telectro_frozen", Vehicle.class), linkId)
					.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro_frozen", VehicleType.class)))
					.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
					.build());
			}
//			uccCarrier.getCarrierCapabilities().getCarrierVehicles().add(cv_8f);
		} else {
			
			for (Id<Link> linkId : uccDepotsLinkIds ){
				uccCarrier.getCarrierCapabilities().getCarrierVehicles().add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro", Vehicle.class), linkId)
					.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro", VehicleType.class)))
					.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
					.build());
			
			uccCarrier.getCarrierCapabilities().getCarrierVehicles().add(CarrierVehicle.Builder.newInstance(Id.create("medium18telectro", Vehicle.class), linkId)
					.setType(vehicleTypes.getVehicleTypes().get(Id.create("medium18telectro", VehicleType.class)))
					.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
					.build());

			}
			
		}
		
	}

	//Step4: VehicleId je Carrier um Location erweitern, da sonst weitere Vorkommen auf Grund gleicher VehicleId ingnoriert werden 
	//		und somit nicht alle Depots genutzt werden.
	private static Carriers renameVehId(Carriers carriers) {
		
		for (Carrier carrier : carriers.getCarriers().values()){
			//da Änderung der vorhanden Fahrzeuge sonst nicht ging, Umweg über newInstance und setzen der Eigenschaften.
			CarrierCapabilities tempCc = CarrierCapabilities.newInstance();
			tempCc.setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
			//Vehicle neu erstellen, da setVehicleId nicht verfügbar.
			for (CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
				//
				tempCc.getCarrierVehicles().add(CarrierVehicle.Builder
						.newInstance(Id.create(cv.getVehicleId().toString() +"_" + cv.getLocation().toString(), Vehicle.class), cv.getLocation())
						.setType(cv.getVehicleType())
						.setEarliestStart(cv.getEarliestStartTime()).setLatestEnd(cv.getLatestEndTime())
						.build());
			}
			carrier.setCarrierCapabilities(tempCc);
			
		}		
		return carriers;
	}

	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}

}
