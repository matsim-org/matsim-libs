package playground.artemc.scenarioTools;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;



public class FacilityTransitStopDistanceCalculator {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/SiouxFalls_Dempo.properties"));
		ResultSet facilities = dba.executeQuery("SELECT * FROM u_artemc.sf_facilities");
		ResultSet busStops = dba.executeQuery("SELECT * FROM u_artemc.siouxfalls_busstops");
		
		
		HashMap<String, double[]> stops = new HashMap<String, double[]>();
		createColumnSQL(dba);
		
		while(busStops.next()){
			double[] coord = new double[2];
			coord[0] = busStops.getDouble("x");
			coord[1] = busStops.getDouble("y");
			stops.put(busStops.getString("id"), coord);
		}
		
		while(facilities.next()){
			double x = facilities.getDouble("x");
			double y = facilities.getDouble("y");
		
			double shortestDistance = 999999.0;
			String closestStop = "";
			for(String stopId:stops.keySet()){
				double distance = Math.sqrt((stops.get(stopId)[0] - x)*(stops.get(stopId)[0] - x)+(stops.get(stopId)[1] - y)*(stops.get(stopId)[1] - y));
				if(distance<shortestDistance){
					shortestDistance = distance;
					closestStop = stopId;
				}
			}
				
			System.out.println(facilities.getString("id")+"   "+closestStop+"  "+shortestDistance);
			
			dba.executeStatement(String.format("UPDATE %s SET transit_distance = %s WHERE id = '%s';",
					"u_artemc.sf_facilities ", shortestDistance, facilities.getString("id")));	
		}
		
		dba.close();
	}
	
	public static void createColumnSQL(DataBaseAdmin dba) throws SQLException, NoConnectionException{
		String columnName = "transit_distance";
		try {
			dba.executeStatement(String.format("ALTER TABLE %s DROP COLUMN %s;",
					"u_artemc.sf_facilities ",columnName));
		} catch (SQLException e) {
			System.err.println("Column "+columnName+" doesn't exist.");
		} 
		
		dba.executeStatement(String.format("ALTER TABLE %s ADD COLUMN %s real;",
					"u_artemc.sf_facilities ",columnName));
	}

}
