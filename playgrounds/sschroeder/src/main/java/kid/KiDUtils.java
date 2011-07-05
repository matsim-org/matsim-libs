package kid;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;

public class KiDUtils {
	
	public static Coordinate getFromGeocode(TransportLeg leg) {
		double longitude = Double.parseDouble(leg.getAttributes().get(KiDSchema.LEG_QUELLADRESSE_GEO_LONG));
		double latitude = Double.parseDouble(leg.getAttributes().get(KiDSchema.LEG_QUELLADRESSE_GEO_LAT));
		return new Coordinate(longitude, latitude);
	}
	
	public static Coordinate getToGeocode(TransportLeg leg) {
		double longitude = Double.parseDouble(leg.getAttributes().get(KiDSchema.LEG_ZIELADRESSE_GEO_LONG));
		double latitude = Double.parseDouble(leg.getAttributes().get(KiDSchema.LEG_ZIELADRESSE_GEO_LAT));
		return new Coordinate(longitude, latitude);
	}
	
	public static Coordinate transformGeo_WGS84_2_DHDNGK4(Coordinate coord) {
		GeotoolsTransformation geoTransformation;
		try {
			geoTransformation = new GeotoolsTransformation(DefaultGeographicCRS.WGS84, CRS.decode("EPSG:31464"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		}
		return geoTransformation.transform(coord);
	}
	
	public static GeotoolsTransformation createTransformation_WGS84ToWGS84UTM33N(){
		GeotoolsTransformation geoTransformation;
		try {
			geoTransformation = new GeotoolsTransformation(DefaultGeographicCRS.WGS84, CRS.decode("EPSG:32633"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		}
		return geoTransformation;
	}
	
	public static GeotoolsTransformation createTransformation_WGS84ToWGS84UTM32N(){
		GeotoolsTransformation geoTransformation;
		try {
			geoTransformation = new GeotoolsTransformation(DefaultGeographicCRS.WGS84, CRS.decode("EPSG:32632"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		}
		return geoTransformation;
	}
	
	public static Coordinate tranformGeo_WGS84_2_WGS8432N(Coordinate coord) {
		GeotoolsTransformation geoTransformation;
		try {
			geoTransformation = new GeotoolsTransformation(DefaultGeographicCRS.WGS84, CRS.decode("EPSG:32632"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException("cannot transform coordinate. error: " + e);
		}
		return geoTransformation.transform(coord);
	}

	public static boolean isGeoCodable(ScheduledTransportChain chain) {
		if(chain.getTransportLegs().size() == 0){
			return false;
		}
		for(TransportLeg leg : chain.getTransportLegs()){
			Coordinate from = getFromGeocode(leg);
			Coordinate to = getToGeocode(leg);
			boolean fromIsGeoCodable = coordinateIsGeoCodable(from);
			boolean toIsGeoCodable = coordinateIsGeoCodable(to);
			if(!(fromIsGeoCodable && toIsGeoCodable)){
				return false;
			}
		}
		return true;
	}
	
	public static boolean isCodableInTimeAndSpace(ScheduledTransportChain chain) {
		if(chain.getTransportLegs().size() == 0){
			return false;
		}
		boolean firstLeg = true;
		for(TransportLeg leg : chain.getTransportLegs()){
			Coordinate from = getFromGeocode(leg);
			Coordinate to = getToGeocode(leg);
			Integer depTime = getDepartureTimeInSeconds(leg);
			boolean fromIsGeoCodable = coordinateIsGeoCodable(from);
			boolean toIsGeoCodable = coordinateIsGeoCodable(to);
			if(depTime == null){
				return false;
			}
			if(!(fromIsGeoCodable && toIsGeoCodable)){
				return false;
			}
		}
		return true;
	}
	
	private static boolean coordinateIsGeoCodable(Coordinate coord) {
		if(coord.x > 0 && coord.y > 0){
			return true;
		}
		return false;
	}
	
	public static String getDestinationLocationType(TransportLeg leg) {
		return leg.getAttributes().get(KiDSchema.LEG_DESTINATION_LOCATIONTYPE);
	}

	public static String getSourceLocationType(TransportLeg leg) {
		return leg.getAttributes().get(KiDSchema.LEG_SOURCE_LOCATIONTYPE);
	}

	public static String getDepartureTime(TransportLeg leg) {
		return leg.getAttributes().get(KiDSchema.LEG_DEPARTURETIME);
	}
	
	public static Integer getDepartureTimeInSeconds(TransportLeg leg) {
		int hourIndex = 0;
		int minuteIndex = 1;
		String time = getDepartureTime(leg);
		String[] timeTokens = time.split(":");
		if(timeTokens[hourIndex].equals("-1")){
			return null;
		}
		Integer hours = null;
		Integer minutes = null;
		if(timeTokens[hourIndex].startsWith("0")){
			hours = Integer.parseInt(timeTokens[hourIndex].substring(1));
		}
		else{
			hours = Integer.parseInt(timeTokens[hourIndex]);
		}
		
		if(timeTokens[minuteIndex].startsWith("0")){
			minutes = Integer.parseInt(timeTokens[minuteIndex].substring(1));
		}
		else{
			minutes = Integer.parseInt(timeTokens[minuteIndex]);
		}
		return hours*3600 + minutes*60;
	}

	public static String getDate(TransportLeg leg) {
		return leg.getAttributes().get(KiDSchema.LEG_ARRIVALDATE);
	}

	public static String getArrivalTime(TransportLeg leg) {
		return leg.getAttributes().get(KiDSchema.LEG_ARRIVALTIME);
	}

	public static String getActivity(TransportLeg leg) {
		return leg.getAttributes().get(KiDSchema.LEG_PURPOSE);
	}

	public static boolean isGeoCodable(ScheduledVehicle vehicle) {
		if(vehicle.getScheduledTransportChains().isEmpty()){
			return false;
		}
		for(ScheduledTransportChain tChain : vehicle.getScheduledTransportChains()){
			if(!isGeoCodable(tChain)){
				return false;
			}
		}
		return true;
	}

	public static Collection<Coordinate> getGeocodesFromActivities(ScheduledTransportChain tChain) {
		Collection<Coordinate> coordinates = new ArrayList<Coordinate>();
		boolean firstLeg = true;
		TransportLeg lastLeg = null;
		for(TransportLeg leg : tChain.getTransportLegs()){
			Coordinate activityCoord = null;
			if(firstLeg){
				activityCoord = getFromGeocode(leg);
				firstLeg = false;
				lastLeg = leg;
			}
			else{
				activityCoord = getToGeocode(lastLeg);
				lastLeg = leg;
			}
			coordinates.add(activityCoord);
		}
		Coordinate coord = getToGeocode(lastLeg);
		coordinates.add(coord);
		return coordinates;
	}

	public static boolean isCodableInTimeAndSpace(ScheduledVehicle vehicle) {
		if(vehicle.getScheduledTransportChains().isEmpty()){
			return false;
		}
		for(ScheduledTransportChain tChain : vehicle.getScheduledTransportChains()){
			if(!isCodableInTimeAndSpace(tChain)){
				return false;
			}
		}
		return true;
	}
}
