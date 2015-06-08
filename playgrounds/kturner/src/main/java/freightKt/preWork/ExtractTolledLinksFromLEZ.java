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

class ExtractTolledLinksFromLEZ {

	/**
	 * @author: Kturner
	 * Extrahiert die Link-Ids eines Netzwerkes, welche Innerhalb einer Zone (Hier Umweltzone - LowEmissionZone) liegen und gibt diese als Textdatei aus.
	 * TODO: TollFile direkt aus den Links erstellen. Aktuell wird die LinkListe manuell kopiert.
	 */

	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/Case_KT/" ;

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "network" ;
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
	//	private static final String TOLL_NAME = "grid-tollCordon";
	//	//Ende Namesdefinition Grid


	private static final String NETFILE = INPUT_DIR + NETFILE_NAME + ".xml" ;
	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";
	private static final String ZONESHAPEFILE = INPUT_DIR + ZONE_SHAPE_NAME + ".shp";
	private static final String NETWORKSHAPEFILE = INPUT_DIR + NETWORK_SHAPE_NAME + ".shp";

	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));

		//Network-Stuff
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETFILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		convertNet2Shape(scenario.getNetwork(), OUTPUT_DIR); //Step 1: Netzwerk zusammenbringen -> Richtige Konvertierung gefunden ;-)
		extractTollLinks(NETWORKSHAPEFILE, ZONESHAPEFILE ,OUTPUT_DIR); //Step 2: Links für Tollfile herausschreiben 
//		TODO: TollFile direkt aus den Links erstellen. Aktuell wird die LinkListe manuell kopiert.
	
		
		System.out.println("### ENDE ###");
	}
	

	//NW Step1: Convert Matsim-Network to Shap-File.
	private static void convertNet2Shape(Network network, String outputDir){

		network = convertCoordinates(network);
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:4326"); 


		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
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
		ShapeFileWriter.writeGeometries(features, outputDir+"MatsimNW_conv_Nodes.shp");
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

	//NW-Step2: Extract all Features of NW-Shape which are within the ZoneShape, write there IDs into a .txt-File 
	// which is designed in a way, that it can get copied easily to a tollFill and create a .shp-File with this Features
	private static void extractTollLinks(String networkShapefile, String zoneShapefile, String outputDir) {

		Collection<SimpleFeature>  zoneFeatures = new ShapeFileReader().readFileAndInitialize(zoneShapefile);
		Collection<SimpleFeature>  networkFeatures = new ShapeFileReader().readFileAndInitialize(networkShapefile);

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		FileWriter writer;
		try {
			writer = new FileWriter(new File(outputDir + "tollLinks.txt")); //- falls die Datei bereits existiert wird diese überschrieben
			//			writer = new FileWriter(outputDir + "tollLinks.txt", true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben

			for (SimpleFeature zoneFeature : zoneFeatures){
				for(SimpleFeature networkFeature : networkFeatures){ 
					Geometry zoneGeometry = (Geometry) zoneFeature.getDefaultGeometry();
					Geometry networkGeometry = (Geometry) networkFeature.getDefaultGeometry();
					if(zoneGeometry.contains(networkGeometry)) {
						features.add(networkFeature);
						// Text wird in den Stream geschrieben
						writer.write("<link id= \"" + networkFeature.getAttribute("ID") +"\" />");
						writer.write(System.getProperty("line.separator"));
					}
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei geschrieben.");
		ShapeFileWriter.writeGeometries(features, outputDir+"TolledLinks.shp");
	}
	
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}

}
