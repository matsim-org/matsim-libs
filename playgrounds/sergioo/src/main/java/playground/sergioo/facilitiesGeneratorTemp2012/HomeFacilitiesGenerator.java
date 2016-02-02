package playground.sergioo.facilitiesGeneratorTemp2012;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class HomeFacilitiesGenerator {
	
	//Constants
	private static final String HOME_FACILITIES_FILE = "./data/currentSimulation/facilities/homeFacilities.xml";
	
	//Methods
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("Home facilities Singapore");
		DataBaseAdmin dataBaseMATSim2  = new DataBaseAdmin(new File("./data/facilities/DataBaseMATSim2.properties"));
		ResultSet homeFacilitiesResult = dataBaseMATSim2.executeQuery("SELECT postal_code,units as capacity,x_utm48n,y_utm48n FROM matsim2.residential_facilities INNER JOIN postal_codes.pcode_zone_xycoords ON zip = postal_code");
		while(homeFacilitiesResult.next()) {
			if(homeFacilitiesResult.getDouble(2)!=0) {
				ActivityFacility facility = facilities.getFacilities().get(Id.create(homeFacilitiesResult.getInt(1), ActivityFacility.class));
				if(facility == null)
					facility = facilities.createAndAddFacility(Id.create(homeFacilitiesResult.getInt(1), ActivityFacility.class), new Coord(homeFacilitiesResult.getDouble(3), homeFacilitiesResult.getDouble(4)));
				ActivityOption option = facility.getActivityOptions().get("home");
				double capacity = 0;
				if(option==null)
					option = ((ActivityFacilityImpl) facility).createAndAddActivityOption("home");
				else
					capacity = option.getCapacity();
				option.setCapacity(capacity+homeFacilitiesResult.getDouble(2));
				option.addOpeningTime(new OpeningTimeImpl(0, 86400));
			}
		}
		new FacilitiesWriter(facilities).write(HOME_FACILITIES_FILE);
		writeFacilitiesOnDatabase(facilities);
	}
	private static void writeFacilitiesOnDatabase(ActivityFacilitiesImpl facilities) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseFacilities  = new DataBaseAdmin(new File("./data/facilities/DataBaseFacilities.properties"));
		ResultSet numResult = dataBaseFacilities.executeQuery("SELECT COUNT(*) FROM Facilities");
		numResult.next();
		int facilityPos=numResult.getInt(1);
		numResult.close();
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			int idFacility;
			ResultSet facilityResult = dataBaseFacilities.executeQuery("SELECT id FROM Facilities WHERE external_id ="+facility.getId().toString());
			if(!facilityResult.next()) {
				facilityPos++;
				idFacility = facilityPos;
				dataBaseFacilities.executeStatement("INSERT INTO Facilities (x,y,external_id) VALUES ("+facility.getCoord().getX()+","+facility.getCoord().getY()+","+facility.getId().toString()+")");
			}
			else
				idFacility = facilityResult.getInt(1);
			facilityResult.close();
			for(ActivityOption option:facility.getActivityOptions().values()) {
				ResultSet optionResult = dataBaseFacilities.executeQuery("SELECT capacity FROM Activity_options WHERE type='"+option.getType()+"' AND facility_id ="+idFacility);
				if(!optionResult.next())
					dataBaseFacilities.executeStatement("INSERT INTO Activity_options (type,facility_id,capacity) VALUES ('"+option.getType()+"',"+idFacility+","+option.getCapacity()+")");
				else
					dataBaseFacilities.executeStatement("UPDATE Activity_options SET capacity=capacity+"+optionResult.getDouble(1)+" WHERE type='"+option.getType()+"' AND facility_id ="+idFacility);
				for(OpeningTime openingTime:option.getOpeningTimes())
					dataBaseFacilities.executeStatement("INSERT INTO Opening_times (day_type,start_time,end_time,type,facility_id) VALUES ('"+openingTime.getStartTime()+","+openingTime.getEndTime()+",'"+option.getType()+"',"+idFacility+")");
			}
		}
	}
	
}
