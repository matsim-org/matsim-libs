package freightKt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

//import jsprit.core.problem.vehicle.VehicleType;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;


class UccCarrierCreator {

	/**
	 * @author: Kturner
	 * TODO: Dokumentieren
	 * UCC: Urban Consolidation Center -> transshipment center 
	 */



	private Carriers carriers ;
	private CarrierVehicleTypes vehicleTypes  ;	
	private String tollfile ;
	private String uccC_prefix;		//PRefix mit denen UCC-CarrierIds beginnen (Rest identisch mit CarrierId).
	private String[] retailerNames;
	private List<Id<Link>> uccDepotsLinkIds = new ArrayList<Id<Link>>();;	//Location of UCC


	double uccOpeningTime;
	double uccClosingTime;	

	private Carriers extractedCarriers;			//Only Carriers of given Retailers
	private Carriers splittedCarriers;			//Carriers splitted into UCC- and nonUCC-Carriers.

	void setUccOpeningTime(double uccOpeningTime) {
		this.uccOpeningTime = uccOpeningTime;
	}

	void setUccClosingTime(double uccClosingTime) {
		this.uccClosingTime = uccClosingTime;
	}
	
	Carriers getExtractedCarriers() {
		return extractedCarriers;
	}

	Carriers getSplittedCarriers() {
		return splittedCarriers;
	}


	/**
	 * Constructor, sets uccOpeningTime: 08:00:00 and uccClosingTime: 21:00:00 
	 * @param carriers: carriers to be handled
	 * @param vehicleTypes: vehicleTypes for solving the problem (must include the vehTypes of later used UCCcarriers)
	 * @param tollfile: path of tollfile -> Services at links defined here were moved to UccCarrier
	 * @param uccC_prefix: prefix of UccCarrier-Id (<UccC_prefix><CarrierName>)
	 * @param retailerNames: Array of all retailer/carrier to extract. (begin of CarrierId)
	 * @param uccDepotsLinkIds: locations at which UCCs were created
	 */
	UccCarrierCreator(Carriers carriers,	CarrierVehicleTypes vehicleTypes, 
			String tollfile,  String uccC_prefix,
			String[] retailerNames, List<Id<Link>> uccDepotsLinkIds) {
		this.tollfile = tollfile;
		this.carriers = carriers;
		this.vehicleTypes = vehicleTypes;
		this.uccC_prefix = uccC_prefix;
		this.retailerNames = retailerNames;
		this.uccDepotsLinkIds = uccDepotsLinkIds;
		uccOpeningTime = 8*3600.0;	// 08:00:00 Uhr
		uccClosingTime = 21*3600.0;	// 21:00:00 Uhr
	}

	/**
	 * Constructor 
	 * @param carriers: carriers to be handled
	 * @param vehicleTypes: vehicleTypes for solving the problem (must include the vehTypes of later used UCCcarriers)
	 * @param tollfile: path of tollfile -> Services at links defined here were moved to UccCarrier
	 * @param uccC_prefix: prefix of UccCarrier-Id (<UccC_prefix><CarrierName>)
	 * @param retailerNames: Array of all retailer/carrier to extract. (begin of CarrierId)
	 * @param uccDepotsLinkIds: locations at which UCCs were created
	 * @param uccOpeningTime: OpeningTime for the UCC (= earliest start of vehicles delivering from UCC)
	 * @param uccClosingTime: ClosingTime for the UCC (= latest return for vehicles delivering from UCC)
	 */
	UccCarrierCreator(Carriers carriers,	CarrierVehicleTypes vehicleTypes, 
			String tollfile,  String uccC_prefix,
			String[] retailerNames, List<Id<Link>> uccDepotsLinkIds,
			double uccOpeningTime, double uccClosingTime) {
		this.tollfile = tollfile;
		this.carriers = carriers;
		this.vehicleTypes = vehicleTypes;
		this.uccC_prefix = uccC_prefix;
		this.retailerNames = retailerNames;
		this.uccDepotsLinkIds = uccDepotsLinkIds;
		this.uccOpeningTime = uccOpeningTime;
		this.uccClosingTime = uccClosingTime;
	}
	
