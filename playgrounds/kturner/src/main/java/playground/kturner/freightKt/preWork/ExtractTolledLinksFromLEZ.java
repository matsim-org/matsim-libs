package playground.kturner.freightKt.preWork;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

class ExtractTolledLinksFromLEZ {

	/**
	 * @author: Kturner 
	 * 
	 * Extrahiert die Link-Ids eines Netzwerkes, welche innerhalb einer Zone 
	 * (Hier Umweltzone - LowEmissionZone) liegen und gibt diese als Textdatei aus -> "_area". 
	 *         
	 * Extrahiert zusätzlich die Links, welche den Cordon der Zone bilden. ->   "_cordon" 
	 * 
	 * Die Ausgabe erfolgt dergestalt, dass sie direkt zur Definition eines 
	 * Roadpricing-Files verwendet werden kann (copy-paste). 
	 * 
	 * Für beide Varianten werden zusätzlich noch .shp-Files erstellt und ausgegeben 
	 * 
	 * TODO: TollFile direkt aus den Links erstellen. Aktuell wird die LinkListe manuell kopiert.
	 */

	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/Case_KT/" ;

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "network" ;
	//	private static final String TOLL_NAME = "toll_city_kt";
	private static final String ZONE_SHAPE_NAME = "Umweltzone/Umweltzone_WGS84";
	private static final String NETWORK_SHAPE_LINKS_NAME = "Umweltzone/MatsimNW_conv_Links";
	private static final String NETWORK_SHAPE_NODES_NAME = "Umweltzone/MatsimNW_conv_Nodes";
	//Ende  Namesdefinition Berlin


	//	//Beginn Namesdefinition KT Für Test-Szenario (Grid)
	//	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Grid_Szenario/" ;
	//	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Matsim/Demand/Grid/UCC2/" ;
	//	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/" ;	
	//
	//	//Dateinamen ohne XML-Endung
	//	private static final String NETFILE_NAME = "grid-network" ;
	//	//private static final String TOLL_NAME = "grid-tollCordon";
	//	//Ende Namesdefinition Grid


	private static final String NETFILE = INPUT_DIR + NETFILE_NAME + ".xml" ;
	private static final String ZONESHAPEFILE = INPUT_DIR + ZONE_SHAPE_NAME + ".shp";
	private static final String NETWORKSHAPEFILE_LINKS = INPUT_DIR + NETWORK_SHAPE_LINKS_NAME + ".shp";
	private static final String NETWORKSHAPEFILE_NODES = INPUT_DIR + NETWORK_SHAPE_NODES_NAME + ".shp";
	//	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";	//Für Output 

