package playground.sergioo.facilitiesGenerator2012;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.OpeningTime.DayType;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class EducationalFacilitiesGenerator {

	//Constants
	private static final String EDUCATIONAL_FACILITIES_FILE = "./data/currentSimulation/facilities/educationalFacilities.xml";
	private static final String[] EDUCATION_ACTIVITY_TYPES = {"s_0900_0400", "s_0700_1000", "s_0700_0800", "s_0700_1200", "s_0700_1500"};
	private static final Double[] EDUCATION_START_TIMES = {9*3600.0,7*3600.0,7*3600.0,7*3600.0,7*3600.0};
	private static final Double[] EDUCATION_DURATIONS = {4*3600.0,10*3600.0,8*3600.0,12*3600.0,15*3600.0};
	private static final String[][] EDUCATION_FACILITY_TYPES = {{"preschool"}, {"PRIMARY SCHOOL","MIXED LEVEL SCHOOLS","Mixed school"}, {"Secondart school","SECONDARY SCHOOL"}, {"JUNIOR COLLEGE","CENTRALISED INSTITUTE"}, {""}};
	
	//Methods
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBasePostalCodes  = new DataBaseAdmin(new File("./data/facilities/DataBasePostalCodes.properties"));
		Map<Integer, Coord> allPostalCodes = new HashMap<Integer, Coord>();
		ResultSet resultZone = dataBasePostalCodes.executeQuery("SELECT zip,lng,lat FROM postal_codes");
		while(resultZone.next())
			allPostalCodes.put(resultZone.getInt(1), new Coord(resultZone.getDouble(2), resultZone.getDouble(3)));
		resultZone.close();
		dataBasePostalCodes.close();
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("Educational facilities Singapore");
		DataBaseAdmin dataBaseMATSim2  = new DataBaseAdmin(new File("./data/facilities/DataBaseMATSim2.properties"));
		ResultSet educationalFacilitiesResult = dataBaseMATSim2.executeQuery("SELECT post_code,total_units,project_name,longitude,latitude,id_education_facility,type FROM matsim2.educational_facilities");
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		while(educationalFacilitiesResult.next()) {
			if(educationalFacilitiesResult.getDouble(2)!=0) {
				double lat = Math.min(educationalFacilitiesResult.getDouble(4),educationalFacilitiesResult.getDouble(5));
				double lon = Math.max(educationalFacilitiesResult.getDouble(4),educationalFacilitiesResult.getDouble(5));
				int postalCode = educationalFacilitiesResult.getInt(1);
				Coord coord = null;
				if(postalCode==0)
					postalCode = getPostalCode(allPostalCodes, new Coord(lon, lat));
				ActivityFacility facility = facilities.getFacilities().get(Id.create(postalCode, ActivityFacility.class));
				if(facility==null) {
					if(educationalFacilitiesResult.getDouble(4)!=0)
						coord = new Coord(lon, lat);
					else
						coord = allPostalCodes.get(postalCode);
					if(coord!=null)
						facility = facilities.createAndAddFacility(Id.create(postalCode, ActivityFacility.class), coordinateTransformation.transform(coord));
					else
						System.out.println(educationalFacilitiesResult.getInt(6));
				}
				if(facility!=null) {
					((ActivityFacilityImpl)facility).setDesc(educationalFacilitiesResult.getString(3));
					int eduType=-1;
					if(educationalFacilitiesResult.getString(7)==null) {
						eduType=4;
					}
					else {
						FIND_TYPE:
						for(int type=0; type<EDUCATION_FACILITY_TYPES.length; type++)
							for(int t=0; t<EDUCATION_FACILITY_TYPES[type].length; t++)
									if(EDUCATION_FACILITY_TYPES[type][t].equalsIgnoreCase(educationalFacilitiesResult.getString(7))) {
										eduType=type;
										break FIND_TYPE;
									}
					}
					ActivityOption option = facility.getActivityOptions().get(EDUCATION_ACTIVITY_TYPES[eduType]);
					double capacity = 0;
					if(option==null)
						option = ((ActivityFacilityImpl) facility).createAndAddActivityOption(EDUCATION_ACTIVITY_TYPES[eduType]);
					else
						capacity = option.getCapacity();
					option.setCapacity((double)((int)(capacity+educationalFacilitiesResult.getDouble(2))));
					option.addOpeningTime(new OpeningTimeImpl(DayType.wkday, EDUCATION_START_TIMES[eduType], EDUCATION_START_TIMES[eduType]+EDUCATION_DURATIONS[eduType]));
				}	
			}
		}
		new FacilitiesWriter(facilities).write(EDUCATIONAL_FACILITIES_FILE);
	}
	private static int getPostalCode(Map<Integer, Coord> allPostalCodes, Coord coord) {
		int zip=-1;
		double nearest = Double.MAX_VALUE;
		for(Entry<Integer, Coord> postalCode: allPostalCodes.entrySet()) {
			double distance = CoordUtils.calcDistance(postalCode.getValue(), coord);
			if(distance<nearest) {
				zip = postalCode.getKey();
				nearest = distance;
			}
		}
		return zip;
	}
	
}
