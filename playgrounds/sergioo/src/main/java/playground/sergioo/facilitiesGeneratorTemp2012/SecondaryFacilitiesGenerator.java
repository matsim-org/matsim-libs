package playground.sergioo.facilitiesGeneratorTemp2012;

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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import others.sergioo.util.algebra.MatrixND;
import others.sergioo.util.algebra.MatrixNDImpl;
import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import others.sergioo.util.fitting.FittingControl1D;
import others.sergioo.util.fitting.FittingData;
import others.sergioo.util.fitting.ProportionFittingControl1D;
import others.sergioo.util.fitting.TotalFittingControl1D;

public class SecondaryFacilitiesGenerator {

	//Enumerations
	private enum URA_PLACE_TYPES {
		EXEC_CONDOS("exec_condos",20),
		FACTORY("factory",1000),
		FACTORY_VACANT_AREA("factory_vacant_area",Double.POSITIVE_INFINITY),
		LANDED_PROPETY("landed_property",10),
		OFFICE_FLOOR_AREA("office_floor_area",500),
		OFFICE_VACANT_FLOOR_AREA("office_vacant_floor_area",Double.POSITIVE_INFINITY),
		PRIVATE_APARTMENTS_CONDOS("private_apartments_condos",10),
		SHOP_FLOOR_AREA("shop_floor_area",5),
		SHOP_VACANT_FLOOR_AREA("shop_vacant_floor_area",Double.POSITIVE_INFINITY),
		WAREHOUSE_FLOOR_AREA("warehouse_floor_area",Double.POSITIVE_INFINITY),
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
	
