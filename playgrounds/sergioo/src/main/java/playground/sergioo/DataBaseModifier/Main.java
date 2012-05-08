package playground.sergioo.DataBaseModifier;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class Main {

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
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		dataBaseAux.executeStatement("CREATE TABLE work_activity_types (name varchar(50), start_time DOUBLE, duration DOUBLE)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0645_0815', 24300, 29700)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0700_1045', 25200, 38700)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0700_1230', 25200, 45000)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0715_1430', 26100, 52200)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0730_0945', 27000, 35100)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0815_0900', 29700, 32400)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0815_1130', 29700, 41400)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0830_1030', 30600, 37800)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0845_0930', 31500, 34200)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0900_1230', 32400, 45000)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0930_0900', 34200, 32400)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_0945_0745', 35100, 27900)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_1030_1045', 37800, 38700)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_1400_0830', 50400, 30600)");
		dataBaseAux.executeStatement("INSERT INTO work_activity_types VALUES('w_2015_0945', 72900, 35100)");
		dataBaseAux.close();
		
	}

}
