package playground.pieter.singapore.utils;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;



public class FacilitiesToSQL {
	DataBaseAdmin dba;
	ScenarioImpl scenario;
	
	public FacilitiesToSQL(DataBaseAdmin dba, ScenarioImpl scenario) {
		super();
		this.dba = dba;
		this.scenario = scenario;
	}
	public void createShortSQLFacilityList(String tableName) throws SQLException, NoConnectionException{
		dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",tableName));
		dba.executeStatement(String.format("CREATE TABLE %s(" +
				"id VARCHAR(45)," +
				"x_utm48n DOUBLE," +
				"y_utm48n DOUBLE," +
				"description VARCHAR(255)" +			
				")",tableName));
		ActivityFacilitiesImpl facs = scenario.getActivityFacilities();
		for(ActivityFacility fac:facs.getFacilities().values()){
			ActivityFacilityImpl fi = (ActivityFacilityImpl) fac;
			String id = fi.getId().toString();
			double x =fi.getCoord().getX();
			double y = fi.getCoord().getY();
			String description = fi.getDesc();
			String sqlInserter = "INSERT INTO %s " +
					"VALUES(\'%s\',%f,%f,\'%s\');";
			dba.executeUpdate(String.format(sqlInserter,tableName,id,x,y,description));

		}
	}
	public void createCompleteFacilityAndActivityTable(String tableName) throws SQLException, NoConnectionException{
		dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",tableName));
		dba.executeStatement(String.format("CREATE TABLE %s(" +
				"id VARCHAR(45)," +
				"x_utm48n DOUBLE," +
				"y_utm48n DOUBLE," +
				"description VARCHAR(255)," +
				"actType VARCHAR(45)," +
				"capacity DOUBLE," +
				"day VARCHAR(45)," +
				"startTime DOUBLE," +
				"endTime DOUBLE" +				
				")",tableName));
		
		ActivityFacilitiesImpl facs = scenario.getActivityFacilities();
		for(ActivityFacility fac:facs.getFacilities().values()){
			ActivityFacilityImpl fi = (ActivityFacilityImpl) fac;
			String id = fi.getId().toString();
			double x =fi.getCoord().getX();
			double y = fi.getCoord().getY();
			String description = fi.getDesc();
			for(ActivityOption ao:fi.getActivityOptions().values()){
				ActivityOptionImpl aoi = (ActivityOptionImpl) ao;
				String actType =aoi.getType();
				double capacity = aoi.getCapacity();
				for(DayType dayType : aoi.getOpeningTimes().keySet()){
					String day = dayType.toString();
					aoi.getOpeningTimes();
					for(OpeningTime ot: aoi.getOpeningTimes(dayType)){
						double startTime = ot.getStartTime();
						double endTime = ot.getStartTime();
						String sqlInserter = "INSERT INTO %s " +
								"VALUES(\'%s\',%f,%f,\'%s\',\'%s\',%f,\'%s\',%f,%f);";
						dba.executeUpdate(String.format(sqlInserter,tableName,id,x,y,description,actType,capacity,day,startTime,endTime));
					}
				}
			}
		}
	}
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 * @deprecated
	 * 
	 * Oh no, cannot set the activity type for activities at a facility anymore
	 */
	
	public void mapActFromSQLtoXML(ResultSet rs) throws SQLException{
		rs.beforeFirst();
		while(rs.next()){
			String id = rs.getString("id");
			String actType = rs.getString("classification");
			ActivityFacility facility = this.scenario.getActivityFacilities().
					getFacilities().get(new IdImpl(id));
			Iterator<ActivityOption> ao =facility.getActivityOptions().values().iterator();
			while(ao.hasNext()){
				ActivityOptionImpl i = (ActivityOptionImpl) ao.next();

//				i.setType(actType);
			}
			
		}
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("data/matsim2.properties"));
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader fcr = new MatsimFacilitiesReader(scenario);
		fcr.readFile(args[0]);
		FacilitiesToSQL f2sql = new FacilitiesToSQL(dba, scenario);
//		f2sql.createCompleteFacilityAndActivityTable("edu_facilities_fullXML");
		f2sql.createShortSQLFacilityList("full_facility_list");
		
//		ResultSet rs = dba.executeQuery("select distinct id, classification from edu_facilities_xml_summary");
//		f2sql.mapActFromSQLtoXML(rs);
//		FacilitiesWriter fcw =  new FacilitiesWriter(f2sql.scenario.getActivityFacilities());
//		String completeFacilitiesXMLFile = args[1];
//		fcw.write(completeFacilitiesXMLFile);
	}

}
