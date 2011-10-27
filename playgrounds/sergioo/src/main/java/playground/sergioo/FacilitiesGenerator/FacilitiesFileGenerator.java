package playground.sergioo.FacilitiesGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import playground.sergioo.dataBase.DataBaseAdmin;

public class FacilitiesFileGenerator {

	//Attributes

	//Methods

	public static void main(String[] args) {
		try {
			generateActivityTypes();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void generateActivityTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBase.properties"));
		
	}
	
}
