package freightKt.preWork;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gml.producer.GeometryTransformer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.opengis.annotation.Obligation;
import org.opengis.annotation.Specification;
import org.opengis.annotation.UML;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

class CreateUCCCarriers {

	/**
	 * @author: Kturner
	 * Ziele:
	 *  1.) Analyse der Carrier aus dem Schroeder/Liedtke-Berlin-Szenario.
	 *  2.) Erstellung eines Carrier-Files mit der Kette der größten Nachfage (Beachte, dass dabei Frozen, dry, Fresh) 
	 * 	jeweils eigene Carrier mit eigener Flotten zsammensetzung sind.) -> Hier aldi
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
	private static final String CARRIERS_2EXTRACT = "aldi" ;
	private static final String TOLL_NAME = "toll_city_kt";
	private static final String ZONE_SHAPE_NAME = "Umweltzone/Umweltzone_WGS84";
	private static final String NETWORK_SHAPE_NAME = "Umweltzone/MatsimNW_conv_Links";
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
	private static final String CARRIEROUTFILE = INPUT_DIR + CARRIERS_2EXTRACT + ".xml";
	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";
	private static final String ZONESHAPEFILE = INPUT_DIR + ZONE_SHAPE_NAME + ".shp";
	private static final String NETWORKSHAPEFILE = INPUT_DIR + NETWORK_SHAPE_NAME + ".shp";

	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));

		//Carrier-Stuff
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERFILE) ;
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPEFILE) ;

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

//		calcInfosPerCarrier(carriers);	//Step 1: Analyse der vorhandenen Carrier
		Carriers extractedCarriers = extractCarrier(carriers, CARRIERS_2EXTRACT); //Step 2a: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)		
		new CarrierPlanXmlWriterV2(extractedCarriers).write(CARRIEROUTFILE) ;
		Carriers splittedCarriers = createUCCCarrier(extractedCarriers, TOLLFILE);	//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
		new CarrierPlanXmlWriterV2(splittedCarriers).write(OUTPUT_DIR +"splittedCarrier.xml");
		
		
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

	//TODO:v Step 3: Seperate Carriers in UCC and non UCC-Carrier.
	//TODO: UCC-Carrier braucht Fahrzeuge.
	private static Carriers createUCCCarrier(Carriers carriers,
			String tollfile2) {
		
		Carriers splittedCarriers = new Carriers();
		
		// TODO Auto-generated method stub
		//Read tollfile
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
//			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
//			rpConfig.setTollLinksFile("F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/toll_distance_test_kt.xml");
			rpReader.parse(TOLLFILE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		Set<Id<Link>> tolledLinkIds = scheme.getTolledLinkIds();  //Links des MautSchematas
		Set<CarrierService> serviceToRemove= new HashSet<CarrierService>(); 
		
		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier uccCarrier = CarrierImpl.newInstance(Id.create("UCC_"+carrier.getId() , Carrier.class));;
			
			for (CarrierService service: carrier.getServices()) {
				if (tolledLinkIds.contains(service.getLocationLinkId())){	//Service liegt in der Maut-Zone (=Umweltzone)
					uccCarrier.getServices().add(service);		//Füge Service zum UCC_Carrier hinzu
					serviceToRemove.add(service);
				}
			}
			
			for (CarrierService service: serviceToRemove){ //neue Schleife, da sonst innerhalb der Schleife das Set modifiziert wird..
				carrier.getServices().remove(service);	//und lösche ihn aus dem normalen Carrier raus
			}
			splittedCarriers.addCarrier(carrier);				//bisherigen Carrier reinschreiben
			splittedCarriers.addCarrier(uccCarrier);
		}
		
		return splittedCarriers;
	}
	
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}

}
