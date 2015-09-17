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
	
	private static final Coord coordHBF = new Coord(657864, 5261863);
	private static final Coord coordKSK = new Coord(657966, 5262218);
	private static final Coord coordRAT = new Coord(658520, 5262175);
	private static final Coord coordKLINIK = new Coord(660250, 5261035);
	
	private static final String GP_HBF = "1\tHBF-Elektro Hauptbahnhof\t"+coordHBF.getX() + "\t" + coordHBF.getY() + "\t47.491043\t11.095773\t2";
	private static final String GP_KSK = "2\tKSK-GAP Kreissparkasse\t"+coordKSK.getX() + "\t" + coordKSK.getY() + "\t47.494208\t11.097258\t1";
	private static final String GP_RAT = "2\tRAT-Elektro Rathaus-Elektro\t"+coordRAT.getX() + "\t" + coordRAT.getY() + "\t47.493692\t11.104595\t1";
	private static final String GP_KLINIK = "2\tKLINIK-Elektro Klinikum Garmisch-Partenkirchen\t"+coordKLINIK.getX() + "\t" + coordKLINIK.getY() + "\t47.483017\t11.127125\t2";
	
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
			writer.write(GP_KLINIK);
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public static void writeCSStationsCase1(){
		
		BufferedWriter writer = IOUtils.getBufferedWriter(Global.matsimInputDir + "Carsharing/stationsScenario1.txt");
		
		try {
			
			writer.write(HEADER);
			writer.newLine();
			writer.write(GP_HBF);
			writer.newLine();
			writer.write(GP_KSK);
			writer.newLine();
			writer.write(GP_RAT);
			writer.newLine();
			writer.write(GP_KLINIK);
			
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
		features.add(pff.createPoint(MGC.coord2Coordinate(coordKLINIK), new String[]{"KLINIK-Elektro Klinikum Garmisch-Partenkirchen"}, "4"));
		
		ShapeFileWriter.writeGeometries(features, outputShapefile);
		
	}

}
