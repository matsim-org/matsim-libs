package playground.kturner.freightKt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;


class UccCarrierCreator {

	/**
	 * @author: Kturner
	 * Erstellt auf Grundlage einer definierten Zone (aktuell über ein Mautfile)
	 * einen neuen Carrier, der zukünftig die Service an den Links der Zonendefinition bedient.
	 * Diese Services werden dann nicht mehr von Ihrem eigentlichen Carrier bedient.
	 * 
	 * Für den neuen Carrier wird mind. ein Depot erschaffen, von welchem er operiert.
	 * Diese(s) wird vom bisherige Carrier versorgt, sodass der Warenstrom immer 
	 * noch im ursprünglichen Depot beginnt.
	 * 
	 * Dem neuen Carrier werden bestimmte Elektrofahrzeugtypen zugeordnet.
	 * 
	 * UCC: Urban Consolidation Center -> transshipment center 
	 */

	private Carriers carriers ;
	private CarrierVehicleTypes vehicleTypes  ;	
	private String zonefile ;
	private String uccC_prefix;		//PRefix mit denen UCC-CarrierIds beginnen (Rest identisch mit CarrierId).
	private ArrayList<String> retailerNames = new ArrayList<String>();
	private ArrayList<Id<Link>> uccDepotsLinkIds = new ArrayList<Id<Link>>();;	//Location of UCC


	double timeShiftUccOpeningTime;
	double timeShiftUccClosingTime;	

	private Carriers extractedCarriers;			//Only Carriers of given Retailers
	private Carriers splittedCarriers;			//Carriers splitted into UCC- and nonUCC-Carriers.

	/**
	 * Um diese Zeit (in Sekunden) wird das UCC eher öffnen, als der Kunde frühestens beliefert werden soll.
	 * @param uccOpeningTime
	 */
	void settimeShiftUccOpeningTime(double uccOpeningTime) {
		this.timeShiftUccOpeningTime = uccOpeningTime;
	}

	/**
	 * Um diese Zeit (in Sekunden) wird das UCC später schließen, als der letzte Kunde Kunde spätestens beliefert werden soll.
	 * @param uccOpeningTime
	 */
	void setTimeShiftUccClosingTime(double uccClosingTime) {
		this.timeShiftUccClosingTime = uccClosingTime;
	}

	Carriers getExtractedCarriers() {
		return extractedCarriers;
	}

	Carriers getSplittedCarriers() {
		return splittedCarriers;
	}


	/**
	 * Constructor, no time-shifts for UCC
	 * @param carriers: carriers to be handled
	 * @param vehicleTypes: vehicleTypes for solving the problem (must include the vehTypes of later used UCCcarriers)
	 * @param zonefile: path of zonefile -> Services at links defined here were moved to UccCarrier
	 * @param uccC_prefix: prefix of UccCarrier-Id (<UccC_prefix><CarrierName>)
	 * @param retailerNames: Array of all retailer/carrier to extract. (begin of CarrierId)
	 * @param uccDepotsLinkIds: locations at which UCCs were created
	 */
	UccCarrierCreator(Carriers carriers, CarrierVehicleTypes vehicleTypes, 
			String zonefile,  String uccC_prefix,
			ArrayList<String> retailerNames, ArrayList<Id<Link>> uccDepotsLinkIds) {
		this.zonefile = zonefile;
		this.carriers = carriers;
		this.vehicleTypes = vehicleTypes;
		this.uccC_prefix = uccC_prefix;
		this.retailerNames = retailerNames;
		this.uccDepotsLinkIds = uccDepotsLinkIds;
		timeShiftUccOpeningTime = 0.0;	
		timeShiftUccClosingTime = 0.0;	
	}

