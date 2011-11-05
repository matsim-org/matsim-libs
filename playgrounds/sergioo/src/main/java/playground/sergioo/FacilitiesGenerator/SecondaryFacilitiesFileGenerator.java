package playground.sergioo.FacilitiesGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.sergioo.dataBase.DataBaseAdmin;
import playground.sergioo.dataBase.NoConnectionException;
import util.fitting.FittingControl;
import util.fitting.FittingData;
import util.fitting.MatrixNDimensions;
import util.fitting.MatrixNDimensionsImpl;
import util.fitting.ProportionFittingControl;
import util.fitting.TotalFittingControl;

public class SecondaryFacilitiesFileGenerator {

	//Enumeration
	private enum URA_PLACE_TYPES {
		EXEC_CONDOS("exec_condos",Double.POSITIVE_INFINITY),
		FACTORY("factory",100),
		FACTORY_VACANT_AREA("factory_vacant_area",Double.POSITIVE_INFINITY),
		LANDED_PROPETY("landed_property",Double.POSITIVE_INFINITY),
		OFFICE_FLOOR_AREA("office_floor_area",30),
		OFFICE_VACANT_FLOOR_AREA("office_vacant_floor_area",Double.POSITIVE_INFINITY),
		PRIVATE_APARTMENTS_CONDOS("private_apartments_condos",Double.POSITIVE_INFINITY),
		SHOP_FLOOR_AREA("shop_floor_area",80),
		SHOP_VACANT_FLOOR_AREA("shop_vacant_floor_area",Double.POSITIVE_INFINITY),
		WAREHOUSE_FLOOR_AREA("warehouse_floor_area",200),
		WAREHOUSE_VACANT_AREA("warehouse_vacant_area",Double.POSITIVE_INFINITY);
		private String name;
		private double areaFactor;
		private URA_PLACE_TYPES(String name, double areaFactor) {
			this.name = name;
			this.areaFactor = areaFactor;
		}
		public double getNumPositions(double area) {
			if(Double.isInfinite(areaFactor))
				return 0;
			else
				return area/areaFactor;
		}
	}

	private static final int NUM_FITTING_ITERATIONS = 50;
	private static final double MAX_NUM_POSITIONS_ONE_PLACE = 1000;
	
	//Methods

