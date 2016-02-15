package playground.dhosse.gap.scenario.carsharing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PointFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;

public class CSStationsCreator {
	
	private static final String HEADER = "Ort\tStandort\tPLZ\tStao-Nr.\tKanton\tGeoX\tGeoY\tNorth\tEast\tFahrzeuge";
	
	private static final Coord coordHBF = new Coord(657852, 5261833);
	private static final Coord coordKSK = new Coord(657966, 5262218);
	private static final Coord coordRAT = new Coord(658520, 5262175);
	private static final Coord coordMAR = new Coord(657139, 5261972);
	
	private static final Coord coordKMNP = new Coord(659641, 5260928);
	private static final Coord coordKREUZ = new Coord(655499, 5259678);
	private static final Coord coordRSP = new Coord(657600, 5262362);
	private static final Coord coordSFW = new Coord(650279, 5257828);
	private static final Coord coordADL = new Coord(657996, 5263059);
	private static final Coord coordMurnauBhf = new Coord(664650, 5283307);
	private static final Coord coordMittenwaldBhf = new Coord(670740, 5256519);
	private static final Coord coordOberauBhf = new Coord(660905, 5269420);
	private static final Coord coordOberammergauBhf = new Coord(654592, 5273805);
	
	//current stations
	private static final String GP_HBF = "1\tHBF-Elektro Hauptbahnhof\t"+coordHBF.getX() + "\t" + coordHBF.getY() + "\t47.491043\t11.095773\t2";
	private static final String GP_KSK = "2\tKSK-GAP Kreissparkasse\t"+coordKSK.getX() + "\t" + coordKSK.getY() + "\t47.494208\t11.097258\t1";
	private static final String GP_RAT = "3\tRAT-Elektro Rathaus-Elektro\t"+coordRAT.getX() + "\t" + coordRAT.getY() + "\t47.493692\t11.104595\t1";
	private static final String GP_MAR = "4\tMAR-Elektro Marienplatz-Elektro\t"+coordMAR.getX() + "\t" + coordMAR.getY() + "\t47.492199\t11.086197\t1";
	
	//possible new stations
	private static final String GRAINAU_SFW = "6\tGrainau Seefeldweg 1\t" + coordSFW.getX() + "\t" + coordSFW.getY() + "\t0.0\t0.0\t2";
	private static final String GP_NEUNER = "6\tNEUNER-GAP Karl-und-Martin-Neuner-Platz\t" + coordKMNP.getX() + "\t" + coordKMNP.getY() + "\t0.0\t0.0\t2";
	private static final String GP_KREUZ = "6\tKREUZ-GAP Kreuzeckbahnhof\t" + coordKREUZ.getX() + "\t" + coordKREUZ.getY() + "\t0.0\t0.0\t2";
	private static final String GP_RSP = "6\tRSP-GAP Richard-Strauss-Platz 1\t" + coordRSP.getX() + "\t" + coordRSP.getY() + "\t0.0\t0.0\t2";
	private static final String GP_ADL = "6\tADL-GAP Adlerstraße 25\t" + coordADL.getX() + "\t" + coordADL.getY() + "\t0.0\t0.0\t2";
	
	private static final String MITTENWALD_BHF = "6\tMittenwald Bahnhof\t" + coordMittenwaldBhf.getX() + "\t" + coordMittenwaldBhf.getY() + "\t0.0\t0.0\t2";
	private static final String OBERAU_BHF = "6\tOberau Bahnhof\t" + coordOberauBhf.getX() + "\t" + coordOberauBhf.getY() + "\t0.0\t0.0\t2";
	private static final String MURNAU_BHF = "6\tMurnau Bahnhof\t" + coordMurnauBhf.getX() + "\t" + coordMurnauBhf.getY() + "\t0.0\t0.0\t2";
	private static final String OBERAMMERGAU = "6\tOberammergau Bahnhof\t" + coordOberammergauBhf.getX() + "\t" + coordOberammergauBhf.getY() + "\t0.0\t0.0\t2";
	
	/**
	 * base case:
	 * <ul>
	 * <li>the following cs stations exist today:
	 * <ul>
	 * <li>Hauptbahnhof, Garmisch-Partenkirchen</li>
	 * <li>Kreissparkasse, Garmisch-Partenkirchen</li>
	 * <li>Rathaus, Garmisch-Partenkirchen</li>
	 * <li>Marienplatz, Garmisch-Partenkirchen</li>
	 * </ul>
	 * </ul>
	 */
	public static void writeCurrentCSStations(){
		
		BufferedWriter writer = IOUtils.getBufferedWriter(Global.matsimInputDir + "Carsharing/stations2015.txt");
		
		try {
			
			writer.write(HEADER);
			writer.newLine();
			writer.write(GP_HBF);
			writer.newLine();
			writer.write(GP_KSK);
			writer.newLine();
			writer.write(GP_RAT);
			writer.newLine();
			writer.write(GP_MAR);
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	/**
	 * case 1:
	 * <ul>
	 * <li>cs stations from base case remain the same</li>
	 * <li>additional cs stations in Garmisch-Partenkirchen (Lkr) are added:</li>
	 * <ul>
	 * <li>Adlerstraße, Garmisch-Partenkirchen</li>
	 * <li>Karl-und-Martin-Neuner-Platz, Garmisch-Partenkirchen</li>
	 * <li>Kreuzeckbahnhof, Garmisch-Partenkirchen</li>
	 * <li>Richard-Strauss-Platz, Garmisch-Partenkirchen</li>
	 * <li>Seefeldweg, Grainau</li>
	 * </ul>
	 * </ul>
	 */
	public static void writeCSStationsCase1(){
		
		BufferedWriter writer = IOUtils.getBufferedWriter(Global.matsimInputDir + "stationsScenario1.txt");
		
		try {
			
			writer.write(HEADER);
			writer.newLine();
			writer.write(GP_HBF);
			writer.newLine();
			writer.write(GP_KSK);
			writer.newLine();
			writer.write(GP_RAT);
			writer.newLine();
			writer.write(GP_MAR);
			writer.newLine();
			writer.write(GP_ADL);
			writer.newLine();
			writer.write(GP_NEUNER);
			writer.newLine();
			writer.write(GP_KREUZ);
			writer.newLine();
			writer.write(GP_RSP);
			writer.newLine();
			writer.write(GRAINAU_SFW);
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public static void writeCSStationToShapefile(String outputShapefile){
		
		Builder builder = new Builder();
		builder.setCrs(MGC.getCRS(Global.toCrs));
		builder.addAttribute("name", String.class);
		PointFeatureFactory pff = builder.create();
		
		List<SimpleFeature> features = new ArrayList<>();
		
		features.add(pff.createPoint(MGC.coord2Coordinate(coordHBF), new String[]{"HBF-Elektro Hauptbahnhof"}, "1"));
		features.add(pff.createPoint(MGC.coord2Coordinate(coordKSK), new String[]{"KSK-GAP Kreissparkasse"}, "2"));
		features.add(pff.createPoint(MGC.coord2Coordinate(coordRAT), new String[]{"RAT-Elektro Rathaus-Elektro"}, "3"));
		features.add(pff.createPoint(MGC.coord2Coordinate(coordMAR), new String[]{"KLINIK-Elektro Klinikum Garmisch-Partenkirchen"}, "4"));
		
		ShapeFileWriter.writeGeometries(features, outputShapefile);
		
	}

}
