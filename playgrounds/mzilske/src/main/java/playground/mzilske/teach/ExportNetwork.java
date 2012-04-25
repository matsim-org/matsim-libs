package playground.mzilske.teach;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class ExportNetwork {
	
	public static void main(String[] args) {
		Links2ESRIShape.main(new String[]{"../../matsim/input/network.xml","input/networkline.shp","input/networkpoly.shp", "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]"});
	}

}
