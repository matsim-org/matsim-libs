package playground.artemc.hits;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class AssignStopsToHitsTester {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException{
		AssignStopsToHITS st = new AssignStopsToHITS();
		ArrayList<String> stopList = st.findStopIDsBus("196");
		StopFinder sf = new StopFinder();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		Coord coordStart = new CoordImpl(103.777885, 1.295161);
		Coord UTMStart = ct.transform(coordStart);
		double stopLon=UTMStart.getX();
		double stopLat=UTMStart.getY();
		
		String closestStop = sf.findClosestStop(stopList, stopLon, stopLat); 

		System.out.println(closestStop);

		String facilityDensities[] = new String[29];

		DataBaseAdmin dbaWorkFacilities = new DataBaseAdmin(new File("./data/dataBases/KrakatauWorkFacilities.properties"));
		ResultSet densities = dbaWorkFacilities.executeQuery("SELECT * FROM StopDensities WHERE stop_id ="+closestStop);
		
			while(densities.next()){
				for(int l=0;l<29;l++){
					facilityDensities[l] = densities.getString(l+9);
					System.out.println(facilityDensities[l]);
				}
			}
		}
		
		
}
	

