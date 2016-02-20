package playground.sergioo.facilitiesGeneratorTemp2012;

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
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class EducationalFacilitiesGenerator {

	//Constants
	private static final String EDUCATIONAL_FACILITIES_FILE = "./data/currentSimulation/facilities/educationalFacilities.xml";
	
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
		ResultSet educationalFacilitiesResult = dataBaseMATSim2.executeQuery("SELECT post_code,total_units,workplaces,longitude,latitude,id_education_facility,type FROM matsim2.educational_facilities");
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
					ActivityOption option = facility.getActivityOptions().get("edu");
					double capacity = 0;
					if(option==null)
						option = ((ActivityFacilityImpl) facility).createAndAddActivityOption("edu");
					else
						capacity = option.getCapacity();
					option.setCapacity(((int)(capacity+educationalFacilitiesResult.getDouble(2))));
					if(educationalFacilitiesResult.getString(7)!= null && (educationalFacilitiesResult.getString(7).toLowerCase().contains("school") || educationalFacilitiesResult.getString(7).toLowerCase().contains("college") || educationalFacilitiesResult.getString(7).toLowerCase().contains("primary") || educationalFacilitiesResult.getString(7).toLowerCase().contains("secondary") || educationalFacilitiesResult.getString(7).toLowerCase().contains("kinder")))
						option.addOpeningTime(new OpeningTimeImpl(18000, 64800));
					else
						option.addOpeningTime(new OpeningTimeImpl(18000, 86400));
					option = facility.getActivityOptions().get("profess");
					capacity = 0;
					if(option==null)
						option = ((ActivityFacilityImpl) facility).createAndAddActivityOption("profess");
					else
						capacity = option.getCapacity();
					option.setCapacity(((int)(capacity+educationalFacilitiesResult.getDouble(3))));
					if(educationalFacilitiesResult.getString(7)!= null && (educationalFacilitiesResult.getString(7).toLowerCase().contains("school") || educationalFacilitiesResult.getString(7).toLowerCase().contains("college") || educationalFacilitiesResult.getString(7).toLowerCase().contains("primary") || educationalFacilitiesResult.getString(7).toLowerCase().contains("secondary") || educationalFacilitiesResult.getString(7).toLowerCase().contains("kinder")))
						option.addOpeningTime(new OpeningTimeImpl(18000, 64800));
					else
						option.addOpeningTime(new OpeningTimeImpl(18000, 86400));
				}	
			}
		}
		new FacilitiesWriter(facilities).write(EDUCATIONAL_FACILITIES_FILE);
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
					dataBaseFacilities.executeStatement("INSERT INTO Opening_times (day_type,start_time,end_time,type,facility_id) VALUES ('wkday',"+openingTime.getStartTime()+","+openingTime.getEndTime()+",'"+option.getType()+"',"+idFacility+")");
			}
		}
	}
	private static int getPostalCode(Map<Integer, Coord> allPostalCodes, Coord coord) {
		int zip=-1;
		double nearest = Double.MAX_VALUE;
		for(Entry<Integer, Coord> postalCode: allPostalCodes.entrySet()) {
			double distance = CoordUtils.calcEuclideanDistance(postalCode.getValue(), coord);
			if(distance<nearest) {
				zip = postalCode.getKey();
				nearest = distance;
			}
		}
		return zip;
	}
	
}
