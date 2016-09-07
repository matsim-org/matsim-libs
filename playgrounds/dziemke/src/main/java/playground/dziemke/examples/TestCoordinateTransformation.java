package playground.dziemke.examples;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.osgeo.proj4j.CRSFactory;

import com.vividsolutions.jts.geom.Envelope;

public class TestCoordinateTransformation {

	public static void main(String[] args) {
		// WGS84 = EPSG:4326
		// Arc 1960 / UTM zone 37S = "EPSG:21037"
		// WGS 84 / UTM zone 37S = "EPSG:31468"
//		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
//				TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation
//				("EPSG:4326", "EPSG:31468");
//				("EPSG:4326", "EPSG:21037");
//				("EPSG:3006", "EPSG:4326");
				(TransformationFactory.WGS84_SA_Albers, "EPSG:4326");

//		Coord originalCoord1 = new Coord(36.82829619497265, -1.291087691581653); // near Nairobi, Kenya
//		Coord originalCoord1 = new Coord(372300, 5802900); // Berlin lower left
//		Coord originalCoord1 = new Coord(413300, 5833900); // Berlin upper right
		Coord originalCoord1 = new Coord(150583.9441831379,-3699678.99131796); // somewhere in NMB in SA_Albers
//		Coord originalCoord2 = new Coord(171583.944, y);
		
		Coord convertedCoord1 = transformation.transform(originalCoord1);
//		Coord convertedCoord2 = transformation.transform(originalCoord2);
		
		System.out.println("###########################################################################");
		System.out.println("originalCoord1: " + originalCoord1);
//		System.out.println("originalCoord2: " + originalCoord2);
		System.out.println("convertedCoord1: " + convertedCoord1);
//		System.out.println("convertedCoord2: " + convertedCoord2);
		System.out.println("###########################################################################");
		
		
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:4326");
		
		System.out.println("alias = " + crs.getAlias());
		System.out.println("###########################################################################");
		System.out.println("crs = " + crs.getCoordinateSystem());
		System.out.println("###########################################################################");
		System.out.println("id = " + crs.getIdentifiers());
		System.out.println("###########################################################################");
		System.out.println("name = " + crs.getName());
		System.out.println("###########################################################################");
		System.out.println("remarks = " + crs.getRemarks());
		System.out.println("###########################################################################");
		System.out.println("wkt = " + crs.toWKT());
		System.out.println("###########################################################################");
		System.out.println("to string = " + crs.toString());
		
		System.out.println("###########################################################################");
		
		
		CRSFactory factory = new CRSFactory();
		org.osgeo.proj4j.CoordinateReferenceSystem crs2 = factory.createFromName("EPSG:4326");
		
		System.out.println("parameter string " + crs2.getParameterString());
		
		System.out.println("balabbfsbfsbfsb");
		
		
		System.out.println("###########################################################################");
		
		Envelope envelope = new Envelope(115000,161000,-3718000,-3679000);
		
		System.out.println(envelope.getMinX());
		System.out.println(envelope.getMaxX());
		System.out.println(envelope.getMinY());
		System.out.println(envelope.getMaxY());
		
	
	}
}