package playground.gregor.gis;

import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.LinkImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.LineString;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.world.World;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

public class Network2Kml {
	
	private NetworkLayer network;
	
	
    private static KML kml; 
	private static String KMLFilename = "./test.kmz";
	private static boolean useCompression = true;
	private static Document kmlDocument;
	
	private Style normal;
	
	
	public Network2Kml(NetworkLayer network){
		this.network = network;
	}

	public void run(){
		createKml(KMLFilename);
		generateStyles();
		try {
			generateKmlData();
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeKml();
		
	}
	private void generateKmlData() throws MismatchedDimensionException, TransformException {
		
		CoordinateTransformationI transform = TransformationFactory.getCoordinateTransformation("WGS84_UTM47S", "WGS84");
		
		
		String key = "link";
		
		
		Folder folder = new Folder(
				key,
				key,
				("Contains all "+ key),
				key, org.matsim.utils.vis.kml.Feature.DEFAULT_LOOK_AT,
				org.matsim.utils.vis.kml.Feature.DEFAULT_STYLE_URL,
				true,
				org.matsim.utils.vis.kml.Feature.DEFAULT_REGION,
				org.matsim.utils.vis.kml.Feature.DEFAULT_TIME_PRIMITIVE);
			
			kmlDocument.addFeature(folder);
			
			Iterator it = network.getLinks().values().iterator();
			while (it.hasNext()){
				LinkImpl link = (LinkImpl) it.next();
				CoordI from = transform.transform(link.getFromNode().getCoord());
				CoordI to = transform.transform(link.getToNode().getCoord());
				
				
				LineString ls = new LineString(new Point(from.getX(),from.getY(),Double.NaN), new Point(to.getX(),to.getY(),Double.NaN));
				
				String styleUrl = this.normal.getStyleUrl();
				
				Placemark placemark = new Placemark(
						link.getId().toString(),
						"link" + link.getId().toString(),
						"link" + link.getId().toString(),
						styleUrl, org.matsim.utils.vis.kml.Feature.DEFAULT_LOOK_AT,
						styleUrl,
						true,
						org.matsim.utils.vis.kml.Feature.DEFAULT_REGION,
						org.matsim.utils.vis.kml.Feature.DEFAULT_TIME_PRIMITIVE);
//				if (!(key.contains("connection") || key.contains("node")) )
//					placemark.setVisibility(false);
				folder.addFeature(placemark);
				placemark.setGeometry(ls);
				
			}
			
		
	}

	private void writeKml() {
		KMLWriter myKMLDocumentWriter;
		myKMLDocumentWriter = new KMLWriter(kml, KMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
		myKMLDocumentWriter.write();
		
	}

	private void generateStyles(){
		normal = new Style("normalRoadStyle");
		kmlDocument.addStyle(normal);
		normal.setLineStyle(new LineStyle(new Color("7f","ff","ae","21"), ColorStyle.DEFAULT_COLOR_MODE, 5));

	}
	
	private void createKml(String kmlFileName) {
		
		kml = new KML();
		kmlDocument = new Document("padang");
		kml.setFeature(kmlDocument);
		
		
		
	}
	
	public static void main(String[] args){
		
		String configFile = "./configs/evacuationConf.xml";
		
		
		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});
		
		
		String netfile = "./networks/padang_net.xml";
		System.out.println("reading network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		System.out.println("done. ");
		
		Network2Kml ntk = new Network2Kml(network);
		ntk.run();
		
	}
	
	
}
