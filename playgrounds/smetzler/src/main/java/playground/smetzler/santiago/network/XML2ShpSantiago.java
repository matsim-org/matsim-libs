package playground.smetzler.santiago.network;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class XML2ShpSantiago {

	public static void main(String[] args) {

		String inputXML = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/santiagoSeconday&PT.xml";
		String outputLineSHP = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/santiago_wktlong_line_PSAD56.shp";
		String outputPolySHP = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/santiago_wktlong_poly_PSAD56.shp";

		Links2ESRIShape.main(new String[]
				{inputXML,
				outputLineSHP,
				outputPolySHP,

				//WKT von "PSAD56 zone 19S"
				"PROJCS[\"PSAD56 / UTM zone 19S\",GEOGCS[\"PSAD56\",DATUM[\"Provisional South American Datum 1956\",SPHEROID[\"International 1924\", 6378388.0, 297.0,AUTHORITY[\"EPSG\",\"7022\"]],TOWGS84[-307.7, 265.3, -363.5, 0.0, 0.0, 0.0, 0.0],AUTHORITY[\"EPSG\",\"6248\"]],PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Geodetic longitude\", EAST],AXIS[\"Geodetic latitude\", NORTH],AUTHORITY[\"EPSG\",\"4248\"]],PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]],PARAMETER[\"central_meridian\", -69.0],PARAMETER[\"latitude_of_origin\", 0.0],PARAMETER[\"scale_factor\", 0.9996],PARAMETER[\"false_easting\", 500000.0],PARAMETER[\"false_northing\", 10000000.0],UNIT[\"m\", 1.0],AXIS[\"Easting\", EAST],AXIS[\"Northing\", NORTH],AUTHORITY[\"EPSG\",\"24879\"]]"

				//"PROJCS[\"PSAD56 / UTM zone 19S\",GEOGCS[\"PSAD56\",DATUM[\"D_Provisional_S_American_1956\",SPHEROID[\"International_1924\",6378388,297]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-69],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]"
				});   
	}
}