package saleem.stockholmmodel.utils;


import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;
/**
 * Converter factory for various conversion from Geotools to MATSim and vice versa.
 *
 * @author Mohammad Saleem
 *
 */
public class StockholmMGC extends MGC{
	private final static Logger log = Logger.getLogger(MGC.class);

	public static final GeometryFactory geoFac = new GeometryFactory();

	private final static Map<String, String> COORDINATE_REFERENCE_SYSTEMS = new HashMap<>();

	static {
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84,
				"EPSG:4326");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM47S,
				"PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM35S, // south-africa
				"PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM36S, // South Africa (eThekwini)
				"PROJCS[\"WGS_1984_UTM_Zone_36S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",33.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM33N, // berlin
				"PROJCS[\"UTM Zone 33, Northern Hemisphere\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",15],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.DHDN_GK4, // Berlin
				"EPSG:31468");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM29N, // Coimbra, Portugal
				"PROJCS[\"WGS_1984_UTM_Zone_29N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-9],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0.0],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.CH1903_LV03_GT, "PROJCS[\"Hotine_Oblique_Mercator_Azimuth_Center\",GEOGCS[\"Bessel" +
				"1841\",DATUM[\"D_unknown\",SPHEROID[\"bessel\",6377397.155,299.1528128]]" +
				",PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[" +
				"\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"latitude_of_center\",46.95240555555556]" +
				",PARAMETER[\"longitude_of_center\",7.439583333333333],PARAMETER[\"azimuth\",90],PARAMETER[" +
				"\"scale_factor\",1],PARAMETER[\"false_easting\",600000],PARAMETER[\"false_northing\",200000],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.CH1903_LV03_Plus_GT, "PROJCS[\"Hotine_Oblique_Mercator_Azimuth_Center\",GEOGCS[\"Bessel" +
				"1841\",DATUM[\"D_unknown\",SPHEROID[\"bessel\",6377397.155,299.1528128]]" +
				",PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[" +
				"\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"latitude_of_center\",46.95240555555556]" +
				",PARAMETER[\"longitude_of_center\",7.439583333333333],PARAMETER[\"azimuth\",90],PARAMETER[" +
				"\"scale_factor\",1],PARAMETER[\"false_easting\",2600000],PARAMETER[\"false_northing\",1200000],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_Albers, // South Africa (Africa Albers equal area conic)
				"PROJCS[\"Africa_Albers_Equal_Area_Conic\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",25.0],PARAMETER[\"Standard_Parallel_1\",20.0],PARAMETER[\"Standard_Parallel_2\",-23.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_SA_Albers, // South Africa (Adapted version of Africa Albers equal area conic)
				"PROJCS[\"South_Africa_Albers_Equal\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers_Conic_Equal_Area\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",24.0],PARAMETER[\"Standard_Parallel_1\",-18.0],PARAMETER[\"Standard_Parallel_2\",-32.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM48N, // Singapore
				"PROJCS[\"WGS_1984_UTM_Zone_48N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",105.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_SVY21, // Singapore2
				"PROJCS[\"SVY21\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",28001.642],PARAMETER[\"False_Northing\",38744.572],PARAMETER[\"Central_Meridian\",103.8333333333333],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",1.366666666666667],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.NAD83_UTM17N, // Toronto, Canada - UTM_NAD1983_Zone17N
				"PROJCS[\"NAD_1983_UTM_Zone_17N\",GEOGCS[\"GCS_North_American_1983\",DATUM[\"D_North_American_1983\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",-81.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_TM, //Singapore3
				"PROJCS[\"WGS_1984_Transverse_Mercator\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",28001.642],PARAMETER[\"False_Northing\",38744.572],PARAMETER[\"Central_Meridian\",103.8333333333333],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",1.366666666666667],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.PCS_ITRF2000_TM_UOS, // South Korea - but used by University of Seoul - probably a wrong one. !NEW!: Replaced by the correct one! TODO: probably needs to be renamed but since UOS use that already let's keep it.
				"PROJCS[\"Korean 1985 Katech(TM128)\",GEOGCS[\"GCS_Korean_Datum_1985\",DATUM[\"D_Korean_Datum_1985\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",400000.0],PARAMETER[\"False_Northing\",600000.0],PARAMETER[\"Central_Meridian\",128.0],PARAMETER[\"Scale_Factor\",0.9999],PARAMETER[\"Latitude_Of_Origin\",38.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(StockholmTransformationFactory.WGS84_RT90, // Stockholm
				"PROJCS[\"RT90 2.5 gon V\", GEOGCS[\"RT90\", DATUM[\"Rikets_koordinatsystem_1990\", SPHEROID[\"Bessel 1841\",6377397.155,299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], AUTHORITY[\"EPSG\",\"6124\"]], PRIMEM[\"Greenwich\",0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\",0.01745329251994328, AUTHORITY[\"EPSG\",\"9122\"]], AUTHORITY[\"EPSG\",\"4124\"]], UNIT[\"metre\",1, AUTHORITY[\"EPSG\",\"9001\"]], PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"latitude_of_origin\",0], PARAMETER[\"central_meridian\",15.80827777777778], PARAMETER[\"scale_factor\",1], PARAMETER[\"false_easting\",1500000], PARAMETER[\"false_northing\",0], AUTHORITY[\"EPSG\",\"3021\"], AXIS[\"Y\",EAST], AXIS[\"X\",NORTH]]");
		COORDINATE_REFERENCE_SYSTEMS.put(StockholmTransformationFactory.WGS84_SWEREF99, // Stockholm
				"PROJCS[\"SWEREF99 TM\", GEOGCS[\"SWEREF99\", DATUM[\"SWEREF99\", SPHEROID[\"GRS 1980\",6378137,298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0,0,0,0,0,0,0],  AUTHORITY[\"EPSG\",\"6619\"]], PRIMEM[\"Greenwich\",0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\",0.01745329251994328, AUTHORITY[\"EPSG\",\"9122\"]], AUTHORITY[\"EPSG\",\"4619\"]], UNIT[\"metre\",1, AUTHORITY[\"EPSG\",\"9001\"]], PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"latitude_of_origin\",0], PARAMETER[\"central_meridian\",15], PARAMETER[\"scale_factor\",0.9996], PARAMETER[\"false_easting\",500000], PARAMETER[\"false_northing\",0], AUTHORITY[\"EPSG\",\"3006\"], AXIS[\"y\",EAST], AXIS[\"x\",NORTH]]");
		COORDINATE_REFERENCE_SYSTEMS.put(StockholmTransformationFactory.WGS84_EPSG3857, // For Stockholm Population File
				"EPSG:3857");

	}

	public static CoordinateReferenceSystem getCRS(final String wktOrAuthorityCodeOrShorthandName) {
		String wktOrAuthorityCode = COORDINATE_REFERENCE_SYSTEMS.get(wktOrAuthorityCodeOrShorthandName);
		if (wktOrAuthorityCode == null) {
			wktOrAuthorityCode = wktOrAuthorityCodeOrShorthandName;
		}
		CoordinateReferenceSystem crs;
		try {
			crs = CRS.parseWKT(wktOrAuthorityCode);
		} catch (FactoryException fe) {
			try {
				log.warn("Assuming that coordinates are in longitude first notation, i.e. (longitude, latitude).");
				crs = CRS.decode(wktOrAuthorityCode, true);
			} catch (FactoryException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return crs;
	}
}
