package playground.smetzler.santiago.polygon;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;



public class CreatePoly {

	static String inputCSV = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/demand/poly/santiago.csv";
	static String outputSHP = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/demand/poly/santiagoPolyConvex.shp";

	public static void main(String[] args) throws Exception {

		Map<String, List<Coord>> haltestellen = CSVeinlesen();
		Map<String, List<Geometry>> multipoints = CreateMulitipoint(haltestellen);
		Map<String, Polygon> polys;
		
		polys = CreatePolygon(multipoints);
		ShapeWriter(polys,outputSHP);
	}



	public static Map<String, List<Coord>> CSVeinlesen () throws IOException {

		Map<String, List<Coord>> halstestellenProZone = new HashMap<>();

		String splitBy = ",";
		String[] zeile = null;
		BufferedReader br = new BufferedReader(new FileReader(inputCSV));
		String line = br.readLine();
		while((line = br.readLine()) != null){

			zeile = line.split(splitBy);
			if (halstestellenProZone.get(zeile[3])== null) {
				halstestellenProZone.put(zeile[3], new ArrayList<Coord>());}

			halstestellenProZone.get(zeile[3]).add(new CoordImpl(zeile[1],zeile[2]));
		}
		br.close();
		return halstestellenProZone;
	}




	public static Map<String, List<Geometry>> CreateMulitipoint ( Map<String, List<Coord>> halstestellenProZone) {

		Map<String, List<Geometry>> geometries = new HashMap<>();

		Coordinate[] coordinates = null;

		for (String  zone : halstestellenProZone.keySet() )
		{	
			halstestellenProZone.get(zone);
			ArrayList<Coordinate> coords = new ArrayList<>();
			
			for (Coord  coordinat : halstestellenProZone.get(zone))
			{
				coords.add(new Coordinate(coordinat.getX(), coordinat.getY(), 0.0));
				coordinates = coords.toArray(new Coordinate[coords.size()]);				
			}

			Geometry multiPoint = new GeometryFactory().createMultiPoint(coordinates);
			geometries.put(zone, new ArrayList<Geometry>());
			geometries.get(zone).add(multiPoint);
		}

		return  geometries;
	}



	public static Map<String, Polygon> CreatePolygon (Map<String, List<Geometry>> multipoints) {
		Map<String, Polygon> polys = new HashMap<>();

		for (String  zone : multipoints.keySet() )	
		{
			for (Geometry  s : multipoints.get(zone))
			{
				if (s.getNumPoints()> 2)
				{
					Polygon convexHull = (Polygon) s.convexHull();
					polys.put(zone,  convexHull);
				}
				else
					//bei weniger als 3 Punkten wird zunaechst kein Polygon sondern ein Punkt oder ein Linestring gemacht. Deswegen wird darum ein Buffer gelegt.
				{
					Geometry convexHullLine = s.convexHull();
					Polygon buffer =  (Polygon) convexHullLine.buffer(20);
					polys.put(zone,  buffer);
				}
			}
		}
		return polys;
	}



//	public static Map<String, Polygon> CreatePolygon (Map<String, List<Geometry>> multipoints) {
//		Map<String, Polygon> polys = new HashMap<>();
//		
//		for (String  zone : multipoints.keySet() )	
//		{
//			
//			for (Geometry  s : multipoints.get(zone))
//			{
//				
//				double xmax = s.getEnvelopeInternal().getMaxX();
//				double ymax = s.getEnvelopeInternal().getMaxY();
//				double xmin = s.getEnvelopeInternal().getMinX();
//				double ymin = s.getEnvelopeInternal().getMinY();
//
//
//				Coordinate c1 = new Coordinate(xmin, ymin);
//				Coordinate c2 = new Coordinate(xmin, ymax);
//				Coordinate c3 = new Coordinate(xmax, ymax);
//				Coordinate c4 = new Coordinate(xmax, ymin);
//				Coordinate c5 = c1;
//				GeometryFactory geofac = new GeometryFactory();
//				Coordinate[] coordinates = new Coordinate[] {c1, c2, c3, c4, c5};
//				LinearRing linearRing = geofac.createLinearRing(coordinates);
//				Polygon polygon = geofac.createPolygon(linearRing, null);
//
//				
//
//				// convex hull !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//		// getInteriorPoint
//				
//				
//				//		 GeometryFactory fact = new GeometryFactory();
//				//		 LinearRing linear = new GeometryFactory().createLinearRing(s.getCoordinates());
//				//		 
//				//		 Polygon poly = fact.createPolygon(linear, null);
//				////		 Polygon poly = new Polygon(linear, null, fact);
//				//		 
//				//		 
//				//	// polys.put("no key yet", poly);
//				
//				polys.put(zone,  polygon);
//
//				
//			}	
//		}
//		return polys;
//	}



	private static void ShapeWriter(Map<String, Polygon> polys, String filename) {

		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().
				setCrs(MGC.getCRS("EPSG:24879")).
				setName("keinName").
				addAttribute("zoneId", String.class).
				create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();


		Object[] featureAttribs;
		for(Entry<String, Polygon> e: polys.entrySet()){
			featureAttribs = new Object[1];
			featureAttribs[0] = e.getKey();
			features.add(factory.createPolygon(e.getValue(), featureAttribs, null));

		}
		ShapeFileWriter.writeGeometries(features, filename);
	}


}