	//Constants
	private static final int NUM_FITTING_ITERATIONS = 100;
	private static final double MAX_NUM_POSITIONS_ONE_PLACE = 1000;
	private static final String SECONDARY_FACILITIES_FILE = "./data/currentSimulation/facilities/secondaryFacilities.xml";
	
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
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("Secondary facilities Singapore");
		BufferedReader reader = new BufferedReader(new FileReader("./data/facilities/postalCodes.txt"));
		SortedMap<Integer, Integer> postalCodes = new TreeMap<Integer, Integer>();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",,,");
			postalCodes.put(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
			line = reader.readLine();
		}
		reader.close();
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliarSec.properties"));
		Map<String,ActitvityTime> times = new HashMap<String,ActitvityTime>();
		ResultSet resultTypes = dataBaseAuxiliar.executeQuery("SELECT id,type FROM Activity_types");
		while(resultTypes.next()) {
			times.put(resultTypes.getString(2), new ActitvityTime());
			ResultSet resultTimes = dataBaseAuxiliar.executeQuery("SELECT type,time FROM Activity_times WHERE activity_type_id="+resultTypes.getInt(1));
			while(resultTimes.next()) {
				Map<Integer,Integer> map = null;
				if(resultTimes.getString(1).equals("start")) {
					map = times.get(resultTypes.getString(2)).startTimes;
					times.get(resultTypes.getString(2)).totalStarts ++; 
				}
				else {
					map = times.get(resultTypes.getString(2)).endTimes;
					times.get(resultTypes.getString(2)).totalEnds ++;
				}
				Integer freq = map.get(resultTimes.getInt(2));
				if(freq==null)
					map.put(resultTimes.getInt(2), 1);
				else
					map.put(resultTimes.getInt(2), freq+1);
			}
		}
		resultTypes.close();
		Map<String,double[]> fractions = new HashMap<String, double[]>();
		ResultSet resultFractions = dataBaseAuxiliar.executeQuery("SELECT name, ura_place_type_id, fraction FROM RealEstate_place_types, RealEstate_place_types_X_URA_place_types  WHERE id=realestate_place_type_id");
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
		for(int postalSector = 0; postalSector<100 && postalCodeEI.hasNext(); postalSector++) {
			System.out.println(postalSector);
			Map<URA_PLACE_TYPES, Double> areas = getPostalSectorAreas(postalSector);
			SortedMap<Integer, Tuple<String, Coord>> facilitiesPostalSector = new TreeMap<Integer, Tuple<String, Coord>>();
			while(postalCodeE.getKey()/10000<(postalSector+1)) {
				ResultSet resultFacility = dataBaseBuildings.executeQuery("SELECT type,longitude,latitude FROM building_directory WHERE id_building_directory="+postalCodeE.getValue());
				if(resultFacility.next()) {
					ResultSet resultType = dataBaseAuxiliar.executeQuery("SELECT * FROM RealEstate_place_types WHERE name='"+resultFacility.getString(1)+"'");
					if(resultType.next())
						facilitiesPostalSector.put(postalCodeE.getKey(), new Tuple<String, Coord>(resultFacility.getString(1).toLowerCase(), new Coord(resultFacility.getDouble(2), resultFacility.getDouble(3))));
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
			FittingControl1D[] fittingControls = new FittingControl1D[dimensions.length];
			MatrixND<Double> controlConstants1=new MatrixNDImpl<Double>(new int[]{numURAPlaceTypes});
			for(int i=0; i<controlConstants1.getDimension(0); i++)
				controlConstants1.setElement(new int[]{i}, URA_PLACE_TYPES.values()[i].getNumPositions(areas.get(URA_PLACE_TYPES.values()[i])));
			fittingControls[0]=new TotalFittingControl1D(controlConstants1);
			MatrixND<Double> controlConstants2=new MatrixNDImpl<Double>(new int[]{facilitiesPostalSector.size(), numURAPlaceTypes});
			Iterator<Tuple<String, Coord>> facilitiesI = facilitiesPostalSector.values().iterator();
			for(int i=0; i<controlConstants2.getDimension(0); i++) {
				String facilityType = facilitiesI.next().getFirst();
				Set<Integer> zeroPositions = new HashSet<Integer>();
				for(int j=0; j<controlConstants2.getDimension(1); j++)
					if(controlConstants1.getElement(new int[]{j})==0 && fractions.get(facilityType)[j]>0)
						zeroPositions.add(j);
				double sum=0;
				for(int j=0; j<controlConstants2.getDimension(1); j++)		
					if(!zeroPositions.contains(j))
						sum += fractions.get(facilityType)[j];
				for(int j=0; j<controlConstants2.getDimension(1); j++)
					if(!zeroPositions.contains(j))
						controlConstants2.setElement(new int[]{i,j}, sum==0?0:fractions.get(facilityType)[j]/sum);
					else
						controlConstants2.setElement(new int[]{i,j}, 0.0);
			}
			fittingControls[1]=new ProportionFittingControl1D(controlConstants2);
			FittingData fittingData = new FittingData(dimensions, fittingControls);
			MatrixND<Double> result=fittingData.run(NUM_FITTING_ITERATIONS);
			CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
			Iterator<Entry<Integer, Tuple<String, Coord>>> facilityI = facilitiesPostalSector.entrySet().iterator();
			int f=0;
			while(facilityI.hasNext()) {
				Entry<Integer, Tuple<String, Coord>> facilityE = facilityI.next();
				ActivityFacilityImpl facility = facilities.createAndAddFacility(Id.create(facilityE.getKey(), ActivityFacility.class), coordinateTransformation.transform(facilityE.getValue().getSecond()));
				ResultSet resultOccupations = dataBaseAuxiliar.executeQuery("SELECT type,quantity FROM Activity_types,Activity_types_X_Place_types,Place_types_X_RealEstate_place_types,RealEstate_place_types WHERE Activity_types.id=activity_type_id AND Activity_types_X_Place_types.place_type_id=Place_types_X_RealEstate_place_types.place_type_id AND realestate_place_type_id=RealEstate_place_types.id AND name='"+facilityE.getValue().getFirst()+"'");
				Map<String,Integer> occupations = new HashMap<String, Integer>();
				int totalQuantity=0;
				while(resultOccupations.next())
					if(!resultOccupations.getString(1).equals("null") && resultOccupations.getInt(2)!=0) {
						if(occupations.get(resultOccupations.getString(1))==null)
							occupations.put(resultOccupations.getString(1), resultOccupations.getInt(2));
						else
							occupations.put(resultOccupations.getString(1), occupations.get(resultOccupations.getString(1))+resultOccupations.getInt(2));
						totalQuantity += resultOccupations.getInt(2);
						if(!facility.getActivityOptions().containsKey(resultOccupations.getString(1))) {
							facility.createAndAddActivityOption(resultOccupations.getString(1));
							double random = Math.random()*times.get(resultOccupations.getString(1)).totalStarts;
							double sum = 0;
							for(Entry<Integer, Integer> timeS:times.get(resultOccupations.getString(1)).startTimes.entrySet()) {
								sum += timeS.getValue();
								if(random<sum) {
									random = Math.random()*times.get(resultOccupations.getString(1)).totalEnds;
									sum = 0;
									for(Entry<Integer, Integer> timeE:times.get(resultOccupations.getString(1)).endTimes.entrySet()) {
										sum += timeE.getValue();
										if(random<sum)  {
											double startTime = (timeS.getKey()/1800)*1800;
											double endTime = (timeE.getKey()/1800)*1800;
											if(startTime>endTime) {
												double temp = endTime;
												endTime = startTime;
												startTime = temp;
											}
											else if(startTime==endTime)
												endTime+=28800;
											((ActivityOptionImpl)facility.getActivityOptions().get(resultOccupations.getString(1))).addOpeningTime(new OpeningTimeImpl(startTime, endTime));
											break;
										}
									}
									break;
								}
							}
							facility.getActivityOptions().get(resultOccupations.getString(1)).setCapacity(0.0);
						}
					}
				int total=0;
				for(int t=0; t<dimensions[1]; t++)
					total+=result.getElement(new int[]{f,t});
				if(total>0) {
					for(int o=0; o<total; o++) {
						double random = Math.random()*totalQuantity;
						double sum = 0;
						for(Entry<String,Integer> occupation:occupations.entrySet()) {
							sum += occupation.getValue();
							if(random<sum) {
								ActivityOption option = facility.getActivityOptions().get(occupation.getKey());
								option.setCapacity(option.getCapacity()+1);
								break;
							}
						}		
					}
					for(String occupation:occupations.keySet())
						if(facility.getActivityOptions().get(occupation).getCapacity()==0)
							facility.getActivityOptions().remove(occupation);
				}
				else
					facilities.getFacilities().remove(facility.getId());
				f++;
			}
		}
		dataBaseAuxiliar.close();
		dataBaseBuildings.close();
		Set<Id<ActivityFacility>> removeFacilities = new HashSet<Id<ActivityFacility>>();
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			Set<String> removeOptions = new HashSet<String>();
			for(Entry<String,ActivityOption> activityOption:facility.getActivityOptions().entrySet())
				if(activityOption.getValue().getCapacity()==0)
					removeOptions.add(activityOption.getKey());
			for(String key:removeOptions)
				facility.getActivityOptions().remove(key);
			if(facility.getActivityOptions().size()==0)
				removeFacilities.add(facility.getId());
		}
		for(Id<ActivityFacility> key:removeFacilities)
			facilities.getFacilities().remove(key);
		new FacilitiesWriter(facilities).write(SECONDARY_FACILITIES_FILE);
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
	private static void createFakeFacilities(int postalSector, SortedMap<Integer, Tuple<String, Coord>> facilitiesPostalSector, Map<URA_PLACE_TYPES, Double> areas, Map<String, double[]> fractions) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBasePostalCodes  = new DataBaseAdmin(new File("./data/facilities/DataBasePostalCodes.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		Set<Integer> usedCodes = new HashSet<Integer>();
		for(URA_PLACE_TYPES uRAPlaceType: URA_PLACE_TYPES.values()) {
			double numPositions = uRAPlaceType.getNumPositions(areas.get(uRAPlaceType));
			if(numPositions>0) {
				double sumFractions = 0;
				for(Tuple<String, Coord> facilityType:facilitiesPostalSector.values())
					sumFractions+=fractions.get(facilityType.getFirst())[uRAPlaceType.ordinal()];
				if(sumFractions==0) {
					for(int i=0; i<numPositions/MAX_NUM_POSITIONS_ONE_PLACE; i++) {
						ResultSet resultPostalCodes = dataBasePostalCodes.executeQuery("SELECT zip,lng,lat FROM postal_codes WHERE zip>="+postalSector*10000+" AND zip<"+(postalSector+1)*10000 + " ORDER BY RAND()");
						boolean fakeOneCreated = false;
						while(resultPostalCodes.next() && !fakeOneCreated)
							if(!dataBaseBuildings.executeQuery("SELECT * FROM building_directory WHERE post_code='"+resultPostalCodes.getInt(1)+"' OR post_code='0"+resultPostalCodes.getInt(1)+"'").next() && !usedCodes.contains(resultPostalCodes.getInt(1))) {
								usedCodes.add(resultPostalCodes.getInt(1));
								ResultSet resultRandomType = dataBaseAuxiliar.executeQuery("SELECT name FROM RealEstate_place_types,RealEstate_place_types_X_URA_place_types WHERE id=realestate_place_type_id AND ura_place_type_id="+(uRAPlaceType.ordinal()+1)+" ORDER BY RAND() LIMIT 1");
								resultRandomType.next();
								facilitiesPostalSector.put(resultPostalCodes.getInt(1), new Tuple<String,Coord>(resultRandomType.getString(1).toLowerCase(), new Coord(resultPostalCodes.getDouble(2), resultPostalCodes.getDouble(3))));
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
			postalCodes.put(resultPostalCodes.getInt(1), new Coord(resultPostalCodes.getDouble(2), resultPostalCodes.getDouble(3)));
		resultPostalCodes.close();
		dataBasePostalCodes.close();
		//Buildings postal codes
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		Map<Id<ActivityFacility>, Integer> buildingsPostalCodes = new HashMap<Id<ActivityFacility>, Integer>();
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
				Coord coord = new Coord(resultFacilities.getDouble(2), resultFacilities.getDouble(3));
				postalCode = postalCodes.keySet().iterator().next();
				for(Entry<Integer, Coord> postalCodeE:postalCodes.entrySet())
					if(CoordUtils.calcDistance(coord, postalCodeE.getValue())<CoordUtils.calcDistance(coord, postalCodes.get(postalCode)))
						postalCode = postalCodeE.getKey();
			}
			buildingsPostalCodes.put(Id.create(resultFacilities.getInt(1), ActivityFacility.class), postalCode);
			if(buildingsPostalCodes.size()%100==0)
				System.out.println(buildingsPostalCodes.size());
		}
		resultFacilities.close();
		dataBaseBuildings.close();
		PrintWriter printWriter = new PrintWriter("./data/facilities/postalCodes.txt");
		for(Entry<Id<ActivityFacility>, Integer> postalCodeE:buildingsPostalCodes.entrySet())
			printWriter.println(postalCodeE.getKey()+",,,"+postalCodeE.getValue());
		printWriter.close();
	}
	private static ActivityFacilities createEmptyFacilities() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("Singapore work capacities");
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Map<String, Id<ActivityFacility>> postCodes = new HashMap<String, Id<ActivityFacility>>();
		Map<Coord, Id<ActivityFacility>> centers = new HashMap<Coord, Id<ActivityFacility>>();
		int numFacilities = 0;
		int numFacRepPosCode = 0;
		int numFacRepLocation = 0;
		DataBaseAdmin dataBaseBuildings  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		ResultSet resultFacilities = dataBaseBuildings.executeQuery("SELECT id_building_directory,longitude,latitude,type,post_code FROM building_directory");
		while(resultFacilities.next()) {
			Coord center = new Coord(resultFacilities.getDouble(2), resultFacilities.getDouble(3));
			ActivityFacilityImpl facility;
			if(!postCodes.containsKey(resultFacilities.getString(5))) {
				if(!centers.containsKey(center)) {
					facility = ((ActivityFacilitiesImpl) facilities).createAndAddFacility(Id.create(resultFacilities.getString(1), ActivityFacility.class), coordinateTransformation.transform(center));
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
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliarSec.properties"));
		ResultSet secondaries = dataBaseHits.executeQuery("SELECT DISTINCT t6_purpose FROM hits.hitsshort WHERE t6_purpose IN('shop','social','biz','eat','medi','errand','rec','relig')");
		int numActivityTypes = 0;
		while(secondaries.next()) {
			dataBaseAuxiliar.executeStatement("INSERT INTO Activity_types (type) VALUES ('"+secondaries.getString(1)+"')");
			numActivityTypes++;
			ResultSet startTimes = dataBaseHits.executeQuery("SELECT t4_endtime FROM hits.hitsshort WHERE t6_purpose='"+secondaries.getString(1)+"'");
			while(startTimes.next()) {
				int intTime = startTimes.getInt(1);
				double time = (intTime%100)*60+(intTime/100)*3600;
				dataBaseAuxiliar.executeStatement("INSERT INTO Activity_times VALUES ("+numActivityTypes+",'start',"+time+")");
			}
			startTimes.close();
			ResultSet endTimes = dataBaseHits.executeQuery("SELECT t3_starttime FROM hits.hitsshort WHERE t6_purpose='home'");
			while(endTimes.next()) {
				int intTime = endTimes.getInt(1);
				double time = (intTime%100)*60+(intTime/100)*3600;
				dataBaseAuxiliar.executeStatement("INSERT INTO Activity_times VALUES ("+numActivityTypes+",'end',"+time+")");
			}
			endTimes.close();
		}
		secondaries.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	private static void writePlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliarSec.properties"));
		ResultSet placeTypes = dataBaseHits.executeQuery("SELECT DISTINCT t5_placetype FROM hits.hitsshort WHERE t6_purpose IN('shop','social','biz','eat','medi','errand','rec','relig')");
		while(placeTypes.next()) {
			dataBaseAuxiliar.executeStatement("INSERT INTO Place_types (name) VALUES ('"+fixed(placeTypes.getString(1))+"')");
		}
		placeTypes.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	private static void crossActivityTypesPlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliarSec.properties"));
		ResultSet activitiesPlaces = dataBaseAuxiliar.executeQuery("SELECT Activity_types.type,Place_types.name,Activity_types.id,Place_types.id FROM Activity_types,Place_types");
		while(activitiesPlaces.next()) {
			ResultSet result = dataBaseHits.executeQuery("SELECT COUNT(*) FROM hits.hitsshort WHERE t6_purpose='"+activitiesPlaces.getString(1)+"' AND t5_placetype='"+fixed(activitiesPlaces.getString(2))+"'");
			if(result.next())
				dataBaseAuxiliar.executeStatement("INSERT INTO Activity_types_X_Place_types VALUES ("+activitiesPlaces.getInt(3)+","+activitiesPlaces.getInt(4)+","+result.getInt(1)+")");
			result.close();
		}
		activitiesPlaces.close();
		dataBaseAuxiliar.close();
		dataBaseHits.close();
	}
	private static void writeRealEstatePlaceTypes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliarSec.properties"));
		DataBaseAdmin dataBaseRealEstate  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealEstate.properties"));
		ResultSet places = dataBaseAuxiliar.executeQuery("SELECT id,name FROM Place_types");
		int numRealEstatePlaces = 0;
		while(places.next()) {
			ResultSet realEstatePlaces = dataBaseRealEstate.executeQuery("SELECT type FROM real_estate.BDType_X_HitsType WHERE hits_type='"+places.getString(2)+"'");
			while(realEstatePlaces.next()) {
				ResultSet realEstateId = dataBaseAuxiliar.executeQuery("SELECT id FROM RealEstate_place_types WHERE name='"+realEstatePlaces.getString(1)+"'");
				int idRealEstatePlace;
				if(realEstateId.next())
					idRealEstatePlace=realEstateId.getInt(1);
				else {
					dataBaseAuxiliar.executeStatement("INSERT INTO RealEstate_place_types (name) VALUES ('"+realEstatePlaces.getString(1)+"')");
					numRealEstatePlaces++;
					idRealEstatePlace = numRealEstatePlaces;
				}
				dataBaseAuxiliar.executeStatement("INSERT INTO Place_types_X_RealEstate_place_types VALUES ("+places.getInt(1)+","+idRealEstatePlace+")");
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
