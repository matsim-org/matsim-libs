package playground.andreas.utils.ana;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

import playground.dgrether.visualization.DgNetworkKmlStyleFactory;

/**
 * Convert MATSim network to kml
 * 
 * @author aneumann
 *
 */
public class KmlNetworkVisualizer {
	
	private static final Logger log = Logger.getLogger(KmlNetworkVisualizer.class);

	/**
	 * Convert MATSim network to kml
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		KmlNetworkVisualizer.convertNetwork2Kml("F:/convert/counts_network_merged.xml_cl.xml", "F:/convert/net.kml", new GK4toWGS84());
	}
	
	public static void convertNetwork2Kml(NetworkImpl network, String kmlfile, CoordinateTransformation coordTransform){
		KmlNetworkVisualizer kmlNetVis = new KmlNetworkVisualizer();
		kmlNetVis.write(network, kmlfile, coordTransform);
	}
	
	public static void convertNetwork2Kml(String netfile, String kmlfile, CoordinateTransformation coordTransform){
		log.info("Reading network...");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netfile);
		KmlNetworkVisualizer.convertNetwork2Kml(scenario.getNetwork(), kmlfile, coordTransform);		
	}
	
	private void write(final NetworkImpl network, final String filename, CoordinateTransformation transform) {
		
		log.info("Converting network to kmz...");
		
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KmlType mainKml;
		DocumentType mainDoc;
		FolderType mainFolder;
		KMZWriter writer;
		
		// init kml
		mainKml = kmlObjectFactory.createKmlType();
		mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		// create a folder
		mainFolder = kmlObjectFactory.createFolderType();
		mainFolder.setName("Matsim Data");
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(mainFolder));
		// the writer
		writer = new KMZWriter(filename);
		try {
			// add the matsim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(logo));
			KmlNetworkWriter netWriter = new KmlNetworkWriter(network,
					transform, writer, mainDoc);
			
			netWriter.setNetworkKmlStyleFactory(new DgNetworkKmlStyleFactory(writer, mainDoc));
			FolderType networkFolder = netWriter.getNetworkFolder();
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(networkFolder));
		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo cause: " + e.getMessage());
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
		log.info("Network written to kmz!");
	}

}
