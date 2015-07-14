package playground.sergioo.ptVehiclesEditor2012;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;


public class VehiclesDataProcessor {

	//Attributes

	//Methods
	private void generateLinesXVehicles() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/ptSystems/DataBase.properties"));
		BufferedReader reader = new BufferedReader(new FileReader("./data/ptSystems/LinesXVehiclesSMRT.csv"));
		String line = reader.readLine();
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			ResultSet result = dataBaseVehicles.executeQuery("SELECT id,company_id FROM pt_systems.Lines WHERE name='"+parts[0]+"'");
			if(result.next() && result.getInt(2)==1) {
				int lineId = result.getInt(1);
				result = dataBaseVehicles.executeQuery("SELECT id FROM pt_systems.Vehicles WHERE description='"+parts[2]+"'");
				if(result.next()) {
					int vehicleId = result.getInt(1);
					result = dataBaseVehicles.executeQuery("SELECT units FROM pt_systems.Lines_Vehicles WHERE line_id="+lineId+" AND vehicle_id="+vehicleId);
					if(result.next()) {
						dataBaseVehicles.executeStatement("UPDATE pt_systems.Lines_Vehicles SET units=units+1 WHERE line_id="+lineId+" AND vehicle_id="+vehicleId);
					}
					else
						dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Lines_Vehicles VALUES ("+lineId+","+vehicleId+","+1+")");
				}
				else
					System.out.println("Vehicle not found: "+parts[2]);
			}
			else if(result.next())
				System.out.println("Line "+parts[0]+" appears in other company file.");
			else
				System.out.println("Line not found: "+parts[0]);
			line=reader.readLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader("./data/ptSystems/LinesXVehiclesSBS.csv"));
		line = reader.readLine();
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			ResultSet result = dataBaseVehicles.executeQuery("SELECT id,company_id FROM pt_systems.Lines WHERE name='"+parts[0]+"'");
			if(result.next() && result.getInt(2)==2) {
				int lineId = result.getInt(1);
				result = dataBaseVehicles.executeQuery("SELECT id FROM pt_systems.Vehicles WHERE description='"+parts[2]+"'");
				if(result.next()) {
					int vehicleId = result.getInt(1);
					result = dataBaseVehicles.executeQuery("SELECT units FROM pt_systems.Lines_Vehicles WHERE line_id="+lineId+" AND vehicle_id="+vehicleId);
					if(result.next()) {
						dataBaseVehicles.executeStatement("UPDATE pt_systems.Lines_Vehicles SET units=units+1 WHERE line_id="+lineId+" AND vehicle_id="+vehicleId);
					}
					else
						dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Lines_Vehicles VALUES ("+lineId+","+vehicleId+","+1+")");
				}
				else
					System.out.println("Vehicle not found: "+parts[2]);
			}
			else if(result.next())
				System.out.println("Line "+parts[0]+" appears in other company file.");
			else
				System.out.println("Line not found: "+parts[0]);
			line=reader.readLine();
		}
		reader.close();
		dataBaseVehicles.close();
	}
	private void defineLinesCompanies() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/ptSystems/DataBase.properties"));
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/ptSystems/SBSLines.txt")));
		Set<String> sBSLinesNames = new HashSet<String>();
		String line = reader.readLine();
		while(line!=null) {
			sBSLinesNames.add(line);
			line = reader.readLine();
		}
		reader.close();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("./data/currentSimulation/transitScheduleWVWAM.xml");
		for(TransitLine transitLine:((ScenarioImpl)scenario).getTransitSchedule().getTransitLines().values())
			if(transitLine.getRoutes().values().iterator().hasNext() && transitLine.getRoutes().values().iterator().next().getTransportMode().trim().equals("bus")) {
				String company = "1";
				if(sBSLinesNames.contains(transitLine.getId().toString()))
					company = "2";
				dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Lines (name,company_id) VALUES ('"+transitLine.getId().toString()+"',"+company+")");
			}
		dataBaseVehicles.close();
	}
	private void defineVehicles() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/ptSystems/DataBase.properties"));
		ResultSet resultComp = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Companies");
		int incCapacity = 1, incEngine = 1, incVehicle = 1;
		while(resultComp.next()) {
			int companyId = resultComp.getInt(1);
			String company = resultComp.getString(2);
			BufferedReader reader = new BufferedReader(new FileReader(new File("./data/ptSystems/"+company+".csv")));
			String line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				int vehicleId=0;
				ResultSet result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Vehicles WHERE description='"+parts[0]+"'");
				if(result.next())
					vehicleId=result.getInt(1);
				else {
					int capacityId = 0;
					result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Capacities WHERE seats="+parts[4]+" AND standing="+parts[5]);
					if(result.next())
						capacityId = result.getInt(1);
					else {
						dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Capacities (seats,standing,freight_capacity) VALUES("+parts[4]+","+parts[5]+",0)");
						capacityId = incCapacity;
						incCapacity++;
					}
					int engineId = 0;
					result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Engines WHERE fuel_type='"+parts[7]+"'");
					if(result.next())
						engineId = result.getInt(1);
					else {
						dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Engines (fuel_type,gas_consumption) VALUES('"+parts[7]+"',0)");
						engineId = incEngine;
						incEngine++;
					}
					dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Vehicles (description,width,length,maximum_speed,capacity_id,engine_id,door_mode) VALUES('"+parts[0]+"',"+parts[1]+","+parts[2]+","+parts[3]+","+capacityId+","+engineId+",'"+parts[9]+"')");
					vehicleId = incVehicle;
					incVehicle++;
				}
				dataBaseVehicles.executeStatement("INSERT INTO pt_systems.Companies_Vehicles VALUES ("+companyId+","+vehicleId+","+parts[10]+")");
				line = reader.readLine();
			}
			reader.close();
		}
		dataBaseVehicles.close();
	}
	private void relateVehiclesToTransitSchedule(String transitFile, String newVehiclesFile, double fraction) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseVehicles  = new DataBaseAdmin(new File("./data/ptSystems/DataBase.properties"));
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(transitFile);
		Vehicles vehicles = ((ScenarioImpl)scenario).getTransitVehicles();
		for(TransitLine transitLine:((ScenarioImpl)scenario).getTransitSchedule().getTransitLines().values()) {
			if(transitLine.getRoutes().values().iterator().hasNext()) {
				ResultSet result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Lines WHERE name='"+transitLine.getId().toString()+"'");
				if(result.next()) {
					int companyId = result.getInt(3);
					Map<Integer,Integer> vehicleIds = new HashMap<Integer,Integer>();
					result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Lines_Vehicles WHERE line_id ="+result.getInt(1));
					if(result.next())
						do
							vehicleIds.put(result.getInt(2),result.getInt(3));
						while(result.next());
					else {
						System.out.println("No vehicles for line: "+transitLine.getId().toString());
						result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Companies_Vehicles WHERE company_id ="+companyId);
						if(result.next())
							do {
								ResultSet result2 = dataBaseVehicles.executeQuery("SELECT type FROM pt_systems.Vehicles WHERE id="+result.getInt(2));
								if(result2.next() && result2.getString(1).equals("bus"))
									vehicleIds.put(result.getInt(2),result.getInt(3));
							} while(result.next());
						else {
							System.out.println("No information for line: "+transitLine.getId().toString());
							boolean correctSystem = false;
							while(!correctSystem) {
								result = dataBaseVehicles.executeQuery("SELECT id,type FROM pt_systems.Vehicles ORDER BY RAND() LIMIT 1");
								if(result.next())
									if(result.getString(2).equals("bus")) {
										correctSystem = true;
										vehicleIds.put(result.getInt(1), 1);
									}
							}
						}
					}
					int total = 0;
					for(Integer units:vehicleIds.values())
						total+=units;
					for(TransitRoute transitRoute:transitLine.getRoutes().values())
						for(Departure departure:transitRoute.getDepartures().values()) {
							if(vehicles.getVehicles().get(departure.getVehicleId())==null) {
								int random = (int)(Math.random()*total);
								int sum = 0;
								for(Entry<Integer, Integer> current:vehicleIds.entrySet()) {
									sum+=current.getValue();
									if(random<sum) {
										VehicleType vehicleType = vehicles.getVehicleTypes().get(Id.create(current.getKey(), VehicleType.class));
										if(vehicleType==null) {
											result = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Vehicles WHERE id="+current.getKey());
											if(result.next()) {
												vehicleType = new VehicleTypeImpl(Id.create(result.getInt(1), VehicleType.class));
												vehicleType.setDescription(result.getString(2));
												vehicleType.setWidth(result.getDouble(3));
												vehicleType.setLength(result.getDouble(4));
												vehicleType.setMaximumVelocity(result.getDouble(5)/3.6);
												ResultSet result2 = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Capacities WHERE id="+result.getInt(6));
												VehicleCapacity vehicleCapacity = new VehicleCapacityImpl();
												if(result2.next()) {
													vehicleCapacity.setSeats((int) (result2.getInt(2)*fraction));
													vehicleCapacity.setStandingRoom((int) (result2.getInt(3)*fraction));
												}
												vehicleType.setCapacity(vehicleCapacity);
												result2 = dataBaseVehicles.executeQuery("SELECT * FROM pt_systems.Engines WHERE id="+result.getInt(7));
												if(result2.next())
													vehicleType.setEngineInformation( VehicleUtils.getFactory().createEngineInformation(FuelType.valueOf(result2.getString(2)), result2.getDouble(3)));
												vehicleType.setDoorOperationMode(DoorOperationMode.valueOf(result.getString(8)));
												vehicleType.setAccessTime(result.getDouble(10)/fraction);
												vehicleType.setEgressTime(result.getDouble(11)/fraction);
												vehicles.addVehicleType( vehicleType);
											}
										}
										vehicles.addVehicle(new VehicleImpl(departure.getVehicleId(), vehicleType));		
										break;
									}
								}
							}
						}
				}
			}
		}
		new VehicleWriterV1(vehicles).writeFile(newVehiclesFile);
		dataBaseVehicles.close();
	}
	public static void main(String[] args) {
		try {
			VehiclesDataProcessor vehiclesDataProcessor =  new VehiclesDataProcessor();
			vehiclesDataProcessor.relateVehiclesToTransitSchedule("./data/currentSimulation/transitScheduleWVWAM.xml", "./data/currentSimulation/vehiclesGood25PercentSampleWAM.xml", 0.25);
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
	
}
