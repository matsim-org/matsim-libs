package city2000w;

import java.io.IOException;

import kid.KiDUtils;


import com.vividsolutions.jts.geom.Coordinate;


public class ShpReaderTester {

	public static String WGS84_32N = "PROJCS[\"WGS_1984_UTM_Zone_32N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]]," +
	"PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000]," +
			"PARAMETER[\"False_Northing\",0],PARAMETER[\"Central_Meridian\",9],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0],UNIT[\"Meter\",1]]";

	public static String FOO = "GEOGCS[\"WGS 84\", DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]], TOWGS84[0,0,0,0,0,0,0]," +
	                          "AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]]," +
	                          "AXIS[\"Lat\",NORTH],AXIS[\"Long\",EAST],AUTHORITY[\"EPSG\",\"4326\"]]";

	
//	public static String WGS84_32N_new = "PROJCS["WGS 84 / UTM zone 32N"," +
//	       GEOGCS["WGS 84", +
//	           DATUM["WGS_1984",
//	               SPHEROID["WGS 84",6378137,298.257223563, +
//	                   AUTHORITY["EPSG","7030"]], +
//	               AUTHORITY["EPSG","6326"]], +
//	           PRIMEM["Greenwich",0, +
//	               AUTHORITY["EPSG","8901"]], +
//	           UNIT["degree",0.01745329251994328, +
//	               AUTHORITY["EPSG","9122"]], +
//	           AUTHORITY["EPSG","4326"]], +
//	       UNIT["metre",1, +
//	           AUTHORITY["EPSG","9001"]], +
//	       PROJECTION["Transverse_Mercator"], +
//	       PARAMETER["latitude_of_origin",0], +
//	       PARAMETER["central_meridian",9], +
//	       PARAMETER["scale_factor",0.9996], +
//	       PARAMETER["false_easting",500000], +
//	       PARAMETER["false_northing",0], +
//	       AUTHORITY["EPSG","32632"], +
//	       AXIS["Easting",EAST], +
//	       AXIS["Northing",NORTH]] +

	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Coordinate oldCoord = new Coordinate(9,42);
		System.out.println(KiDUtils.tranformGeo_WGS84_2_WGS8432N(oldCoord));
		System.out.println(KiDUtils.transformGeo_WGS84_2_DHDNGK4(oldCoord));
	}

}
