package playground.dziemke.other;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;



//changed file location to input/potsdam instead of potsdam only and named it ...2
//changed osm to osm.pbf
//deleted Links2ESRIShape import line

public class Xml2Shp {

   public static void main(String[] args) {

	   // get String from coordinate transformation from here:
	   // CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]"); 

	   // calls method "main" of class "Links2ESRIShape" with
	   // "netfile = args[0]", "outputFileLs = args[1]", "outputFileP =args[2]", and "defaultCRS = args[3]"
      
	   // Links2ESRIShape.main(new String[]{"../../matsim/input/network.xml","input/networkline.shp","input/networkpoly.shp", crs});
//	   Links2ESRIShape.main(new String[]{"D:/Workspace/container/demand/input/network/bb_5_v_notscaled.xml",
//			   "D:/Workspace/container/demand/input/network/bb_5_v_notscaled_line_DHDN_GK4.shp",
//			   "D:/Workspace/container/demand/input/network/bb_5_v_notscaled_poly_DHDN_GK4.shp",
//			   "DHDN_GK4"
			   Links2ESRIShape.main(new String[]{"D:/Workspace/matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz",
					   "D:/VSP-MAXess/network_line.shp",
					   "D:/VSP-MAXess/network_poly.shp",
					   TransformationFactory.WGS84
			   // "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]"
			   });
	   // open the file and re-save the layer with CRS 25833. Then it should fit with the "gemeinden_xy.shp" file
   }
}