	/**
	 * Constructor 
	 * @param carriers: carriers to be handled
	 * @param vehicleTypes: vehicleTypes for solving the problem (must include the vehTypes of later used UCCcarriers)
	 * @param zonefile: path of zonefile -> Services at links defined here were moved to UccCarrier
	 * @param uccC_prefix: prefix of UccCarrier-Id (<UccC_prefix><CarrierName>)
	 * @param retailerNames: Array of all retailer/carrier to extract. (begin of CarrierId)
	 * @param uccDepotsLinkIds: locations at which UCCs were created
	 * @param uccOpeningTime: OpeningTime for the UCC (= earliest start of vehicles delivering from UCC)
	 * @param uccClosingTime: ClosingTime for the UCC (= latest return for vehicles delivering from UCC)
	 */
	UccCarrierCreator(Carriers carriers,	CarrierVehicleTypes vehicleTypes, 
			String zonefile,  String uccC_prefix,
			ArrayList<String> retailernames, ArrayList<Id<Link>> uccDepotsLinkIds,
			double uccOpeningTime, double uccClosingTime) {
		this.zonefile = zonefile;
		this.carriers = carriers;
		this.vehicleTypes = vehicleTypes;
		this.uccC_prefix = uccC_prefix;
		this.retailerNames = retailernames;
		this.uccDepotsLinkIds = uccDepotsLinkIds;
		this.timeShiftUccOpeningTime = uccOpeningTime;
		this.timeShiftUccClosingTime = uccClosingTime;
	}

	/**
	 * Reduced constructor if only method "extractCarriers(Carriers carriers, String[] retailerNames)" 
	 * is used. Otherwise use other constructor.
	 * @param carriers: carriers to be handled
	 * @param retailerNames: Array of all retailer/carrier to extract. (begin of CarrierId)
	 */
	UccCarrierCreator(Carriers carriers, ArrayList<String> retailerNames) {
		this.carriers = carriers;
		this.retailerNames = retailerNames;
	}

	//Standard constructor (empty)
	UccCarrierCreator() {
	}

