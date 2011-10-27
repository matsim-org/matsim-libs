package playground.sergioo.PTVehiclesEditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

import playground.sergioo.dataBase.DataBaseAdmin;
import playground.sergioo.dataBase.NoConnectionException;

public class VehiclesDataProcessor {

	//Attributes

	//Methods
	
	private void defineLinesCompanies() {
		try {
			DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/busSystems/DataBase.properties"));
			BufferedReader reader = new BufferedReader(new FileReader(new File("./data/busSystems/SBSLines.txt")));
			Set<String> sBSLinesNames = new HashSet<String>();
			String line = reader.readLine();
			while(line!=null) {
				sBSLinesNames.add(line);
				line = reader.readLine();
			}
			reader.close();
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			scenario.getConfig().scenario().setUseTransit(true);
			new TransitScheduleReader(scenario).readFile("./data/currentSimulation/transitScheduleWVWAM.xml");
			for(TransitLine transitLine:((ScenarioImpl)scenario).getTransitSchedule().getTransitLines().values())
				if(transitLine.getRoutes().values().iterator().hasNext() && transitLine.getRoutes().values().iterator().next().getTransportMode().trim().equals("bus")) {
					String company = "1";
					if(sBSLinesNames.contains(transitLine.getId().toString()))
						company = "2";
					dataBaseVehicles.executeStatement("INSERT INTO bus_systems.Lines (name,company_id) VALUES ('"+transitLine.getId().toString()+"',"+company+")");
				}
			dataBaseVehicles.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoConnectionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	private void defineVehicles() {
		try {
			DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/busSystems/DataBase.properties"));
			ResultSet resultComp = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Companies");
			int incCapacity = 1, incEngine = 1, incVehicle = 1;
			while(resultComp.next()) {
				int companyId = resultComp.getInt(1);
				String company = resultComp.getString(2);
				BufferedReader reader = new BufferedReader(new FileReader(new File("./data/busSystems/"+company+".csv")));
				String line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(",");
					int vehicleId=0;
					ResultSet result = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Vehicles WHERE description='"+parts[0]+"'");
					if(result.next())
						vehicleId=result.getInt(1);
					else {
						int capacityId = 0;
						result = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Capacities WHERE seats="+parts[4]+" AND standing="+parts[5]);
						if(result.next())
							capacityId = result.getInt(1);
						else {
							dataBaseVehicles.executeStatement("INSERT INTO bus_systems.Capacities (seats,standing,freight_capacity) VALUES("+parts[4]+","+parts[5]+",0)");
							capacityId = incCapacity;
							incCapacity++;
						}
						int engineId = 0;
						result = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Engines WHERE fuel_type='"+parts[7]+"'");
						if(result.next())
							engineId = result.getInt(1);
						else {
							dataBaseVehicles.executeStatement("INSERT INTO bus_systems.Engines (fuel_type,gas_consumption) VALUES('"+parts[7]+"',0)");
							engineId = incEngine;
							incEngine++;
						}
						dataBaseVehicles.executeStatement("INSERT INTO bus_systems.Vehicles (description,width,length,maximum_speed,capacity_id,engine_id,door_mode) VALUES('"+parts[0]+"',"+parts[1]+","+parts[2]+","+parts[3]+","+capacityId+","+engineId+",'"+parts[9]+"')");
						vehicleId = incVehicle;
						incVehicle++;
					}
					dataBaseVehicles.executeStatement("INSERT INTO bus_systems.Companies_Vehicles VALUES ("+companyId+","+vehicleId+","+parts[10]+")");
					line = reader.readLine();
				}
			}
			dataBaseVehicles.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoConnectionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	private void relateVehiclesToTransitSchedule(String transitFile, String newVehiclesFile) {
		try {
			DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/busSystems/DataBase.properties"));
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			scenario.getConfig().scenario().setUseTransit(true);
			scenario.getConfig().scenario().setUseVehicles(true);
			new TransitScheduleReader(scenario).readFile(transitFile);
			Vehicles vehicles = ((ScenarioImpl)scenario).getVehicles();
			for(TransitLine transitLine:((ScenarioImpl)scenario).getTransitSchedule().getTransitLines().values()) {
				if(transitLine.getRoutes().values().iterator().hasNext() && transitLine.getRoutes().values().iterator().next().getTransportMode().trim().equals("bus")) {
					ResultSet result = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Lines WHERE name='"+transitLine.getId().toString()+"'");
					if(result.next()) {
						result = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Companies_Vehicles WHERE company_id ='"+result.getInt(3)+"'");
						Map<Integer,Integer> vehicleIds = new HashMap<Integer,Integer>();
						while(result.next())
							vehicleIds.put(result.getInt(2),result.getInt(3));
						int total = 0;
						for(Integer units:vehicleIds.values())
							total+=units;
						for(TransitRoute transitRoute:transitLine.getRoutes().values())
							for(Departure departure:transitRoute.getDepartures().values()) {
								int random = (int)(Math.random()*total);
								int sum = 0;
								for(Entry<Integer, Integer> current:vehicleIds.entrySet()) {
									sum+=current.getValue();
									if(random<sum) {
										VehicleType vehicleType = vehicles.getVehicleTypes().get(new IdImpl(current.getKey()));
										if(vehicleType==null) {
											result = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Vehicles WHERE id="+current.getKey());
											if(result.next()) {
												vehicleType = new VehicleTypeImpl(new IdImpl(result.getInt(1)));
												vehicleType.setDescription(result.getString(2));
												vehicleType.setWidth(result.getDouble(3));
												vehicleType.setLength(result.getDouble(4));
												vehicleType.setMaximumVelocity(result.getDouble(5));
												ResultSet result2 = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Capacities WHERE id="+result.getInt(6));
												VehicleCapacity vehicleCapacity = new VehicleCapacityImpl();
												if(result2.next()) {
													vehicleCapacity.setSeats(result2.getInt(2));
													vehicleCapacity.setStandingRoom(result2.getInt(3));
												}
												vehicleType.setCapacity(vehicleCapacity);
												result2 = dataBaseVehicles.executeQuery("SELECT * FROM bus_systems.Engines WHERE id="+result.getInt(7));
												if(result2.next())
													vehicleType.setEngineInformation( new EngineInformationImpl(FuelType.valueOf(result2.getString(2)), result2.getDouble(3)));
												vehicleType.setDoorOperationMode(DoorOperationMode.valueOf(result.getString(8)));
												vehicles.getVehicleTypes().put(vehicleType.getId(), vehicleType);
											}
										}
										vehicles.getVehicles().put(departure.getVehicleId(),new VehicleImpl(departure.getVehicleId(), vehicleType));
										break;
									}
								}
							}
					}
				}
			}
			new VehicleWriterV1(vehicles).writeFile(newVehiclesFile);
			dataBaseVehicles.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoConnectionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		VehiclesDataProcessor vehiclesDataProcessor =  new VehiclesDataProcessor();
		//vehiclesDataProcessor.defineLinesCompanies();
		//vehiclesDataProcessor.defineVehicles();
		vehiclesDataProcessor.relateVehiclesToTransitSchedule("./data/currentSimulation/transitScheduleWV.xml", "./data/currentSimulation/vehiclesGood25PercentSample.xml");
	}
	
}
