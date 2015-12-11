package playground.sergioo.facilitiesGenerator2012;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class DBToFacilitiesFile {

	/**
	 * 
	 * @param args
	 * 0 - Database properties file
	 * 1 - Facilities table
	 * 2 - Facilities file
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		ActivityFacilitiesFactory factory = new ActivityFacilitiesFactoryImpl();
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		DataBaseAdmin dataBase  = new DataBaseAdmin(new File(args[0]));
		ResultSet resultSet = dataBase.executeQuery("SELECT * FROM "+args[1]);
		while(resultSet.next()) {
			Id<ActivityFacility> id = Id.create(resultSet.getInt(1), ActivityFacility.class);
			ActivityFacility facility = facilities.getFacilities().get(id);
			if(facility == null) {
				facility = factory.createActivityFacility(id, new Coord(resultSet.getDouble(2), resultSet.getDouble(3)));
				((ActivityFacilityImpl)facility).setDesc(resultSet.getString(4));
				facilities.addActivityFacility(facility);
			}
			String type = resultSet.getString(5);
			if(!type.equals("total")) {
				if(type.equals("social"))
					type = "leisure";
				double capacity;
				if(type.startsWith("w_")||type.endsWith("school"))
					capacity = resultSet.getDouble(6);
				else
					capacity = resultSet.getDouble(7);
				ActivityOption option = facility.getActivityOptions().get(type);
				if(option == null) {
					option = factory.createActivityOption(type);
					option.setCapacity(0);
					facility.addActivityOption(option);
				}
				option.setCapacity(option.getCapacity() + capacity);
				option.addOpeningTime(new OpeningTimeImpl(resultSet.getDouble(8), resultSet.getDouble(9)));
			}
		}
		new FacilitiesWriter(facilities).write(args[2]);
	}

}