	/**
	 * Reduced constructor if only method "extractCarriers(Carriers carriers, String[] retailerNames)" 
	 * is used. Otherwise use other constructor.
	 * @param carriers: carriers to be handled
	 * @param retailerNames: Array of all retailer/carrier to extract. (begin of CarrierId)
	 */
	UccCarrierCreator(Carriers carriers, String[] retailerNames) {
		this.carriers = carriers;
		this.retailerNames = retailerNames;
	}
	
	//Standard constructor (empty)
	UccCarrierCreator() {
	}

	 void createSplittedUccCarrriers() {
		//Step1 Analysis of Carriers: not done here any more....
		extractedCarriers = extractCarriers(carriers, retailerNames); //Step 2: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)		
		splittedCarriers = createUCCCarrier(extractedCarriers, vehicleTypes, tollfile, uccDepotsLinkIds, uccOpeningTime, uccClosingTime);	//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
		splittedCarriers = renameVehId(splittedCarriers); 				//Step4: VehId je Carrier einzigartig machen, da sonst weitere Vorkommen ignoriert werden (und somit nicht alle Depots genutzt werden).

		System.out.println("### ENDE: UCCCarriers.run ###");
	}

	//Step 1: Analyse der Carrier hier entfernt und in Package PreWork gelassen.

