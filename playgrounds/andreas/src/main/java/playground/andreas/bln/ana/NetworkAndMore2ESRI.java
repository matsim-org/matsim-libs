package playground.andreas.bln.ana;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class NetworkAndMore2ESRI extends Links2ESRIShape{
	
	private static Logger log = Logger.getLogger(Links2ESRIShape.class);

	public NetworkAndMore2ESRI(Network network, String filename) {
		super(network, filename);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netfile = null ;
		String outputFileLs = null ;
		String outputFileP = null ;
		
		if ( args.length == 0 ) {
			netfile = "./bb_5_A100_16_17_18_v_scaled_simple.xml.gz";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";

			outputFileLs = "./networkLs.shp";
			outputFileP = "./networkP.shp";
		} else if ( args.length == 3 ) {
			netfile = args[0] ;
			outputFileLs = args[1] ;
			outputFileP  = args[2] ;
		} else {
			log.error("Arguments cannot be interpreted.  Aborting ...") ;
			System.exit(-1) ;
		}
		
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		log.info("done.");

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
//		builder.setFeatureGeneratorPrototype(CountVehOnLinksStringBasedFeatureGenerator.class);
		builder.setFeatureGeneratorPrototype(LinkstatsStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);		
		new Links2ESRIShape(network,outputFileLs, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.01);
//		builder.setFeatureGeneratorPrototype(CountVehOnLinksPolygonBasedFeatureGenerator.class);
		builder.setFeatureGeneratorPrototype(LinksstatsPolygonBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();

	}

}
