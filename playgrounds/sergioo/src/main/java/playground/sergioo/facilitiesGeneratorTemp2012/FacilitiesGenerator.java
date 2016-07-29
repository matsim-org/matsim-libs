package playground.sergioo.facilitiesGeneratorTemp2012;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class FacilitiesGenerator {
	
	private static final Logger log = Logger.getLogger(FacilitiesGenerator.class);
	
	public static void createBDTypeXActivityTypeTable() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBase.properties"));
		try {
			dataBaseBuildings.executeQuery("SELECT * FROM BDType_X_ActivityType");
			dataBaseBuildings.executeStatement("DROP TABLE BDType_X_ActivityType");
		} catch (SQLException e) {	
		}
		dataBaseBuildings.executeStatement("CREATE TABLE BDType_X_ActivityType AS (SELECT DISTINCT bh.type, ha.t6_purpose FROM (SELECT DISTINCT type, hits_type FROM BDType_X_HitsType) bh, (SELECT DISTINCT t5_placetype, t6_purpose FROM hits.hitsshort) ha WHERE bh.hits_type = ha.t5_placetype)");
		dataBaseBuildings.close();
	}
	
	/*public static void createBDTypeXHitsTypeTable() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		ResultSet result = dataBaseHits.executeQuery("SELECT type,hits_type FROM real_estate_xref");
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBase.properties"));
		try {
			dataBaseBuildings.executeQuery("SELECT * FROM BDType_X_HitsType");
			dataBaseBuildings.executeStatement("DROP TABLE BDType_X_HitsType");
		} catch (SQLException e) {	
		}
		dataBaseBuildings.executeStatement("CREATE TABLE BDType_X_HitsType AS(SELECT type,hits_type FROM hits.real_estate_xref)");
		dataBaseBuildings.executeStatement("DELETE FROM BDType_X_HitsType");
		while(result.next()) {
			String types = result.getString(2);
			if(result.getString(1)!=null && types!=null) {
				String[] parts = types.split(" ");
				for(String hitsType:parts)
					if(!hitsType.equals(""))
						dataBaseBuildings.executeStatement("INSERT INTO BDType_X_HitsType VALUES ('"+result.getString(1)+"','"+hitsType+"')");
			}
		}
		result.close();
		dataBaseHits.close();
		dataBaseBuildings.close();
	}
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		createBDTypeXHitsTypeTable();
	}*/
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		if(args.length==5)
			createBDTypeXActivityTypeTable();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[1]));
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
		matsimNetworkReader.readFile(args[2]);
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBase.properties"));
		Map<String, Id<ActivityFacility>> postCodes = new HashMap<String, Id<ActivityFacility>>();
		Map<Coord, Id<ActivityFacility>> centers = new HashMap<Coord, Id<ActivityFacility>>();
		ResultSet resultFacilities = dataBaseBuildings.executeQuery("SELECT id_building_directory,longitude,latitude,type,post_code FROM building_directory");
		int numFacilities = 0;
		int numFacRepPosCode = 0;
		int numFacRepLocation = 0;
		int numFacNoActivities = 0;
		while(resultFacilities.next()) {
			ActivityFacilityImpl facility;
			Coord center = new Coord(resultFacilities.getDouble(2), resultFacilities.getDouble(3));
			boolean newFacility = false;
			if(!postCodes.containsKey(resultFacilities.getString(5))) {
				if(!centers.containsKey(center)) {
					facility = facilities.createAndAddFacility(Id.create(resultFacilities.getString(1), ActivityFacility.class), coordinateTransformation.transform(center));
					newFacility = true;
				}
				else {
					numFacRepPosCode++;
					facility = (ActivityFacilityImpl) facilities.getFacilities().get(centers.get(center));
				}
			}
			else {
				numFacRepLocation++;
				facility = (ActivityFacilityImpl) facilities.getFacilities().get(postCodes.get(resultFacilities.getString(5)));
			}
			ResultSet resultBDTypeXActivityType = dataBaseBuildings.executeQuery("SELECT * FROM BDType_X_ActivityType WHERE type='"+resultFacilities.getString(4)+"'");
			while(resultBDTypeXActivityType.next()) {
				String activityType = resultBDTypeXActivityType.getString(2);
				if(((PlanCalcScoreConfigGroup)scenario.getConfig().getModule(PlanCalcScoreConfigGroup.GROUP_NAME)).getActivityTypes().contains(activityType))
					if(!facility.getActivityOptions().containsKey(activityType))
						facility.createAndAddActivityOption(activityType);
			}
			resultBDTypeXActivityType.close();
			if(facility.getActivityOptions().size()==0) {
				numFacNoActivities++;
				facilities.getFacilities().remove(Id.create(resultFacilities.getString(1), ActivityFacility.class));
			}
			else if(newFacility){
				postCodes.put(resultFacilities.getString(5), facility.getId());
				centers.put(center, facility.getId());
			}
			numFacilities++;
		}
		log.info(facilities.getFacilities().size()+" of "+numFacilities);
		log.info(numFacRepLocation+" facilities with same locations");
		log.info(numFacRepPosCode+" facilities with same postal code");
		log.info(numFacNoActivities+" facilities without activities");
		resultFacilities.close();
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(facilities, (Network) scenario.getNetwork());
		new FacilitiesWriter(facilities).write(args[3]);
	}
	
}
