package playground.artemc.hits;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Receives a list of stops and using geo-codes from a database finds a closest stop to the coordinates given.
 *
 * @author artemc
 */

public class StopFinder {

	public String findClosestStop(ArrayList<String> stopList, double endLon, double endLat) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		String stop="";
		
		HashMap<String, Tuple<Double, Double>> allBusStops = new HashMap<String, Tuple<Double, Double>>();
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/artemcKrakatau.properties"));
		ResultSet allStops = dba.executeQuery("SELECT stop_id, stop_lat, stop_lon FROM bus_stops");		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		
		while(allStops.next()){
			String stopId=allStops.getString(1);
			double stopLat=allStops.getDouble(2);
			double stopLon=allStops.getDouble(3);

			Coord coordStart = new Coord(stopLon, stopLat);
			Coord UTMStart = ct.transform(coordStart);
			stopLon=UTMStart.getX();
			stopLat=UTMStart.getY();	
			
			allBusStops.put(stopId, new Tuple<Double, Double>(stopLon, stopLat));		
		}
		
		//Choose transit stop
		int count=0;
		String closestStop="";
		double min_distance=0;
		for(String stopId:stopList){
			double stopLon = allBusStops.get(stopId).getFirst();
			double stopLat = allBusStops.get(stopId).getSecond();
			double distance = Math.sqrt((stopLon-endLon)*(stopLon-endLon)+(stopLat-endLat)*(stopLat-endLat));
			if(count==0){
				closestStop=stopId;
				min_distance=distance;
			}
			else if(distance<min_distance)
			{
				closestStop=stopId;
				min_distance=distance;		
			}	
			count++;
		}
		
		//System.out.println(min_distance);
		
		return closestStop;
		
	}

}