	void createSplittedUccCarrriers() {
		//Step1 Analysis of Carriers: not done here any more....
		//Step 2: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)
		extractedCarriers = extractCarriers(carriers, retailerNames); 		
		//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
		splittedCarriers = createUCCCarrier(extractedCarriers, vehicleTypes, 
				zonefile, uccDepotsLinkIds, timeShiftUccOpeningTime, timeShiftUccClosingTime);	
		//Step4: VehId je Carrier einzigartig machen, da sonst weitere Vorkommen 
		//		ignoriert werden (und somit nicht alle Depots genutzt werden).
		splittedCarriers = renameVehId(splittedCarriers); 				

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
	Carriers extractCarriers(Carriers carriers, ArrayList<String> retailerNames) {
		if (retailerNames == null) {
			return carriers;
		}
		String carrierId;
		Carriers tempCarriers = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();
			for (String retailerName : retailerNames)
				if (carrierId.startsWith(retailerName)){	//Carriername beginnt mit Retailername
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
	private Carriers createUCCCarrier(Carriers carriers, CarrierVehicleTypes vehicleTypes, 
			String zonefile, List<Id<Link>> uccDepotsLinkIds2, double uccEarlierOpeningTime, 
			double uccLaterClosingTime) {

		// Carrierfile, welches beide Carrier enthält: sowohl UCC, als auch non UCC
		Carriers splittedCarriers = new Carriers(); 

		//Read zonefile
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			rpReader.readFile(zonefile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Set<Id<Link>> tolledLinkIds = scheme.getTolledLinkIds();  //Link-Ids des MautSchemas
		//Liste der zum UCC-Carrier übertragenen Services -> wird später aus normalen Carrier entfernt
		Set<CarrierService> serviceToRemove= new HashSet<CarrierService>(); 	

		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier uccCarrier = CarrierImpl.newInstance(Id.create(uccC_prefix + carrier.getId() , Carrier.class));

			for (CarrierService service: carrier.getServices()) {
				if (tolledLinkIds.contains(service.getLocationLinkId())){	//Service liegt in der Maut-Zone (=Umweltzone)
					uccCarrier.getServices().add(service);		//Füge Service zum UCC_Carrier hinzu
					serviceToRemove.add(service);
				}
			}

			//neue Schleife, da sonst innerhalb der Schleife das Set modifiziert wird..
			for (CarrierService service: serviceToRemove){ 
				carrier.getServices().remove(service);	//und lösche ihn aus dem normalen Carrier raus
			}

			//bisherigen Carrier reinschreiben, darf auch ohne Service sein, 
			//da ggf während Laufzeit nachfrage erhält (Depot -> UCC).
			splittedCarriers.addCarrier(carrier); 

			//Fahrzeug für jedes Lieferzeitfenster erstellen (analog Schröder/Liedtke, da jsprit keine Wartezeiten berücksichtigt.
			if (!uccCarrier.getServices().isEmpty()){		//keinen UCC ohne Nachfrage übernehmen.
				ArrayList<TimeWindow> timeWindows = calcTimeWindows(uccCarrier);
				System.out.println("Zeitfenster: " + timeWindows.toString());
				for (TimeWindow tw : timeWindows) {
					addVehicles(uccCarrier, vehicleTypes, uccDepotsLinkIds2, 
							Math.max(0, tw.getStart() -uccEarlierOpeningTime), Math.min(24*3500, tw.getEnd() +uccLaterClosingTime)); 
				}
				checkCapacity(uccCarrier, vehicleTypes);
				uccCarrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
				splittedCarriers.addCarrier(uccCarrier);
			}


			//			// Fahrzeug steht über maximale Zeitspanne zur Verfügung.
			//			if (!uccCarrier.getServices().isEmpty()){		//keinen UCC ohne Nachfrage übernehmen.
			//				TimeWindow tw = calcMaxRangeOfStartTimeWindow(uccCarrier);
			//				//Depotzeiten: Zeitfenster +- 1h
			//				addVehicles(uccCarrier, vehicleTypes, uccDepotsLinkIds2, 
			//						Math.max(0, tw.getStart() -uccEarlierOpeningTime), Math.min(24*3500, tw.getEnd() +uccLaterClosingTime)); 
			//				uccCarrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
			//				splittedCarriers.addCarrier(uccCarrier);
			//			}
		}
		return splittedCarriers;
	}


	/**Überprüft, ob die Services auf Grund der Kapazität der zur Verfügung stehenden Flotte bedient werden kann.
	 * Falls nicht, wird der betreffende Service durch mehrere kleinere ersetzt, die sich an der größten 
	 * zur Verfügugn stehenden Fzg-Kapazität orientiert.
	 * @param carrier
	 * @param vehicleTypes
	 */
	private void checkCapacity(Carrier carrier, CarrierVehicleTypes vehicleTypes) {
		Set<CarrierService> servicesToRemove= new HashSet<CarrierService>(); 
		Set<CarrierService> servicesToAdd= new HashSet<CarrierService>(); 

		for (CarrierService service : carrier.getServices()){
			//Alphabetsliste erstellen
			List<Character> alph = new ArrayList<Character>() ;
			for(char c='a'; c<='z'; c++) {
				alph.add(c);
			}

			int totalServiceDemand = service.getCapacityDemand();
			double totalServiceDuration = service.getServiceDuration();

			//Calc max VehicleCapacity
			int maxVehicleCapacity = -1;
			for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles()) {
				if (maxVehicleCapacity < vehicle.getVehicleType().getCarrierVehicleCapacity()){
					maxVehicleCapacity = vehicle.getVehicleType().getCarrierVehicleCapacity();
				}
			}
			//			System.out.println("MaxVehicleCapacity: " + maxVehicleCapacity);
			// Neuerstellung geteilter Services
			if (totalServiceDemand > maxVehicleCapacity){
				int demandtoAssignLeft = totalServiceDemand;
				//				System.out.println("Totel Service Demand: " + totalServiceDemand);
				//				System.out.println("Demand2AssignLeft1: " + demandtoAssignLeft);
				int numberOfNewServices = 1;
				//Assign with heighest deliverable Capacity
				while ((demandtoAssignLeft / maxVehicleCapacity) > 0.){
					//					System.out.println("AssignLeft/vehCap: "+ demandtoAssignLeft / maxVehicleCapacity);
					int assignDemand = maxVehicleCapacity;
					//					System.out.println("AssignDemand1: " + assignDemand);
					Id<CarrierService> id = Id.create(service.getId().toString() + "_" + alph.get(numberOfNewServices-1), CarrierService.class);
					CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(id, service.getLocationLinkId());
					serviceBuilder.setCapacityDemand(assignDemand)
					.setServiceStartTimeWindow(service.getServiceStartTimeWindow())
					.setServiceDuration(totalServiceDuration*assignDemand/totalServiceDemand); 
					servicesToAdd.add(serviceBuilder.build());
					demandtoAssignLeft = demandtoAssignLeft - assignDemand;
					//					System.out.println("Demand2AssignLeft2: " + demandtoAssignLeft);
					//					System.out.println("AssignLeft/vehCap2: "+ demandtoAssignLeft / maxVehicleCapacity);
					numberOfNewServices++;
				}
				//Assign Rest
				if 	(demandtoAssignLeft != 0){
					Id<CarrierService> id = Id.create(service.getId().toString() + "_" + alph.get(numberOfNewServices-1), CarrierService.class);
					CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(id, service.getLocationLinkId());
					serviceBuilder.setCapacityDemand(demandtoAssignLeft)
					.setServiceStartTimeWindow(service.getServiceStartTimeWindow())
					.setServiceDuration(totalServiceDuration*demandtoAssignLeft/totalServiceDemand);
					servicesToAdd.add(serviceBuilder.build());
				}
				servicesToRemove.add(service);
			}
		}

		//neue Schleife, da sonst innerhalb der Schleife das Set modifiziert wird..
		for (CarrierService service: servicesToAdd){ 
			carrier.getServices().add(service);
		}

		//neue Schleife, da sonst innerhalb der Schleife das Set modifiziert wird..
		for (CarrierService service: servicesToRemove){ 
			carrier.getServices().remove(service);	//und lösche ihn aus dem normalen Carrier raus
		}
	}

	//TODO: Absichern, dass zu erstellende VehicleType auch in VehicleTypes vorhanden sind! 
	/*
	 * Step3b: Elektro-Fahrzeug-Typen den UCC zuordnen
	 * Dabei gilt, dass frozen nur über den light8telectro_frozen verfügt  und alle anderen
	 * light8telectro und medium18telectro verfügen. Es werden Fahrzeuge für jedes Depot angelegt.
	 */
	private void addVehicles(Carrier carrier, CarrierVehicleTypes vehicleTypes, 
			List<Id<Link>> uccDepotsLinkIds, double uccOpeningTime, double uccClosingTime) {

		if (carrier.getId().toString().endsWith("TIEFKUEHL")){
			for (Id<Link> linkId : uccDepotsLinkIds ){
				carrier.getCarrierCapabilities().getCarrierVehicles()
				.add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro_frozen", Vehicle.class), linkId)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro_frozen", VehicleType.class)))
						.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
						.build());
			}
		} else {
			for (Id<Link> linkId : uccDepotsLinkIds ){
				carrier.getCarrierCapabilities().getCarrierVehicles()
				.add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro", Vehicle.class), linkId)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro", VehicleType.class)))
						.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
						.build());

				carrier.getCarrierCapabilities().getCarrierVehicles()
				.add(CarrierVehicle.Builder.newInstance(Id.create("medium18telectro", Vehicle.class), linkId)
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

			//da Änderung der vorhandenen Fahrzeuge sonst nicht ging, Umweg über 
			//temporären neuen Carrier & setzen der Eigenschaften.
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
					//TODO: Abischerung gegen Leerlaufen des Alphabet-Arrays  überprüfen/ausprobieren (mehr als 26 mal verwendet erstellen.)
					Assert.assertTrue("No more chars to add to vehicleId",  nuOfVehPerId.get(vehIdwLink) <= alph.size());
					newVehId = vehIdwLink + alph.get(nuOfVehPerId.get(vehIdwLink)-1);
				}	

				//Vehicle neu erstellen, da setVehicleId nicht verfügbar.
				//Dabei eindeutigen Buchstaben für jede VehId-DepotLink-Kombination einfügen

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

	Carriers createServicesToUCC(Carriers uccCarriers,	Carriers nonUccCarriers) {
		//Services aus den UCC für die Non-UCC erstellen -> Funktioniert grundsätzlich, KT 02.05.15
		for (Carrier uccC : uccCarriers.getCarriers().values()){
			for (Carrier nonUccC : nonUccCarriers.getCarriers().values()){
				//TODO: Sicherstellen, dass jeder Service auch erstellt wird--> Sicherheitsabfrage, ansonsten Fehler erzeugen!	
				if (uccC.getId().toString().equals(uccC_prefix+nonUccC.getId().toString())){

					//Zählt nachfrage an UCC-LinkID
					Map<Id<Link>, Integer> demandAtUCC = new HashMap<Id<Link>, Integer>();
					//für die einzelnen Touren die Nachfrage an den einzelnen Depots zählen
					for (ScheduledTour st : uccC.getSelectedPlan().getScheduledTours()){
						Id<Link> uccLocationId = st.getVehicle().getLocation();
						int demand = 0;

						for (TourElement tourElement : st.getTour().getTourElements()){
							if(tourElement instanceof ServiceActivity){
								ServiceActivity serviceAct = (ServiceActivity)tourElement;
								demand += serviceAct.getService().getCapacityDemand();
							}
						}

						if (demandAtUCC.containsKey(uccLocationId)){
							demandAtUCC.put(uccLocationId, demandAtUCC.get(uccLocationId)+demand);  
						} else  {
							demandAtUCC.put(uccLocationId, demand);
						}
					}


					//TODO: Sicherstellen (assert), dass bisher kein Service mit UCC_Prefix existiert, da sonst doppelte Einträge -> Fehler
					//TODO: Absicherung einbauen, dass UCC von den Zeitfenstern her beliefert werden kann. -> ggf Zeitfenster anpassen, an 
					//neue Services erstellen des nonUccC zum Depot des uccC.
					for (Id<Link> linkId : demandAtUCC.keySet()){				//Nun erstelle die ganzen Services
						for (int i = 1; i<=demandAtUCC.get(linkId); i++){
							double earliestVehDepUCC = calcEarliestDep(uccC ,linkId);	
							CarrierService.Builder csBuilder = CarrierService.Builder
									.newInstance(Id.create("to_"+uccC_prefix+linkId.toString()+"_"+i, CarrierService.class), linkId)
									//Jeder Service nur Nachfrage = 1, damit Fzg Aufteilung frei erfolgen kann
									.setCapacityDemand(1)		
									.setServiceDuration(60)	//60sec = 1min
									// Innerhalb der ersten 2 Stunden nach Öffnungszeit soll die Ware dort ankommen 
									//(Da aus Gründen der Vergleichbarkeit bisher die Öffnungszeiten der Hauptdepots nicht verändert werden)
									.setServiceStartTimeWindow(TimeWindow.newInstance(
											Math.max(0, earliestVehDepUCC), Math.max(0, earliestVehDepUCC +7200 ))); 
							nonUccC.getServices().add(csBuilder.build());
						}	
					}

				} //end if
			}
		}
		return nonUccCarriers;	
	}

	//Früheste Abfahrt eines Fahrzeuges des Carriers vom angebenenen Depot
	//	TODO: Asserts gegen Null testen ;)
	private double calcEarliestDep(Carrier carrier, Id<Link> linkId) {
		Assert.assertNotNull("linkId must not be null!: " + linkId);
		double earliestDepTime = 24*3600.0; 	//24 Uhr 
		for (CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()) {
			Assert.assertNotNull("DepotLocation must not be null!: " + cv.toString(), cv.getLocation());
			if (cv.getLocation() == linkId){
				Assert.assertNotNull("aerliestDepartureTime must not be null!: " + cv.toString(), cv.getEarliestStartTime());
				if (cv.getEarliestStartTime() < earliestDepTime) {
					earliestDepTime = cv.getEarliestStartTime();
				} 
			}
		}
		return earliestDepTime;
	}

	//Früheste Abfahrt eines Fahrzeuges des Carriers 
	//	TODO: Test gegen Null (cv.getEarliestStartTime) testen ;)
	private double calcEarliestDep(Carrier carrier) {
		double earliestDepTime = 24*3600.0; 	//24 Uhr 
		for (CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()) {
			Assert.assertNotNull("aerliestDepartureTime must not be null!: " + cv.toString(), cv.getEarliestStartTime());
			if (cv.getEarliestStartTime() < earliestDepTime) {
				earliestDepTime = cv.getEarliestStartTime();
			} 
		}
		return earliestDepTime;
	}

	/**
	 * Gibt die maximale Spanne (früheste Startzeit, späteste Endzeit) 
	 * der Service-Zeiten des Carriers an, zu denen ein Service begonnen werden darf.
	 */
	private TimeWindow calcMaxRangeOfStartTimeWindow(Carrier carrier){
		double earliestServiceStartBeginTime = 24*3600.0; 	//24 Uhr 
		double latestServiceStartEndTime = 0.0;
		for (CarrierService cs : carrier.getServices()) {
			if (cs.getServiceStartTimeWindow().getStart() < earliestServiceStartBeginTime) {
				earliestServiceStartBeginTime = cs.getServiceStartTimeWindow().getStart();
			}
			if (cs.getServiceStartTimeWindow().getEnd() > latestServiceStartEndTime) {
				latestServiceStartEndTime = cs.getServiceStartTimeWindow().getEnd();
			}
		}
		Assert.assertTrue(earliestServiceStartBeginTime <= latestServiceStartEndTime);
		TimeWindow timeWindow = TimeWindow.newInstance(earliestServiceStartBeginTime, latestServiceStartEndTime);
		return timeWindow;
	}

	/**
	 * Gibt die verschiedenenZeitfenster (früheste Startzeit, späteste Endzeit) 
	 * der Service-Zeiten des Carriers an, zu denen ein Service begonnen werden darf.
	 */
	private ArrayList<TimeWindow> calcTimeWindows(Carrier carrier){
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		for (CarrierService cs : carrier.getServices()) {
			double startTime = cs.getServiceStartTimeWindow().getStart();
			double endTime = cs.getServiceStartTimeWindow().getEnd();
			Assert.assertTrue(startTime < endTime);
			TimeWindow tw = TimeWindow.newInstance(startTime, endTime);
			if (!timeInTimeWindow(timeWindows, tw)) {
				timeWindows.add(tw);
				System.out.println("added TimeWindow: " + tw.toString());
			} else System.out.println("Nicht hinzugefügt");
		}
		return timeWindows;
	}

	private boolean timeInTimeWindow(ArrayList<TimeWindow> timeWindows ,TimeWindow timewindow){
		for (TimeWindow tw : timeWindows){
			if (tw.getStart() == timewindow.getStart() && tw.getEnd() == timewindow.getEnd()) {
				System.out.println("TW ist bereits enthalten");
				return true;
			}
		}
		System.out.println("TW bisher nicht enthalten");
		return false;
	}
}
