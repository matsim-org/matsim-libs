package playground.sergioo.FacilitiesGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import playground.sergioo.dataBase.DataBaseAdmin;
import playground.sergioo.dataBase.NoConnectionException;

public class FacilitiesFileGenerator {

	//Attributes

	//Methods

	public static void main(String[] args) {
		try {
			//writeActivityTypes();
			//writePlaceTypes();
			//crossActivityTypesPlaceTypes();
			writeRealEstatePlaceTypes();
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
		} catch (NoConnectionException e) {
			e.printStackTrace();
		}
	}

	private static void writeActivityTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet occupations = dataBaseHits.executeQuery("SELECT DISTINCT p6_occup FROM hits.hitsshort WHERE t6_purpose='work'");
		int numActivityTypes = 0;
		while(occupations.next()) {
			dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Activity_types (type,occupation) VALUES ('work','"+occupations.getString(1)+"')");
			numActivityTypes++;
			ResultSet startTimes = dataBaseHits.executeQuery("SELECT t4_endtime FROM hits.hitsshort WHERE t6_purpose='work' AND p6_occup='"+occupations.getString(1)+"'");
			while(startTimes.next()) {
				int intTime = startTimes.getInt(1);
				double time = (intTime%100)*60+(intTime/100)*3600;
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Activity_times VALUES ("+numActivityTypes+",'start',"+time+")");
			}
			startTimes.close();
			ResultSet endTimes = dataBaseHits.executeQuery("SELECT t3_starttime FROM hits.hitsshort WHERE t6_purpose='home' AND p6_occup='"+occupations.getString(1)+"'");
			while(endTimes.next()) {
				int intTime = endTimes.getInt(1);
				double time = (intTime%100)*60+(intTime/100)*3600;
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Activity_times VALUES ("+numActivityTypes+",'end',"+time+")");
			}
			endTimes.close();
		}
		occupations.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	
	private static void writePlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet placeTypes = dataBaseHits.executeQuery("SELECT DISTINCT t5_placetype FROM hits.hitsshort WHERE t6_purpose='work'");
		while(placeTypes.next()) {
			dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Place_types (name) VALUES ('"+fixed(placeTypes.getString(1))+"')");
		}
		placeTypes.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	
	private static void crossActivityTypesPlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet activitiesPlaces = dataBaseAuxiliar.executeQuery("SELECT Activity_types.occupation,Place_types.name,Activity_types.id,Place_types.id FROM facilities_auxiliar.Activity_types,facilities_auxiliar.Place_types");
		while(activitiesPlaces.next()) {
			ResultSet result = dataBaseHits.executeQuery("SELECT COUNT(*) FROM hits.hitsshort WHERE t6_purpose='work' AND p6_occup='"+activitiesPlaces.getString(1)+"' AND t5_placetype='"+fixed(activitiesPlaces.getString(2))+"'");
			if(result.next())
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Activity_types_X_Place_types VALUES ("+activitiesPlaces.getInt(3)+","+activitiesPlaces.getInt(4)+","+result.getInt(1)+")");
			result.close();
		}
		activitiesPlaces.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}

	private static void writeRealEstatePlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		DataBaseAdmin dataBaseRealEstate  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		ResultSet places = dataBaseAuxiliar.executeQuery("SELECT id,name FROM facilities_auxiliar.Place_types");
		int numRealEstatePlaces = 0;
		while(places.next()) {
			ResultSet realEstatePlaces = dataBaseRealEstate.executeQuery("SELECT type FROM real_estate.BDType_X_HitsType WHERE hits_type='"+places.getString(2)+"'");
			while(realEstatePlaces.next()) {
				ResultSet realEstateId = dataBaseAuxiliar.executeQuery("SELECT id FROM facilities_auxiliar.RealEstate_place_types WHERE name='"+realEstatePlaces.getString(1)+"'");
				int idRealEstatePlace;
				if(realEstateId.next())
					idRealEstatePlace=realEstateId.getInt(1);
				else {
					dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.RealEstate_place_types (name) VALUES ('"+realEstatePlaces.getString(1)+"')");
					numRealEstatePlaces++;
					idRealEstatePlace = numRealEstatePlaces;
				}
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Place_types_X_RealEstate_place_types VALUES ("+places.getInt(1)+","+idRealEstatePlace+")");
			}
		}
		places.close();
		dataBaseRealEstate.close();
		dataBaseAuxiliar.close();
	}
	
	private static String fixed(String varchar) {
		return varchar.replaceAll("'", "''");
	}
	
}
