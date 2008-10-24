package playground.gregor.gis.deprecated;

import java.util.Iterator;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LineStringType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.StyleType;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.world.World;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

public class Network2Kml {

	private NetworkLayer network;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private static KmlType kml;
	private static String KMLFilename = "./test.kmz";
	private static boolean useCompression = true;
	private static DocumentType kmlDocument;

	private StyleType normal;


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

		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation("WGS84_UTM47S", "WGS84");


		String key = "link";


		FolderType folder = this.kmlObjectFactory.createFolderType();
		folder.setName(key);
		folder.setDescription("Contains all " + key);

		kmlDocument.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(folder));
		
		Iterator it = this.network.getLinks().values().iterator();
		while (it.hasNext()){
			Link link = (Link) it.next();
			Coord from = transform.transform(link.getFromNode().getCoord());
			Coord to = transform.transform(link.getToNode().getCoord());

			LineStringType ls = this.kmlObjectFactory.createLineStringType();
			ls.getCoordinates().add(Double.toString(from.getX()) + "," + Double.toString(from.getY()) + "," + Double.toString(Double.NaN));
			ls.getCoordinates().add(Double.toString(to.getX()) + "," + Double.toString(to.getY()) + "," + Double.toString(Double.NaN));

			String styleUrl = this.normal.getId();

			PlacemarkType placemark = this.kmlObjectFactory.createPlacemarkType();
			placemark.setName("link" + link.getId().toString());
			placemark.setDescription("link" + link.getId().toString());
			placemark.setAddress(styleUrl);
			placemark.setStyleUrl(styleUrl);

			//			if (!(key.contains("connection") || key.contains("node")) )
//			placemark.setVisibility(false);
			placemark.setAbstractGeometryGroup(this.kmlObjectFactory.createLineString(ls));
			folder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));

		}


	}

	private void writeKml() {
		
		KMZWriter myKMLDocumentWriter = new KMZWriter(KMLFilename);
		myKMLDocumentWriter.writeMainKml(kml);
		myKMLDocumentWriter.close();

	}

	private void generateStyles(){
		
		this.normal = this.kmlObjectFactory.createStyleType();
		this.normal.setId("normalRoadStyle");
		
		LineStyleType lst = this.kmlObjectFactory.createLineStyleType();
		byte[] color = new byte[]{(byte) 0x7f, (byte) 0xff, (byte) 0xae, (byte) 0x21};
		lst.setColor(color);
		lst.setWidth(5.0);
		this.normal.setLineStyle(lst);
		
		kmlDocument.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(this.normal));

	}

	private void createKml(String kmlFileName) {

		kml = this.kmlObjectFactory.createKmlType();
		kmlDocument = this.kmlObjectFactory.createDocumentType();
		kml.setAbstractFeatureGroup(this.kmlObjectFactory.createDocument(kmlDocument));

	}

	public static void main(String[] args){

		String configFile = "./configs/evacuationConf.xml";


		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});


		String netfile = "./networks/padang_net.xml";
		System.out.println("reading network xml file... ");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		System.out.println("done. ");

		Network2Kml ntk = new Network2Kml(network);
		ntk.run();

	}


}
