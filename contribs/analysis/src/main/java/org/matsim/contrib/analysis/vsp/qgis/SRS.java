package org.matsim.contrib.analysis.vsp.qgis;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class SRS{
	
	private String proj4;
	private String srsid;
	private String srid;
	private String authid;
	private String description;
	private String projectionacronym;
	private String ellipsoidacronym;

	private SRS(String proj4, String srsid, String srid, String authid, String description, String projectionacronym, String ellipsoidacronym){
		this.proj4 = proj4;
		this.srsid = srsid;
		this.srid = srid;
		this.authid = authid;
		this.description = description;
		this.projectionacronym = projectionacronym;
		this.ellipsoidacronym = ellipsoidacronym;
	}

	/**
	 * Creates a new SRS from an input string (e.g. {@code TransformationFactory.WGS84}).
	 * 
	 * @param srs String representation of the spatial reference system you want to use here
	 * @return
	 */
	public static SRS createSpatialRefSys(String srs){
		
		if(srs.equals(TransformationFactory.DHDN_GK4)){
			
			return new SRS("+proj=tmerc +lat_0=0 +lon_0=12 +k=1 +x_0=4500000 +y_0=0 +ellps=bessel +towgs84=598.1,73.7,418.2,0.202,0.045,-2.455,6.7 +units=m +no_defs",
					"2648", "31468", "EPSG:31468", "DHDN / Gauss-Kruger zone 4", "tmerc", "bessel");
			
		} else if(srs.equals(TransformationFactory.WGS84)){
			
			return new SRS("+proj=longlat +datum=WGS84 +no_defs",
					"3452", "4326", "EPSG:4326", "WGS 84", "longlat", "WGS84");
			
		} else if(srs.equals(TransformationFactory.WGS84_SA_Albers)){
			
			return new SRS("+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs",
					"100000", "0", "USER:100000", "WGS84_SA_Albers", "aea", "");
			
		} else if(srs.equals("WGS84_Pseudo_Mercator")){
			
			return new SRS("+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs",
					"3857", "3857", "EPSG:3857", "WGS 84 / Pseudo Mercator", "merc", "WGS84");
		
			// TODO the following is a quick fixing to make ESPG:21037 useable; right now I don't
			// see, however, why we need this class instead of just using the coordinate systems from MGC
		} else if(srs.equals("EPSG:21037")){
			
			return new SRS("+proj=utm +zone=37 +south +ellps=clrk80 +units=m +no_defs",
					"21037", "21037", "EPSG:21037", "Arc 1960 / UTM zone 3Ss", "", "");
			
			// TODO the following is a quick fixing to make TransformationFactory.WGS84_UTM31N useable; right now I don't
			// see, however, why we need this class instead of just using the coordinate systems from MGC
		} else if(srs.equals(TransformationFactory.WGS84_UTM31N)){ // EPSG:32641, e.g. for Hasselt

			return new SRS("+proj=utm +zone=31 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",
					"32631", "32631", "EPSG:32631",  "WGS 84 / UTM zone 31N", "", "");

			// TODO the following is a quick fixing to make TransformationFactory.WGS84_UTM33N useable... see above
		} else if(srs.equals(TransformationFactory.WGS84_UTM33N)){ // EPSG:32641, e.g. for Hasselt

			return new SRS("+proj=utm +zone=33 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",
					"32633", "32633", "EPSG:32633",  "WGS 84 / UTM zone 33N", "", "");

		} else {
			
			throw new RuntimeException("Unsupported coordinate system.");
			
		}
		
	}
	
	public String getProj4() {
		return proj4;
	}

	public String getSrsid() {
		return srsid;
	}

	public String getSrid() {
		return srid;
	}

	public String getAuthid() {
		return authid;
	}

	public String getDescription() {
		return description;
	}

	public String getProjectionacronym() {
		return projectionacronym;
	}

	public String getEllipsoidacronym() {
		return ellipsoidacronym;
	}
	
}