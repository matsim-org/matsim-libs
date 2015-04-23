package playground.dziemke.other;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class Xml2Shp {

   public static void main(String[] args) {

	   // example how to use a user-defined CRS
	   // CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]"); 

	   // calls method "main" of class "Links2ESRIShape" with
	   // "netfile = args[0]", "outputFileLs = args[1]", "outputFileP =args[2]", and "defaultCRS = args[3]"
      
//	   Links2ESRIShape.main(new String[]{"D:/Workspace/container/demand/input/network/bb_5_v_notscaled.xml",
//			   "D:/Workspace/container/demand/input/network/bb_5_v_notscaled_line_DHDN_GK4.shp",
//			   "D:/Workspace/container/demand/input/network/bb_5_v_notscaled_poly_DHDN_GK4.shp",
//			   TransformationFactory.DHDN_GK4
//	   });
	   // open the file and re-save the layer with CRS 25833. Then it should fit with the "gemeinden_xy.shp" file
	   
	   Links2ESRIShape.main(new String[]{"../../matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz",
			   "../../data/nmbm/network/network_line.shp",
			   "../../data/nmbm/network/network_poly.shp",
			   TransformationFactory.WGS84_SA_Albers
			   // "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]"
	   });
	   
	   // following taken from MGC.java
	   // this notation is called WKT format: http://de.wikipedia.org/wiki/European_Petroleum_Survey_Group_Geodesy#EPSG-Codes
	   // transformations.put(TransformationFactory.WGS84_Albers, // South Africa (Africa Albers equal area conic)
	   // "PROJCS[\"Africa_Albers_Equal_Area_Conic\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",25.0],PARAMETER[\"Standard_Parallel_1\",20.0],PARAMETER[\"Standard_Parallel_2\",-23.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		
	   // transformations.put(TransformationFactory.WGS84_SA_Albers, // South Africa (Adapted version of Africa Albers equal area conic)
	   // "PROJCS[\"South_Africa_Albers_Equal\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers_Conic_Equal_Area\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",24.0],PARAMETER[\"Standard_Parallel_1\",-18.0],PARAMETER[\"Standard_Parallel_2\",-32.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		
	   // the same information in PROJ.4 format: http://de.wikipedia.org/wiki/European_Petroleum_Survey_Group_Geodesy#EPSG-Codes
	   // USER:100000 -  * Generated CRS (+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs)
   }
}