	public static void main(String[] args) {
		try {
			//writeActivityTypes();
			//writePlaceTypes();
			//crossActivityTypesPlaceTypes();
			//writeRealEstatePlaceTypes();
			//savePostalCodes();
			assignActivityOptions();
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
	private static void assignActivityOptions() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("Secondary facilities Singapore");
		BufferedReader reader = new BufferedReader(new FileReader("./data/facilities/postalCodes.txt"));
		SortedMap<Integer, Integer> postalCodes = new TreeMap<Integer, Integer>();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",,,");
			postalCodes.put(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
			line = reader.readLine();
		}
		reader.close();
		Map<String,double[]> fractions = new HashMap<String, double[]>();
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet resultFractions = dataBaseAuxiliar.executeQuery("SELECT name, ura_place_type_id, fraction FROM Sec_RealEstate_place_types, Sec_RealEstate_place_types_X_URA_place_types  WHERE id=realestate_place_type_id");
		while(resultFractions.next()) {
			double[] fracs = fractions.get(resultFractions.getString(1).toLowerCase());
			if(fracs==null)
				fracs = new double[URA_PLACE_TYPES.values().length];
			fracs[resultFractions.getInt(2)-1] = resultFractions.getDouble(3);
			fractions.put(resultFractions.getString(1).toLowerCase(), fracs);
		}
		for(Entry<String,double[]> fracs:fractions.entrySet()) {
			double sum=0;
			for(double frac:fracs.getValue())
				sum+=frac;
			for(int i=0; i<fracs.getValue().length; i++)	
				fracs.getValue()[i] = fracs.getValue()[i]/sum;
		}
		Iterator<Entry<Integer, Integer>> postalCodeEI = postalCodes.entrySet().iterator();
		Entry<Integer, Integer> postalCodeE = postalCodeEI.next();
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		for(int postalSector = 0; postalSector<100; postalSector++) {
			System.out.println(postalSector);
			Map<URA_PLACE_TYPES, Double> areas = getPostalSectorAreas(postalSector);
			Map<Id, String> facilitiesPostalSector = new HashMap<Id, String>();
			while(postalCodeE.getKey()/10000<postalSector) {
				ResultSet resultFacility = dataBaseBuildings.executeQuery("SELECT type FROM building_directory WHERE id_building_directory="+postalCodeE.getValue());
				if(resultFacility.next()) {
					ResultSet resultType = dataBaseAuxiliar.executeQuery("SELECT * FROM Sec_RealEstate_place_types WHERE name='"+resultFacility.getString(1)+"'");
					if(resultType.next())
						facilitiesPostalSector.put(new IdImpl(postalCodeE.getValue()), resultFacility.getString(1).toLowerCase());
					resultType.close();
				}
				resultFacility.close();
				if(postalCodeEI.hasNext())
					postalCodeE = postalCodeEI.next();
				else
					break;
			}
			createFakeFacilities(postalSector, facilitiesPostalSector, areas, fractions);
			int numURAPlaceTypes = URA_PLACE_TYPES.values().length;
			int[] dimensions = new int[] {facilitiesPostalSector.size(), numURAPlaceTypes};
			FittingControl[] fittingControls = new FittingControl[dimensions.length];
			MatrixNDimensions<Double> controlConstants1=new MatrixNDimensionsImpl<Double>(new int[]{numURAPlaceTypes});
			for(int i=0; i<controlConstants1.getDimensions()[0]; i++)
				controlConstants1.setElement(new int[]{i}, URA_PLACE_TYPES.values()[i].getNumPositions(areas.get(URA_PLACE_TYPES.values()[i])));
			fittingControls[0]=new TotalFittingControl(controlConstants1);
			/*for(int i=0; i<dimensions[1]; i++) 
				System.out.print(controlConstants1.getElement(new int[]{i})+" ");
			System.out.println();
			System.out.println();*/
			MatrixNDimensions<Double> controlConstants2=new MatrixNDimensionsImpl<Double>(new int[]{facilitiesPostalSector.size(), numURAPlaceTypes});
			Iterator<String> facilitiesI = facilitiesPostalSector.values().iterator();
			for(int i=0; i<controlConstants2.getDimensions()[0]; i++) {
				String facilityType = facilitiesI.next();
				Set<Integer> zeroPositions = new HashSet<Integer>();
				for(int j=0; j<controlConstants2.getDimensions()[1]; j++)
					if(controlConstants1.getElement(new int[]{j})==0 && fractions.get(facilityType)[j]>0)
						zeroPositions.add(j);
				double sum=0;
				for(int j=0; j<controlConstants2.getDimensions()[1]; j++)		
					if(!zeroPositions.contains(j))
						sum += fractions.get(facilityType)[j];
				for(int j=0; j<controlConstants2.getDimensions()[1]; j++)
					if(!zeroPositions.contains(j))
						controlConstants2.setElement(new int[]{i,j}, sum==0?0:fractions.get(facilityType)[j]/sum);
					else
						controlConstants2.setElement(new int[]{i,j}, 0.0);
			}
			fittingControls[1]=new ProportionFittingControl(controlConstants2);
			/*for(int i=0; i<dimensions[0]; i++) { 
				for(int j=0; j<dimensions[1]; j++)
					System.out.print(controlConstants2.getElement(new int[]{i,j})+" ");
				System.out.println();
			}
			System.out.println();
			System.out.println();*/
			FittingData fittingData = new FittingData(dimensions, fittingControls);
			MatrixNDimensions<Double> result=fittingData.run(NUM_FITTING_ITERATIONS);
			for(int i=0; i<dimensions[0]; i++) { 
				for(int j=0; j<dimensions[1]; j++)
					System.out.print(result.getElement(new int[]{i,j})+" ");
				System.out.println();
			}
			Iterator<Entry<Id, String>> facilityI = facilitiesPostalSector.entrySet().iterator();
			/*while(facilitiesI.hasNext()) {
				Entry<Id, String> facility = facilityI.next();
				
				facility = facilities.createFacility(new IdImpl(facility.getKey()), coordinateTransformation.transform(center));
			}*/
		}
		dataBaseAuxiliar.close();
		dataBaseBuildings.close();
	}
	private static Map<URA_PLACE_TYPES, Double> getPostalSectorAreas(int postalSector) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAreas  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealis.properties"));
		Map<URA_PLACE_TYPES, Double> areas = new HashMap<URA_PLACE_TYPES, Double>();
		ResultSet area = dataBaseAreas.executeQuery("SELECT executive_condominium FROM exec_condos_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.EXEC_CONDOS, (double) area.getInt(1));
		else
			areas.put(URA_PLACE_TYPES.EXEC_CONDOS, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT multiple_user_factory, single_user_factory, business_park FROM factory_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.FACTORY, area.getDouble(1)+area.getDouble(2)+area.getDouble(3));
		else
			areas.put(URA_PLACE_TYPES.FACTORY, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT multiple_user_factory, single_user_factory, business_park FROM factory_vacant_area_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.FACTORY_VACANT_AREA, area.getDouble(1)+area.getDouble(2)+area.getDouble(3));
		else
			areas.put(URA_PLACE_TYPES.FACTORY_VACANT_AREA, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT detached_house, semi_detached_house, terrace_house FROM landed_property_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.LANDED_PROPETY,area.getDouble(1)+area.getDouble(2)+area.getDouble(3));
		else
			areas.put(URA_PLACE_TYPES.LANDED_PROPETY, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT private, public FROM office_floor_area_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.OFFICE_FLOOR_AREA, area.getDouble(1)+area.getDouble(2));
		else
			areas.put(URA_PLACE_TYPES.OFFICE_FLOOR_AREA, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT private, public FROM office_vacant_floor_area_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.OFFICE_VACANT_FLOOR_AREA, area.getDouble(1)+area.getDouble(2));
		else
			areas.put(URA_PLACE_TYPES.OFFICE_VACANT_FLOOR_AREA, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT apartment, condominium FROM private_apartments_condos_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.PRIVATE_APARTMENTS_CONDOS, area.getDouble(1)+area.getDouble(2));
		else
			areas.put(URA_PLACE_TYPES.PRIVATE_APARTMENTS_CONDOS, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT private, public FROM shop_floor_area_postal_sector WHERE subzone="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.SHOP_FLOOR_AREA, area.getDouble(1)+area.getDouble(2));
		else
			areas.put(URA_PLACE_TYPES.SHOP_FLOOR_AREA, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT private, public FROM shop_vacant_floor_area_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.SHOP_VACANT_FLOOR_AREA, area.getDouble(1)+area.getDouble(2));
		else
			areas.put(URA_PLACE_TYPES.SHOP_VACANT_FLOOR_AREA, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT warehouse FROM warehouse_floor_area_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.WAREHOUSE_FLOOR_AREA, area.getDouble(1));
		else
			areas.put(URA_PLACE_TYPES.WAREHOUSE_FLOOR_AREA, 0.0);
		area.close();
		area = dataBaseAreas.executeQuery("SELECT warehouse FROM warehouse_vacant_area_postal_sector WHERE postal_sector="+postalSector);
		if(area.next())
			areas.put(URA_PLACE_TYPES.WAREHOUSE_VACANT_AREA, area.getDouble(1));
		else
			areas.put(URA_PLACE_TYPES.WAREHOUSE_VACANT_AREA, 0.0);
		area.close();
		dataBaseAreas.close();
		return areas;
	}
	private static void createFakeFacilities(int postalSector, Map<Id, String> facilitiesPostalSector, Map<URA_PLACE_TYPES, Double> areas, Map<String, double[]> fractions) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBasePostalCodes  = new DataBaseAdmin(new File("./data/facilities/DataBasePostalCodes.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		Set<Integer> usedCodes = new HashSet<Integer>();
		for(URA_PLACE_TYPES uRAPlaceType: URA_PLACE_TYPES.values()) {
			double numPositions = uRAPlaceType.getNumPositions(areas.get(uRAPlaceType));
			if(numPositions>0) {
				double sumFractions = 0;
				for(String facilityType:facilitiesPostalSector.values())
					sumFractions+=fractions.get(facilityType)[uRAPlaceType.ordinal()];
				if(sumFractions==0) {
					for(int i=0; i<numPositions/MAX_NUM_POSITIONS_ONE_PLACE; i++) {
						ResultSet resultPostalCodes = dataBasePostalCodes.executeQuery("SELECT zip FROM postal_codes WHERE zip>="+postalSector*10000+" AND zip<"+(postalSector+1)*10000 + " ORDER BY RAND()");
						boolean fakeOneCreated = false;
						while(resultPostalCodes.next() && !fakeOneCreated)
							if(!dataBaseBuildings.executeQuery("SELECT * FROM building_directory WHERE post_code='"+resultPostalCodes.getInt(1)+"' or post_code='0"+resultPostalCodes.getInt(1)+"'").next() && !usedCodes.contains(resultPostalCodes.getInt(1))) {
								usedCodes.add(resultPostalCodes.getInt(1));
								ResultSet resultRandomType = dataBaseAuxiliar.executeQuery("SELECT name FROM RealEstate_place_types,RealEstate_place_types_X_URA_place_types WHERE id=realestate_place_type_id AND ura_place_type_id="+(uRAPlaceType.ordinal()+1)+" ORDER BY RAND() LIMIT 1");
								resultRandomType.next();
								facilitiesPostalSector.put(new IdImpl("new"+facilitiesPostalSector.size()), resultRandomType.getString(1).toLowerCase());
								fakeOneCreated=true;
							}
						if(fakeOneCreated==false)
							System.out.println("No positions for fake facility: "+postalSector+", "+uRAPlaceType.name);
					}
				}
			}
		}
		dataBaseAuxiliar.close();
		dataBasePostalCodes.close();
	}
	private static void savePostalCodes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		//Postal Codes
		DataBaseAdmin dataBasePostalCodes  = new DataBaseAdmin(new File("./data/facilities/DataBasePostalCodes.properties"));
		ResultSet resultPostalCodes = dataBasePostalCodes.executeQuery("SELECT zip,lng,lat FROM postal_codes");
		Map<Integer,Coord> postalCodes = new HashMap<Integer, Coord>();
		while(resultPostalCodes.next())
			postalCodes.put(resultPostalCodes.getInt(1), new CoordImpl(resultPostalCodes.getDouble(2), resultPostalCodes.getDouble(3)));
		resultPostalCodes.close();
		dataBasePostalCodes.close();
		//Buildings postal codes
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		Map<Id,Integer> buildingsPostalCodes = new HashMap<Id, Integer>();
		ResultSet resultFacilities = dataBaseBuildings.executeQuery("SELECT id_building_directory,longitude,latitude,post_code FROM building_directory");
		while(resultFacilities.next()) {
			boolean badPostalCode = false;
			int postalCode = -1;
			if(resultFacilities.getString(4)==null)
				badPostalCode = true;
			else
				try {
					postalCode = Integer.parseInt(resultFacilities.getString(4));
				} catch (Exception e) {
					badPostalCode = true;
				}
			if(badPostalCode) {
				Coord coord = new CoordImpl(resultFacilities.getDouble(2), resultFacilities.getDouble(3));
				postalCode = postalCodes.keySet().iterator().next();
				for(Entry<Integer, Coord> postalCodeE:postalCodes.entrySet())
					if(CoordUtils.calcDistance(coord, postalCodeE.getValue())<CoordUtils.calcDistance(coord, postalCodes.get(postalCode)))
						postalCode = postalCodeE.getKey();
			}
			buildingsPostalCodes.put(new IdImpl(resultFacilities.getInt(1)), postalCode);
			if(buildingsPostalCodes.size()%100==0)
				System.out.println(buildingsPostalCodes.size());
		}
		resultFacilities.close();
		dataBaseBuildings.close();
		PrintWriter printWriter = new PrintWriter("./data/facilities/postalCodes.txt");
		for(Entry<Id, Integer> postalCodeE:buildingsPostalCodes.entrySet())
			printWriter.println(postalCodeE.getKey()+",,,"+postalCodeE.getValue());
		printWriter.close();
	}
	private static ActivityFacilities createEmptyFacilities() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		ActivityFacilities facilities = new ActivityFacilitiesImpl("Singapore work capacities");
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Map<String,Id> postCodes = new HashMap<String,Id>();
		Map<Coord,Id> centers = new HashMap<Coord,Id>();
		int numFacilities = 0;
		int numFacRepPosCode = 0;
		int numFacRepLocation = 0;
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		ResultSet resultFacilities = dataBaseBuildings.executeQuery("SELECT id_building_directory,longitude,latitude,type,post_code FROM building_directory");
		while(resultFacilities.next()) {
			Coord center = new CoordImpl(resultFacilities.getDouble(2), resultFacilities.getDouble(3));
			ActivityFacilityImpl facility;
			if(!postCodes.containsKey(resultFacilities.getString(5))) {
				if(!centers.containsKey(center)) {
					facility = ((ActivityFacilitiesImpl) facilities).createFacility(new IdImpl(resultFacilities.getString(1)), coordinateTransformation.transform(center));
					postCodes.put(resultFacilities.getString(5),facility.getId());
					centers.put(center,facility.getId());
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
			numFacilities++;
		}
		resultFacilities.close();
		dataBaseBuildings.close();
		return facilities;
	}

	private static void writeActivityTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet secondaries = dataBaseHits.executeQuery("SELECT DISTINCT t6_purpose FROM hits.hitsshort WHERE t6_purpose!='work' AND t6_purpose!='home' AND t6_purpose!='pickupdropof'"/*TODO*/);
		int numActivityTypes = 0;
		while(secondaries.next()) {
			dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_Activity_types (type) VALUES ('"+secondaries.getString(1)+"')");
			numActivityTypes++;
			ResultSet startTimes = dataBaseHits.executeQuery("SELECT t4_endtime FROM hits.hitsshort WHERE t6_purpose='"+secondaries.getString(1)+"'");
			while(startTimes.next()) {
				int intTime = startTimes.getInt(1);
				double time = (intTime%100)*60+(intTime/100)*3600;
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_Activity_times VALUES ("+numActivityTypes+",'start',"+time+")");
			}
			startTimes.close();
			ResultSet endTimes = dataBaseHits.executeQuery("SELECT t3_starttime FROM hits.hitsshort WHERE t6_purpose='home'");
			while(endTimes.next()) {
				int intTime = endTimes.getInt(1);
				double time = (intTime%100)*60+(intTime/100)*3600;
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_Activity_times VALUES ("+numActivityTypes+",'end',"+time+")");
			}
			endTimes.close();
		}
		secondaries.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	
	private static void writePlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet placeTypes = dataBaseHits.executeQuery("SELECT DISTINCT t5_placetype FROM hits.hitsshort t6_purpose!='work' AND t6_purpose!='home' AND t6_purpose!='pickupdropof'"/*TODO*/);
		while(placeTypes.next()) {
			dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_Place_types (name) VALUES ('"+fixed(placeTypes.getString(1))+"')");
		}
		placeTypes.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	
	private static void crossActivityTypesPlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet activitiesPlaces = dataBaseAuxiliar.executeQuery("SELECT Sec_Activity_types.type,Sec_Place_types.name,Sec_Activity_types.id,Sec_Place_types.id FROM facilities_auxiliar.Sec_Activity_types,facilities_auxiliar.Sec_Place_types");
		while(activitiesPlaces.next()) {
			ResultSet result = dataBaseHits.executeQuery("SELECT COUNT(*) FROM hits.hitsshort WHERE t6_purpose='"+activitiesPlaces.getString(1)+"' AND t5_placetype='"+fixed(activitiesPlaces.getString(2))+"'");
			if(result.next())
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_Activity_types_X_Place_types VALUES ("+activitiesPlaces.getInt(3)+","+activitiesPlaces.getInt(4)+","+result.getInt(1)+")");
			result.close();
		}
		activitiesPlaces.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}

	private static void writeRealEstatePlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		DataBaseAdmin dataBaseRealEstate  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		ResultSet places = dataBaseAuxiliar.executeQuery("SELECT id,name FROM facilities_auxiliar.Sec_Place_types");
		int numRealEstatePlaces = 0;
		while(places.next()) {
			ResultSet realEstatePlaces = dataBaseRealEstate.executeQuery("SELECT type FROM real_estate.BDType_X_HitsType WHERE hits_type='"+places.getString(2)+"'");
			while(realEstatePlaces.next()) {
				ResultSet realEstateId = dataBaseAuxiliar.executeQuery("SELECT id FROM facilities_auxiliar.Sec_RealEstate_place_types WHERE name='"+realEstatePlaces.getString(1)+"'");
				int idRealEstatePlace;
				if(realEstateId.next())
					idRealEstatePlace=realEstateId.getInt(1);
				else {
					dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_RealEstate_place_types (name) VALUES ('"+realEstatePlaces.getString(1)+"')");
					numRealEstatePlaces++;
					idRealEstatePlace = numRealEstatePlaces;
				}
				dataBaseAuxiliar.executeStatement("INSERT INTO facilities_auxiliar.Sec_Place_types_X_RealEstate_place_types VALUES ("+places.getInt(1)+","+idRealEstatePlace+")");
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
