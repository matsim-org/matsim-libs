package playground.smetzler.bikeDemand;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class XML2Shpfile {

	public static void main(String[] args) {

		String inputXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/tempelhof_MATsim_sepNet_bikeOnly_CustomFreespeedwSlopeOppo.xml";
		String outputLineSHP = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/tempelhof_MATsim_line.shp";
		String outputPolySHP = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/tempelhof_MATsim_poly.shp";

		Links2ESRIShape.main(new String[]
				{inputXML,
				outputLineSHP,
				outputPolySHP,
				
				"EPSG:31468" //DHDN_GK4 
				//WKT von "PSAD56 zone 19S"
//				"PROJCS[\"PSAD56 / UTM zone 19S\",GEOGCS[\"PSAD56\",DATUM[\"Provisional South American Datum 1956\",SPHEROID[\"International 1924\", 6378388.0, 297.0,AUTHORITY[\"EPSG\",\"7022\"]],TOWGS84[-307.7, 265.3, -363.5, 0.0, 0.0, 0.0, 0.0],AUTHORITY[\"EPSG\",\"6248\"]],PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Geodetic longitude\", EAST],AXIS[\"Geodetic latitude\", NORTH],AUTHORITY[\"EPSG\",\"4248\"]],PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]],PARAMETER[\"central_meridian\", -69.0],PARAMETER[\"latitude_of_origin\", 0.0],PARAMETER[\"scale_factor\", 0.9996],PARAMETER[\"false_easting\", 500000.0],PARAMETER[\"false_northing\", 10000000.0],UNIT[\"m\", 1.0],AXIS[\"Easting\", EAST],AXIS[\"Northing\", NORTH],AUTHORITY[\"EPSG\",\"24879\"]]"

				//"PROJCS[\"PSAD56 / UTM zone 19S\",GEOGCS[\"PSAD56\",DATUM[\"D_Provisional_S_American_1956\",SPHEROID[\"International_1924\",6378388,297]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-69],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]"
				});   
	}
}