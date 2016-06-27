package playground.gthunig.WKT2Proj4;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.proj.Projection;

/**
 * Created by GabrielT on 27.06.2016.
 */
public class WKT2Proj4 {

	public static void main(String[] args) {
		do1();
	}

	public static void do1() {
		CRSFactory factory = new CRSFactory();
		CoordinateReferenceSystem crs = factory.createFromName("EPSG:21037");
		System.out.println("crs.toString() = " + crs.toString());
		System.out.println("crs.getParameterString() = " + crs.getParameterString());
		Projection projection = crs.getProjection();
		System.out.println("projection.toString() = " + projection.toString());
	}
}
