package playground.dhosse.cl.population;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

public class Freight {

	private static final String svnWorkingDir = "../../shared-svn/"; 	//Path: KT (SVN-checkout)
	private static final String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/inputFromElsewhere/exportedFilesFromDatabase/" ;
	private static final String outputDir = svnWorkingDir + "Kai_und_Daniel/inputForMATSim/freight/" ; //outputDir of this class -> input for Matsim (KT)

	private static Map<String, Coord> zonaId2Coord = new HashMap<String, Coord>();
	private static Map<String, FreightTrip> tripId2FreightTrip = new HashMap<String, FreightTrip>(); 

	private static final Logger log = Logger.getLogger(Freight.class);

	//TODO: Pläne aus den Informationen erstellen (Haben nur 2 Aktivitäten: Start und Zielort. Muss aber getrennt erfolgen, 
	// da für Outgoing Verkehr die Startzeit unbekannt ist.)
	//TODO: Hochrechnen auf andere Zeiten, die in der Umfrage nicht erfasst sind
	//TODO: LKW-Verkehr innerhalb des Großraums Santiago erstellen.
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File file = new File(outputDir);
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	

		String crs = "EPSG:32719";
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);

		createObservationPoints();
		readZonas(workingDirInputFiles + "freight_Centroides.csv");
		convertZonas2Shape(crs, outputDir + "shapes/");
		readEODData(workingDirInputFiles + "freight_EODcam.csv");
		//TODO: generatePlans();
		
		log.info("### Done. ###");

	}

	

	/**
	 * Create locations for the ObservationPoints of the survey as origin oder destination of freight tour.
	 * Origin and destination are separated so there the incoming traffic start on an inbound link to Santiago and 
	 * outgoing traffic ends on an outbound link from Santigo
	 */
	private static void createObservationPoints() {

		//CE01	CAMINO A MELIPILLA
		zonaId2Coord.put("CE01in", new Coord(292630.0, 6271720.0));
		zonaId2Coord.put("CE01out", new Coord(292650.0, 6271755.0)); 
		//CE02	AUTOPISTA DEL SOL
		zonaId2Coord.put("CE02in", new Coord(296025.0, 6271935));
		zonaId2Coord.put("CE02out", new Coord(296025.0, 6271905)); 
		//CE03	RUTA 68 (A VALPARAISO) // TODO: how to make sure, that the right link will be used?
		zonaId2Coord.put("CE03in", new Coord(323540.0, 6296520.0));
		zonaId2Coord.put("CE03out", new Coord(323560.0, 6296520.0)); 
		//CE04	RUTA 5 SUR (ANGOSTURA) ; out of box -> moved northbound on Ruta 5
		zonaId2Coord.put("CE04in", new Coord(338200.0, 6253060.0));
		zonaId2Coord.put("CE04out", new Coord(338180.0, 6253060.0)); 
		//CE05	RUTA 5 NORTE (LAMPA)
		zonaId2Coord.put("CE05in", new Coord(336100.0 , 6321210.0));
		zonaId2Coord.put("CE05out", new Coord(336160.0 , 6321210.0)); 
		//CE06	CAMINO PADRE HURTADO
		zonaId2Coord.put("CE06in", new Coord(343990.0, 6266370.0));
		zonaId2Coord.put("CE07out", new Coord(343955.0, 6266370.0)); 
		//CE07	AUTOPISTA LOS LIBERTADORES; toll Station Chacabuco -> not found on google earth.
		zonaId2Coord.put("CE07in", new Coord(346515.0, 6327220.0));
		zonaId2Coord.put("CE07out", new Coord(346560.0, 6327220.0)); 
		//CE08	CAMINO SAN JOSE DE MAIPO
		zonaId2Coord.put("CE08in", new Coord(358240.0, 6281315.0));
		zonaId2Coord.put("CE08out", new Coord(358240.0, 6281280.0)); 
		//CE09	CAMINO A FARELLONES
		zonaId2Coord.put("CE09in", new Coord(360730.0, 6307035.0));
		zonaId2Coord.put("CE09out", new Coord(360730.0, 6306980.0)); 
		//CE10	CAMINO LO ECHEVERS (Lampa) Road Isabel Riquelme, north of Antonio Varas
		zonaId2Coord.put("CE10in", new Coord(323950.0, 6316720.0));
		zonaId2Coord.put("CE10out", new Coord(324000.0, 6316720.0)); 		
	}

	/**
	 * Read Coordinates for OS-Zonas from file
	 * @param ZonasFile
	 */
	private static void readZonas(String ZonasFile){

		log.info("Reading zonas from file " + ZonasFile + "...");

		final int idxZonaId = 0;
		final int idxCoordX = 1;
		final int idxCoordY = 2;

		BufferedReader reader = IOUtils.getBufferedReader(ZonasFile);

		int counter = 0;

		try {

			String line = reader.readLine();

			while( (line = reader.readLine()) != null ){

				String[] splittedLine = line.split(";");

				String id = splittedLine[idxZonaId];
				String x = splittedLine[idxCoordX].replace("," , ".");
				String y = splittedLine[idxCoordY].replace("," , ".");
				//				this.ZonaId2Coord.put(id, new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				zonaId2Coord.put(id, new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				counter++;

			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();

		}

		log.info("Read data of " + counter + " zonas...");

	}

	private static void convertZonas2Shape(String crs, String outputDir){

		File file = new File(outputDir);
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("zonas").
				addAttribute("ID", String.class).
				create();

		for (String zonaId : zonaId2Coord.keySet()) {
//			SimpleFeature ft = nodeFactory.createPoint(zonaId2Coord.get(zonaId), null, zonaId);
			SimpleFeature ft = nodeFactory.createPoint(zonaId2Coord.get(zonaId), new Object[] {zonaId}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outputDir + "Zonas_Points.shp");
	}
	
	/**
	 * Read trip data from EOD file.
	 * @param string
	 */
	private static void readEODData(String EODFile) {
		log.info("Reading trips from file " + EODFile + "...");

		final int idxTripId = 0;
		final int idxPCon = 3;		//Point of survey
		final int idxHour = 7;		//Time of recorded 
		final int idxMinute = 8;	//Time of recorded 
		final int idxAxis = 10;		//Number of axis of truck
		final int idxZOrig = 11;	//Zone of tour start
		final int idxZDest = 12;	//Zone of tour destination
		final int idxCarga = 13; //Type of goods loaded
		
		BufferedReader reader = IOUtils.getBufferedReader(EODFile);

		int counter = 0;

		try {

			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){

				String[] splittedLine = line.split(";");

				String id = splittedLine[idxTripId].split(",")[0];
				String originZone = splittedLine[idxZOrig];
				String destinationZone = splittedLine[idxZDest];
				String pCon = splittedLine[idxPCon].split(",")[0]; //Point of survey
				int numberOfAxis =  new Double(splittedLine[idxAxis].split(",")[0]).intValue();
				double timeOfSurvey = new Double(splittedLine[idxHour].replace("," , "."))*3600 + new Double(splittedLine[idxMinute].replace("," , "."))*60;
				String typeOfGoods = splittedLine[idxCarga].split(",")[0]; 
				
				//Vehicle was recorded on the way TO Santiago (inbound)
				if ( originZone == pCon) {
					originZone = originZone.concat("in");
				}
				
				//Vehicle was recorded on the way FROM Santiago (outbound)
				if ( destinationZone == pCon) {
					destinationZone = destinationZone.concat("out");
				}
				
				FreightTrip trip = new FreightTrip(id, originZone, destinationZone, pCon, numberOfAxis, timeOfSurvey, typeOfGoods);
				tripId2FreightTrip.put(id, trip);
				counter++;
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();

		}

		log.info("Read data of " + counter + " trips...");
		
	}
}
