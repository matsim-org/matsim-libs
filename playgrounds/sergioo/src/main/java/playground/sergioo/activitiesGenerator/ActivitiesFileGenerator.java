package playground.sergioo.activitiesGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import playground.sergioo.dataBase.DataBaseAdmin;

public class ActivitiesFileGenerator {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		
	}

}