	//Korrekturen für Cordon Maut - BerlinSzenario:
	private static final ArrayList<String> removeLinks = new ArrayList<String>(Arrays.asList("7689", "7845", "7673", "6867", "3612", "3605", "6972", "5490", "7360", "3629", "3687", "3787", "3794"));
	private static final ArrayList<String> addLinks =  new ArrayList<String>(Arrays.asList("7682", "5367", "3682", "3697", "3778", "3791"));

	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));

		//Network-Stuff
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETFILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		convertNet2Shape(scenario.getNetwork(), OUTPUT_DIR); //Step 1: Netzwerk zusammenbringen -> Richtige Konvertierung gefunden ;-)

		Collection<SimpleFeature>  zoneFeatures = new ShapeFileReader().readFileAndInitialize(ZONESHAPEFILE);
		Collection<SimpleFeature>  linkFeatures = new ShapeFileReader().readFileAndInitialize(NETWORKSHAPEFILE_LINKS);
		Collection<SimpleFeature>  nodeFeatures = new ShapeFileReader().readFileAndInitialize(NETWORKSHAPEFILE_NODES);

		ArrayList<String> nodesInZone = calcNodesInZone(zoneFeatures, nodeFeatures);
		extractTollLinksArea(linkFeatures, nodesInZone, zoneFeatures ,OUTPUT_DIR); //Step 2: Links für Tollfile herausschreiben 
		extractTollLinksCordon(linkFeatures, nodesInZone, zoneFeatures ,OUTPUT_DIR); //Step 2: Links für Tollfile herausschreiben -> Definiton der CordonLinks

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
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), 
									link.getLength(), NetworkUtils.getType(((Link)link)), link.getCapacity(), link.getFreespeed()}, null);
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

	//Convert coordinates of NW from Gauß-Krueger (Potsdam) [EPSG: 31468]
	//to coordinate-format of low-emission-zone WGS84 [EPSG:4326]
	private static Network convertCoordinates(Network net){

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:4326");

		for(Node node : net.getNodes().values()){
			Coord newCoord = ct.transform(node.getCoord());
			((Node)node).setCoord(newCoord);
		}

		return net;
	}

	//NW-Step2a: Extract all Features of NW-Shape which are within the ZoneShape, write their IDs into a .txt-File 
	// which is designed in a way, that it can get copied easily to a tollFill and create a .shp-File with this Features
	private static void extractTollLinksArea(Collection<SimpleFeature> linkFeatures, 
			ArrayList<String> nodesInZone, Collection<SimpleFeature> zoneFeatures, String outputDir) {

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		FileWriter writer;
		try {
			writer = new FileWriter(new File(outputDir + "tollLinks_area.txt")); //- falls die Datei bereits existiert wird diese überschrieben
		
			for (SimpleFeature zoneFeature : zoneFeatures){
				for(SimpleFeature linkFeature : linkFeatures){ 
					Geometry zoneGeometry = (Geometry) zoneFeature.getDefaultGeometry();
					Geometry networkGeometry = (Geometry) linkFeature.getDefaultGeometry();
					if( ( zoneGeometry.contains(networkGeometry) 		 // Link innerhalb der Zone
							|| (zoneGeometry.crosses(networkGeometry) && (nodesInZone.contains(linkFeature.getAttribute("toID"))) )	 //Link kreuzt Zonengrenze von außen nach innen.
							|| addLinks.contains(linkFeature.getAttribute("ID")) ) //Link soll manuell hinzugefügt werden
							&& !removeLinks.contains(linkFeature.getAttribute("ID")) ){
						features.add(linkFeature);
						// Text wird in den Stream geschrieben
						writer.write("<link id= \"" + linkFeature.getAttribute("ID") +"\" />");
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
		ShapeFileWriter.writeGeometries(features, outputDir+"TolledLinks_area.shp");
	}

	//NW-Step2b: Extract all Features of NW-Shape which are defining the ZoneShape, write there IDs into a .txt-File 
	// which is designed in a way, that it can get copied easily to a tollFill and create a .shp-File with this Features
	private static void extractTollLinksCordon(Collection<SimpleFeature> linkFeatures, 
			ArrayList<String> nodesInZone, Collection<SimpleFeature> zoneFeatures, String outputDir) {

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		FileWriter writer;
		try {
			writer = new FileWriter(new File(outputDir + "tollLinks_Cordon.txt")); //- falls die Datei bereits existiert wird diese überschrieben
			//			writer = new FileWriter(outputDir + "tollLinks.txt", true);  //true ---> wird ans Ende und nicht an den Anfang geschrieben
			for (SimpleFeature zoneFeature : zoneFeatures){
				for(SimpleFeature linkFeature : linkFeatures){ 
					Geometry zoneGeometry = (Geometry) zoneFeature.getDefaultGeometry();
					Geometry networkGeometry = (Geometry) linkFeature.getDefaultGeometry();

					if( ( (zoneGeometry.crosses(networkGeometry) && (nodesInZone.contains(linkFeature.getAttribute("toID"))) )	 //Link kreuzt Zonengrenze von außen nach innen.
							|| addLinks.contains(linkFeature.getAttribute("ID")) ) //Link soll manuell hinzugefügt werden
							&& !removeLinks.contains(linkFeature.getAttribute("ID")) ){
				
						features.add(linkFeature);
						writer.write("<link id= \"" + linkFeature.getAttribute("ID") +"\" />");
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
		ShapeFileWriter.writeGeometries(features, outputDir+"TolledLinks_Cordon.shp");
	}

	private static ArrayList<String> calcNodesInZone(Collection<SimpleFeature> zoneFeatures, 
			Collection<SimpleFeature> nodeFeatures){

		ArrayList<String> nodesInZone = new  ArrayList<String>();
		for (SimpleFeature zoneFeature : zoneFeatures){
			for(SimpleFeature networkFeature : nodeFeatures){ 
				Geometry zoneGeometry = (Geometry) zoneFeature.getDefaultGeometry();
				Geometry networkGeometry = (Geometry) networkFeature.getDefaultGeometry();
				if(zoneGeometry.contains(networkGeometry)) {
					nodesInZone.add(networkFeature.getAttribute("ID").toString());
				}
			}
		}
		return nodesInZone;
	}

	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}

}
