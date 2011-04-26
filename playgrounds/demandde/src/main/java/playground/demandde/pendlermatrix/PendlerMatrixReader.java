package playground.demandde.pendlermatrix;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;




public class PendlerMatrixReader {

	private static final Logger log = Logger.getLogger(PendlerMatrixReader.class);

	private static final String PV_EINPENDLERMATRIX = "../../detailedEval/eingangsdaten/Pendlermatrizen/EinpendlerMUC_843_062004.csv";

	private static final String PV_AUSPENDLERMATRIX = "../../detailedEval/eingangsdaten/Pendlermatrizen/AuspendlerMUC_843_062004.csv";

	private static final String NODES = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv";

	private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();

	private TripFlowSink flowSink;

	public void run() {
		readNodes();
		readMatrix(PV_EINPENDLERMATRIX);
		readMatrix(PV_AUSPENDLERMATRIX);
		flowSink.complete();
	}

	private void readNodes() {
		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(NODES);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {
				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("Knoten")) {
						return;
					}
					int zone = Integer.parseInt(row[5]);
					double x = Double.parseDouble(row[2]);
					double y = Double.parseDouble(row[3]);
					Zone zone1 = new Zone(zone, 1, 1, coordinateTransformation.transform(new CoordImpl(x,y)));
					zones.put(zone, zone1);
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void readMatrix(final String filename) {
		
		Logger.getLogger(this.getClass()).warn("this method may read double entries in the Pendlermatrix (such as Nuernberg) twice. " +
				"If this may be a problem, you need to check.  kai, apr'11" ) ;
		
		System.out.println("======================" + "\n"
						   + "Start reading " + filename + "\n"
						   + "======================" + "\n");
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {","});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("#")) {
						return;
					}
					Integer quelle = null ;
					Integer ziel = 0;
					// car market share for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
					double carMarketShare = 0.67;
					// scale factor, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
					double scaleFactor = 1.29;

					if (filename.equals(PV_EINPENDLERMATRIX)){
						try {
							quelle = Integer.parseInt(row[2]);
							ziel = 9162 ;
							
							int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
							int workPt = (int) ((1 - carMarketShare) * totalTrips) ;
							int educationPt = 0 ;
							int workCar = (int) (carMarketShare * totalTrips);
							int educationCar = 0 ;
							String label = row[3] ;
							if ( !label.contains("brige ") && !quelle.equals(ziel)) {
								process(quelle, ziel, workPt, educationPt, workCar, educationCar);
							} else {
								System.out.println( " uebrige? : " + label ) ;
							}
						} catch ( Exception ee ) {
							System.err.println("we are trying to read quelle: " + quelle ) ;
							//						System.exit(-1) ;
						}
					}
					else if (filename.equals(PV_AUSPENDLERMATRIX)){
						try {
							quelle = 9162;
							ziel = Integer.parseInt(row[2]);

							int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
							int workPt = (int) ((1 - carMarketShare) * totalTrips) ;
							int educationPt = 0 ;
							int workCar = (int) (carMarketShare * totalTrips);
							int educationCar = 0 ;
							String label = row[3] ;
							if ( !label.contains("brige ") && !quelle.equals(ziel)) {
								process(quelle, ziel, workPt, educationPt, workCar, educationCar);
							} else {
								System.out.println( " uebrige? : " + label ) ;
							}
						} catch ( Exception ee ) {
							System.err.println("we are trying to read quelle: " + quelle ) ;
							//						System.exit(-1) ;
						}
					}
					else{
						System.err.println("ATTENTION: check filename!") ;
					}
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void process(int quelle, int ziel, int workPt, int educationPt, int workCar, int educationCar) {
		Zone source = zones.get(quelle);
		Zone sink = zones.get(ziel);
		if (source == null) {
			log.error("Unknown source: " + quelle);
			return;
		}
		if (sink == null) {
			log.error("Unknown sink: " + ziel);
			return;
		}
		int carQuantity = workCar + educationCar ;
		int ptQuantity = workPt + educationPt;
		int scaledCarQuantity = scale(carQuantity);
		int scaledPtQuantity = scale(ptQuantity);
		
		if (scaledCarQuantity != 0) {
			log.info(quelle + "->" + ziel + ": " + scaledCarQuantity + " car trips");
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledCarQuantity, TransportMode.car, "pvWork", 0.0);
		}
		if (scaledPtQuantity != 0){
			log.info(quelle + "->" + ziel + ": " + scaledPtQuantity + " pt trips");
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledPtQuantity, TransportMode.pt, "pvWork", 0.0);
		}
	}

	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.1 );
		return scaled;
	}

	void setFlowSink(TripFlowSink flowSink) {
		this.flowSink = flowSink;
	}

}
