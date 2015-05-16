package freightKt.prepareCarrier;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.freight.carrier.Carrier;
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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

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
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/Umweltzone/" ;
	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/";

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "network" ;
	private static final String VEHTYPES_NAME = "vehicleTypes" ;
	private static final String CARRIERS_NAME = "carrierLEH_v2_withFleet" ;
	private static final String CARRIERS_OUT_NAME = "carrier_1Retailer" ;
	private static final String TOLL_NAME = "toll_distance_test_kt";
	private static final String SHAPE_NAME = "Umweltzone";
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
	private static final String SHAPEFILE = INPUT_DIR + SHAPE_NAME + ".shp";
	
	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));
				
		//Network-Stuff
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(NETFILE);
        Scenario scenario = ScenarioUtils.loadScenario(config);
		
		convertNet2Shape2(scenario.getNetwork(), OUTPUT_DIR); //Step 1: Netzwerk zusammenbringen -> Richtige Konvertierung gefunden ;-)
		extractTollFile(scenario.getNetwork(), SHAPEFILE ,OUTPUT_DIR);
		
	
		//Carrier-Stuff
//		Carriers carriers = new Carriers() ;
//		new CarrierPlanXmlReaderV2(carriers).read(CARRIERFILE) ;
//		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
//		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPEFILE) ;
//		
//		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		
//		calcInfosPerCarrier(carriers);	//Step 1: Analyse der vorhandenen Carrier
//		extractCarrier(carriers, "aldi"); //Step 2: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)		

		
		System.out.println("### ENDE ###");
	}

	private static void extractTollFile(Network network, String shapefile2,
			String outputDir) {
		// TODO Auto-generated method stub
		
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
	
	//Convert coordinates of NW from Gauß-Krueger (Potsdam) [EPSG: 31468] to coordinate-format of low-emission-zone WGS84 [EPSG:4326]
	private static Network convertCoordinates(Network net){
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:4326");
		
		for(Node node : net.getNodes().values()){
			Coord newCoord = ct.transform(node.getCoord());
			((NodeImpl)node).setCoord(newCoord);
		}
		
		return net;
	}

	@SuppressWarnings("unchecked")
	private static void convertNet2Shape2(Network network, String outputDir){
		
		network = convertCoordinates(network);
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:4326"); 
		

		@SuppressWarnings("rawtypes")
		Collection features = new ArrayList();
        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
                setCrs(crs).
                setName("link").
                addAttribute("ID", String.class).
                addAttribute("fromID", String.class).
                addAttribute("toID", String.class).
                addAttribute("length", Double.class).
                addAttribute("type", String.class).
                addAttribute("capacity", Double.class).
                addAttribute("freespeed", Double.class).
                create();

        for (Link link : network.getLinks().values()) {
            Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
            Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
            Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
            SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
                    new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), ((LinkImpl)link).getType(), link.getCapacity(), link.getFreespeed()}, null);
            features.add(ft);
        }   
        ShapeFileWriter.writeGeometries(features, outputDir+"MatsimNW_conv_Links.shp");
       
        
        features.clear();
       
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("nodes").
                addAttribute("ID", String.class).
                create();

        for (Node node : network.getNodes().values()) {
            SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
            features.add(ft);
        }
        ShapeFileWriter.writeGeometries(features, outputDir+"MatsimNW_conv_Links.shp");
	}
	
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + "erstellt: "+ file.mkdirs());	
	}

}
