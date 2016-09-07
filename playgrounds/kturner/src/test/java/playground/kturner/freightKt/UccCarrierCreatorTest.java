package playground.kturner.freightKt;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.testcases.MatsimTestUtils;

import playground.kturner.freightKt.UccCarrierCreator;

/**
 * 
 * @author kturner
 *
 */
public class UccCarrierCreatorTest {
	
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	// Test, ob bei retailerNames = null alle Carrier extrahiert werden.
    @Test
    public final void testExtractCarriers1() {
   	
    	final String CARRIERS_FILE = utils.getClassInputDirectory() + "carriers.xml";
		final ArrayList<String> retailerNames = null ;
    		
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
		
		UccCarrierCreator creator = new UccCarrierCreator(carriers, null, null, null, retailerNames, null);
		Carriers extCarriers = creator.extractCarriers(carriers, retailerNames);
		
		Assert.assertTrue("gridCarrier not extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier", Carrier.class)));
		Assert.assertTrue("gridCarrier1 not extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier1", Carrier.class)));
		Assert.assertTrue("gridCarrier2 not extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier2", Carrier.class)));
		Assert.assertTrue("gridCarrier3 not extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier3", Carrier.class)));
    }
    
 // Test, ob bei retailerNames != null nur die Genannten extrahiert werden.
    @Test
    public final void testExtractCarriers2() {
   	
    	final String CARRIERS_FILE = utils.getClassInputDirectory() + "carriers.xml";
		final ArrayList<String> retailerNames = new ArrayList<String>(Arrays.asList("gridCarrier3", "gridCarrier1"));
			
		
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
		
		UccCarrierCreator creator = new UccCarrierCreator(carriers, null, null, null, retailerNames, null);
		Carriers extCarriers = creator.extractCarriers(carriers, retailerNames);
		
		Assert.assertFalse("gridCarrier extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier", Carrier.class)));
		Assert.assertTrue("gridCarrier1 not extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier1", Carrier.class)));
		Assert.assertFalse("gridCarrier2 extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier2", Carrier.class)));
		Assert.assertTrue("gridCarrier3 not extracted", extCarriers.getCarriers().containsKey(Id.create("gridCarrier3", Carrier.class)));
    }

    //Testet das Umbenennen der Fahrzeuge.
    //zum einen Erweiterung um den Depot-Link, 
    //zum anderen bei dann immer noch mehrfachem Auftreten die Erweiterung um laufenden Buchstaben.
    //Prüft dabei dann auch die wesentlichen Eigenschaften ab (location, earliestSTartTime, latestEndTime, VehicleType)
    @Test 
    public final void testRenameVehId() {

    	final String CARRIERS_FILE = utils.getClassInputDirectory() + "carriers.xml";
    	
    	final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
    	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

    	UccCarrierCreator creator = new UccCarrierCreator(carriers, null);
    	Carriers renamedVehCarriers = creator.renameVehId(carriers);
    	
    	CarrierCapabilities gridCarrierCapabilties = renamedVehCarriers.getCarriers().get(Id.create("gridCarrier", Carrier.class)).getCarrierCapabilities();
	
    	//Erstelle CarrierVehicles für die Prüfung.
		CarrierVehicle gridVehicle3_i6_0 = CarrierVehicle.Builder
				.newInstance(Id.create("gridVehicle3_i(6,0)", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)"))
				.setType(vehicleTypes.getVehicleTypes().get(Id.create("gridType03", CarrierVehicleType.class)))
				.setEarliestStart(0.0)
				.setLatestEnd(86399)
				.build();
		
		CarrierVehicle gridVehicle3_i6_0b = CarrierVehicle.Builder
				.newInstance(Id.create("gridVehicle3_i(6,0)b", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)"))
				.setType(vehicleTypes.getVehicleTypes().get(Id.create("gridType03", CarrierVehicleType.class)))
				.setEarliestStart(0.0)
				.setLatestEnd(86399)
				.build();
		
		ArrayList<Id<org.matsim.vehicles.Vehicle>> vehicleIds = new ArrayList<Id<org.matsim.vehicles.Vehicle>>();
		
		//Read all CarrierVehicleIds of gridCarrier
		for (CarrierVehicle cv : gridCarrierCapabilties.getCarrierVehicles()) {
			vehicleIds.add(cv.getVehicleId());
		}
		
		//Test, if Capabilities are correct.
		for (CarrierVehicle cv : gridCarrierCapabilties.getCarrierVehicles()) {
			Assert.assertTrue("Vehicle Id doesn't exists: "+ gridVehicle3_i6_0.getVehicleId().toString(), vehicleIds.contains(gridVehicle3_i6_0.getVehicleId()));
			if (cv.getVehicleId() == gridVehicle3_i6_0.getVehicleId()){
				Assert.assertTrue(gridVehicle3_i6_0.toString()+ "has different earliest StartTime", cv.getEarliestStartTime() == gridVehicle3_i6_0.getEarliestStartTime());
				Assert.assertTrue(gridVehicle3_i6_0.toString()+ "has different latest EndTime", cv.getLatestEndTime() == gridVehicle3_i6_0.getLatestEndTime());
				Assert.assertTrue(gridVehicle3_i6_0.toString()+ "has different location", cv.getLocation() == gridVehicle3_i6_0.getLocation());
				Assert.assertTrue(gridVehicle3_i6_0.toString()+ "has different latest EndTime", cv.getVehicleType() == gridVehicle3_i6_0.getVehicleType());
			}
			Assert.assertTrue("Vehicle Id doesn't exists: "+ gridVehicle3_i6_0b.getVehicleId().toString(), vehicleIds.contains(gridVehicle3_i6_0b.getVehicleId()));
			if (cv.getVehicleId() == gridVehicle3_i6_0b.getVehicleId()){
				Assert.assertTrue(gridVehicle3_i6_0b.toString()+ "has different earliest StartTime", cv.getEarliestStartTime() == gridVehicle3_i6_0b.getEarliestStartTime());
				Assert.assertTrue(gridVehicle3_i6_0b.toString()+ "has different latest EndTime", cv.getLatestEndTime() == gridVehicle3_i6_0b.getLatestEndTime());
				Assert.assertTrue(gridVehicle3_i6_0b.toString()+ "has different location", cv.getLocation() == gridVehicle3_i6_0b.getLocation());
				Assert.assertTrue(gridVehicle3_i6_0b.toString()+ "has different latest EndTime", cv.getVehicleType() == gridVehicle3_i6_0b.getVehicleType());
			
			}
		}
		
    }
    
    //TODO: muss noch erstellt werden!!!
    //Als Test nur für gridCarrier1. Es sind 4 Services umzuplanen: #3,#4,#6,#10
    @Test 
    public final void testCreateUCCCarrier() {

    	final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
    	final String CARRIERS_FILE = utils.getClassInputDirectory() + "carriers.xml";
    	final String ZONE_FILE = utils.getClassInputDirectory() + "LEZ.xml";
    	final String uccPrefix = "UCC-";
    	
    	// All retailer/carrier to handle in UCC-Case. (begin of CarrierId); null if all should be used.
    	final ArrayList<String> retailerNames = new ArrayList<String>(Arrays.asList("gridCarrier1"));
    	//Location of UCC
    	final ArrayList<String> uccDepotsLinkIdsString = new ArrayList<String>(Arrays.asList("j(0,5)", "j(10,5)")); 
    	ArrayList<Id<Link>> uccDepotsLinkIds = new ArrayList<Id<Link>>();	//Location of UCC
    	for (String linkId : uccDepotsLinkIdsString){
    		uccDepotsLinkIds.add(Id.createLinkId(linkId));
    	}

    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

    	//Erstellen der UCC-Carrier. Danach alle als separierte Carrier zusammen in einem Carriers-Container. 
    	UccCarrierCreator creator = new UccCarrierCreator(carriers, vehicleTypes, ZONE_FILE, uccPrefix, retailerNames, uccDepotsLinkIds);
    	creator.createSplittedUccCarrriers();
    	carriers = creator.getSplittedCarriers();
    	
    	Carrier uccCarrier = carriers.getCarriers().get(Id.create(uccPrefix+"gridCarrier1", Carrier.class));
    	//Test ob UCC-Carrier erstellt wurde: 
    	Assert.assertNotNull("UCC-Carrier fehlt", uccCarrier);

    	//Test ob UCC-Carrier die richtigen Services enthält (#3,#4,#6,#10)
    	ArrayList<String> serviceIdStringsofUccC = new ArrayList<String>();
    	for (CarrierService service : uccCarrier.getServices()){
    		serviceIdStringsofUccC.add(service.getId().toString());
    	}
    	Assert.assertTrue("Service #3 nicht in UCC", serviceIdStringsofUccC.contains("3"));
    	Assert.assertTrue("Service #4 nicht in UCC", serviceIdStringsofUccC.contains("4"));
    	Assert.assertTrue("Service #6 nicht in UCC", serviceIdStringsofUccC.contains("6"));
    	Assert.assertTrue("Service #10 nicht in UCC", serviceIdStringsofUccC.contains("10"));
    	
    	Carrier nonUccCarrier = carriers.getCarriers().get(Id.create("gridCarrier1", Carrier.class));
    	//Test ob Non-UCC-Carrier die entsprechenden Services NICHT mehr enthält (#3,#4,#6,#10)
    	ArrayList<String> serviceIdStringsofNonUccC = new ArrayList<String>();
    	for (CarrierService service : nonUccCarrier.getServices()){
    		serviceIdStringsofNonUccC.add(service.getId().toString());
    	}
    	Assert.assertFalse("Service #3 nicht nicht aus NonUcc entfernt", serviceIdStringsofNonUccC.contains("3"));
    	Assert.assertFalse("Service #4 nicht nicht aus NonUcc entfernt", serviceIdStringsofNonUccC.contains("4"));
    	Assert.assertFalse("Service #6 nicht nicht aus NonUcc entfernt", serviceIdStringsofNonUccC.contains("6"));
    	Assert.assertFalse("Service #10 nicht nicht aus NonUcc entfernt", serviceIdStringsofNonUccC.contains("10"));
    	
    	//Enthält UCC-Carrier Fahrzeuge für alle UCC-Depot-Standorte?
    	ArrayList<String> uccVehicleDepotString = new ArrayList<String>();
    	for (CarrierVehicle cv : uccCarrier.getCarrierCapabilities().getCarrierVehicles()){
    		uccVehicleDepotString.add(cv.getLocation().toString());
    	}
    	Assert.assertTrue("Depots UCC-Carrier nicht korrekt", uccVehicleDepotString.containsAll(uccDepotsLinkIdsString));
    	
    	//Und sind auch keine anderen Standort enthalten?
    	uccVehicleDepotString.removeAll(uccDepotsLinkIdsString);
    	Assert.assertTrue("UCC-Carrier enthält noch andere Depots", uccVehicleDepotString.isEmpty());
    }
    
    //Testet, ob die Services zur Belieferung der UCCs korrekt erstellt werden:
    //Richtige Menge an richtiges Depot
    @Test
    public final void testCreateServicesToUCC() {

    	final String UCC_CARRIERS_FILE = utils.getInputDirectory() + "carriers_ucc.xml";
    	final String NON_UCC_CARRIERS_FILE = utils.getInputDirectory() + "carriers_nonUcc.xml";
    	
    	Carriers uccCarriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(uccCarriers).readFile(UCC_CARRIERS_FILE) ;
    	
    	Carriers nonUccCarriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(nonUccCarriers).readFile(NON_UCC_CARRIERS_FILE) ;

    	UccCarrierCreator creator = new UccCarrierCreator(null, null, null, "UCC-", null, null);
    	Carriers nonUCCCinclSrvToUcc = creator.createServicesToUCC(uccCarriers, nonUccCarriers);
    	
    	
    	ArrayList<CarrierService> services = new ArrayList<CarrierService>();
    	for (CarrierService service : nonUCCCinclSrvToUcc.getCarriers().get(Id.create("gridCarrier1", Carrier.class)).getServices()){
    		services.add(service);
    	}    	
    	
    	//Es muss nachfrage von zwei zum Link j(0,5) und zwei zum Link j(10,5) erstellt worden sein.
    	//Da diese aktuell (15.7.15, KT) noch zu einer Einheit sind es in Summe nunmehr 10 Services
    	Assert.assertTrue("Anzahl Services nicht korrekt", services.size() == 10);
    	Assert.assertTrue("Nachfrage für UCC j(0,5) nicht korrekt", calcDemandToUCC(services, "j(0,5)") == 2);
    	Assert.assertTrue("Nachfrage für UCC j(10,5) nicht korrekt", calcDemandToUCC(services, "j(10,5)") == 2);
       }

	/**
	 * @param services
	 * @param demandLinkIdString
	 * @return Gesamtnachfrage für angegebenen Ziellink
	 */
	private int calcDemandToUCC(ArrayList<CarrierService> services, String demandLinkIdString) {
		int demandOfUcc = 0;
    	for (CarrierService service : services){
    		if (service.getLocationLinkId() == Id.createLinkId(demandLinkIdString)){
    			demandOfUcc = demandOfUcc + service.getCapacityDemand();
    		}
    	}
    	return demandOfUcc;
	}
    
}