	/** (Step2)
	 * Extrahieren einzelner Retailer (alle, die mit dem RetailerNamen beginnen)
	 * @param carriers
	 * @param retailerNames:  Array of all retailer/carrier to extract. (begin of CarrierId)
	 * @return carriers with Id starting with retailerName or 
	 * 			if retailerNames == null the unmodified carriers.
	 */
	Carriers extractCarriers(Carriers carriers, String[] retailerNames) {
		if (retailerNames == null) {
			return carriers;
		}
		String carrierId;
		Carriers tempCarriers = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();
			for (String retailerName : retailerNames)
				if (carrierId.startsWith(retailerName)){			//Carriername beginnt mit Retailername
					tempCarriers.addCarrier(carrier);
				}
		}
		return tempCarriers;
	}

	/*Step3: Nachfrage auf Carrier UCC und normal aufteilen.
	 * Dabei wird für jeden Carrier, der Nachfrage an bemautetem Gebiet hat ein neuer Carrier(<UccC_prefix><CarrierName>) erstellt und 
	 * der Service entsprechend vom bisherigen in den UCC-Carrier verlegt.
	 * Sollte der ursprüngliche Carrier danach keine Nachfrage mehr haben, so bleibt er erhalten, da er später noch 
	 * die UCC beliefern muss.
	 */
	Carriers createUCCCarrier(Carriers carriers, CarrierVehicleTypes vehicleTypes, String tollfile, List<Id<Link>> uccDepotsLinkIds2, double uccOpeningTime, double uccClosingTime) {

		Carriers splittedCarriers = new Carriers(); // Carrierfile, welches beide Carrier enthält: sowohl UCC, als auch non UCC

		//Read tollfile
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			rpReader.parse(tollfile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Set<Id<Link>> tolledLinkIds = scheme.getTolledLinkIds();  //Link-Ids des MautSchemas
		Set<CarrierService> serviceToRemove= new HashSet<CarrierService>(); 	//Liste der zum UCC-Carrier übertragenen Services -> wird später aus normalen Carrier entfernt

		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier uccCarrier = CarrierImpl.newInstance(Id.create(uccC_prefix + carrier.getId() , Carrier.class));

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
				addVehicles(uccCarrier, vehicleTypes, uccDepotsLinkIds2, uccOpeningTime, uccClosingTime);
				uccCarrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
				splittedCarriers.addCarrier(uccCarrier);
			}
		}

		return splittedCarriers;
	}


	//TODO: Öffnungszeiten UCC ausgliedern?
	//TODO: Absichern, dass zu erstellende VehicleType auch in VehicleTypes vorhanden sind! 
	/*
	 * Step3b: Elektro-Fahrzeug-Typen den UCC zuordnen
	 * Dabei gilt, dass frozen nur über den light8telectro_frozen verfügt  und alle anderen
	 * light8telectro und medium18telectro verfügen. Es werden Fahrzeuge für jedes Depot angelegt.
	 */
	void addVehicles(Carrier Carrier, CarrierVehicleTypes vehicleTypes, List<Id<Link>> uccDepotsLinkIds, double uccOpeningTime, double uccClosingTime) {

		if (Carrier.getId().toString().endsWith("TIEFKUEHL")){

			for (Id<Link> linkId : uccDepotsLinkIds ){
				Carrier.getCarrierCapabilities().getCarrierVehicles().add( CarrierVehicle.Builder.newInstance(Id.create("light8telectro_frozen", Vehicle.class), linkId)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro_frozen", VehicleType.class)))
						.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
						.build());
			}

		} else {

			for (Id<Link> linkId : uccDepotsLinkIds ){
				Carrier.getCarrierCapabilities().getCarrierVehicles().add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro", Vehicle.class), linkId)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro", VehicleType.class)))
						.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
						.build());

				Carrier.getCarrierCapabilities().getCarrierVehicles().add(CarrierVehicle.Builder.newInstance(Id.create("medium18telectro", Vehicle.class), linkId)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("medium18telectro", VehicleType.class)))
						.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
						.build());

			}

		}

	}

	/* Step4: 
	 * VehicleId je Carrier um Location erweitern, da sonst weitere Vorkommen auf Grund gleicher VehicleId ingnoriert werden 
	 * und somit nicht alle Depots genutzt werden.
	 */
	Carriers renameVehId(Carriers carriers) {
		//Alphabetsliste erstellen
		List<Character> alph = new ArrayList<Character>() ;
		for(char c='a'; c<='z'; c++) {
			alph.add(c);
		}

		for (Carrier carrier : carriers.getCarriers().values()){
			//zählt mit, wie oft Id für diesen Carrier vergeben wurde
			Map<String, Integer> nuOfVehPerId = new TreeMap<String, Integer>();
			
			//da Änderung der vorhanden Fahrzeuge sonst nicht ging, Umweg über temporären neuen Carrier & setzen der Eigenschaften.
			CarrierCapabilities tempCc = CarrierCapabilities.newInstance();
			tempCc.setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
			for (CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
				String vehIdwLink = cv.getVehicleId().toString() + "_" + cv.getLocation().toString();
				String newVehId;
				if (!nuOfVehPerId.containsKey(vehIdwLink)){
					nuOfVehPerId.put(vehIdwLink, 1);
					newVehId = vehIdwLink;
				} else {
					nuOfVehPerId.put(vehIdwLink, nuOfVehPerId.get(vehIdwLink) + 1);
					newVehId = vehIdwLink + alph.get(nuOfVehPerId.get(vehIdwLink)-1);
				}	
				
				//Vehicle neu erstellen, da setVehicleId nicht verfügbar. Dabei eindeutigen Buchstaben für jede VehId-DepotLink-Kombination einfügen
				//TODO: Abischerung gegen Leerlaufen des Alphabet-Arrays  (mehr als 26 mal verwendet erstellen.)
				tempCc.getCarrierVehicles().add(CarrierVehicle.Builder
						.newInstance(Id.create(newVehId, Vehicle.class), cv.getLocation())
						.setType(cv.getVehicleType())
						.setEarliestStart(cv.getEarliestStartTime()).setLatestEnd(cv.getLatestEndTime())
						.build());
			}
			carrier.setCarrierCapabilities(tempCc); //Zurückschreiben des neuen Carriers

		}		
		return carriers;
	}


}
