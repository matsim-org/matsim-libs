package playground.sergioo.ctivitiesGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import playground.sergioo.dataBase.DataBaseAdmin;
import playground.sergioo.dataBase.NoConnectionException;

public class ActivitiesFileGenerator {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/ArtemDataBase.properties"));
		long value = 3;
		ResultSet rs = dba.executeQuery("SELECT * FROM BusStops2 WHERE Code="+value);
		while(!rs.next()) {
			String name = rs.getString(1);
			System.out.println(name);
		}
		dba.close();
	}

}
