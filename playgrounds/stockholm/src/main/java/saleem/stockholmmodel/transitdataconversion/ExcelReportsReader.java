package saleem.stockholmmodel.transitdataconversion;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.matsim.api.core.v01.Coord;

/**
 * A class to convert Excel files of transit trips, transit stops, 
 * vehicles and transit  schedule into a MatSim readable XML Format. 
 * A complex transit schedule data structure is constructed based on the excel files.
 * 
 * @author Mohammad Saleem
 *
 */
public class ExcelReportsReader {
	
	public void readVehicleTypes(TransitSchedule transit, String path){//Reads vehicle-types excel file and adds data about transit Stops to TransitSchedule object
		try {
			HSSFRow row;
			HSSFCell cell;
			int cols = 0; // No of columns
			int tmp = 0;
			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(path));
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			int rows; // No of rows
			rows = sheet.getPhysicalNumberOfRows();
			// This trick ensures that we get the data properly even if it doesn't start from first few rows
			for(int r = 1; r < rows; r++) {
				row = sheet.getRow(r);
				if(row != null) {
					VehicleType vehicletype = new VehicleType();
					vehicletype.setID(row.getCell(0).getStringCellValue());
					vehicletype.setDesciption(row.getCell(1).getStringCellValue());
					vehicletype.setNumberOfSeats((int)row.getCell(2).getNumericCellValue());
					vehicletype.setStandingCapacity((int)row.getCell(3).getNumericCellValue());
					vehicletype.setLength(Double.parseDouble(row.getCell(4).getStringCellValue()));
					vehicletype.setWidth(Double.parseDouble(row.getCell(5).getStringCellValue()));
					transit.addVehicleType(vehicletype);
				}
			}
			}catch(Exception ioe) {
				ioe.printStackTrace();
			}
	}
	public void readExcelReportStopStallen(TransitSchedule transit, String path){//Reads Stoppstallen.xls and adds data about transit Stops to TransitSchedule object
		ArrayList<String> ids = new ArrayList<String>();
		try {
			HSSFRow row;
			HSSFCell cell;
			int cols = 0; // No of columns
			int tmp = 0;
			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(path));
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			int rows; // No of rows
			rows = sheet.getPhysicalNumberOfRows();
			// This trick ensures that we get the data properly even if it doesn't start from first few rows
			for(int i = 0; i < 10 || i < rows; i++) {
				row = sheet.getRow(i);
				if(row != null) {
					cols = sheet.getRow(i).getPhysicalNumberOfCells();
					if(tmp > cols) cols = tmp;
				}
			}
			for(int r = 9; r < rows; r++) {
				row = sheet.getRow(r);
				if(row != null) {
					Coord coord = new Coord(Double.parseDouble(row.getCell(8).getStringCellValue()), Double.parseDouble(row.getCell(10).getStringCellValue()));
					//coord=DataConversion.deg2UTM(coord);
					String name = row.getCell(4).getStringCellValue();
					String id = Integer.toString((int)row.getCell(0).getNumericCellValue());
					Stop stop = new Stop(name, coord, id);
					stop.setTransportMode(row.getCell(13).getStringCellValue());
					if(!ids.contains(id)){
						transit.addStop(stop);
					}
					ids.add(id);
				}
			}
			}catch(Exception ioe) {
				ioe.printStackTrace();
			}
	}
	public void readExcelReports(TransitSchedule transit, String path){//Reads Tidtabell - Stoppstallenummer.xls files 
																		//and adds data about lines, routes, route profiles and departures to transit schedule object
		try {
			HSSFRow row;
			HSSFCell cell;
			File[] files = new File(path).listFiles();
			for(int f=0; f<files.length ; f++){
				
				POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(files[f]));
				HSSFWorkbook wb = new HSSFWorkbook(fs);
				HSSFSheet sheet = wb.getSheetAt(0);
				if(files[f].getName().startsWith("Tidtabell - Stoppställenummer")){
					Line line = new Line();
					TransitRoute troute = new TransitRoute();
					line.setLineId("line" + files[f].getName().substring(29, files[f].getName().length()-4));//Id=line+line number
					Line line_r = new Line();
					line_r.setLineId("line" + files[f].getName().substring(29, files[f].getName().length()-4)+"_r");//Id=line+line number
					int rows; // No of rows
					rows = sheet.getPhysicalNumberOfRows();
					int cols = 0; // No of columns
					int tmp = 0;
				//This trick ensures that we get the data properly even if it doesn't start from first few rows
					for(int i = 0; i < 10 || i < rows; i++) {
					row = sheet.getRow(i);
						if(row != null) {
							cols = sheet.getRow(i).getPhysicalNumberOfCells();
								if(tmp > cols) cols = tmp;
						}
					}
					String tag = "";
					for(int r = 9; r < rows; r++) {
						//System.out.println();
						row = sheet.getRow(r);
						if(row != null) {
							Stop stop = transit.getStop(Integer.toString((int)row.getCell(11).getNumericCellValue()));
							//stop.setDepartureTime(row.getCell(18).getStringCellValue());
							Link link = new Link(stop.getId());//Temporarily assigning Stop ID to Line ID
							if(tag.equals("") || row.getCell(7).getStringCellValue().equals(tag)){
								tag = row.getCell(7).getStringCellValue();
								if(r != rows-1){
									String departureOffset = stop.calculateDepartureOffset(row.getCell(18).getStringCellValue(), sheet.getRow(r+1).getCell(18).getStringCellValue());
									stop.setDepartureOffset(departureOffset);
								}
								stop.setDepartureTime(row.getCell(18).getStringCellValue());
								troute.addStop(stop);//get the stop with the particular id and add it to route profile
								troute.addLink(link);
								troute.setID(troute.getFirstStop().getId() + "to" + troute.getLastStop().getId());
								if(r == rows-1){//for last row, add the transit route into the line
									if(row.getCell(6).getStringCellValue().equals("1")){
										int index = line.indexOfTransitRoute(troute);
										if(index == -1){//If the line does not already has this route
											Departure departure = new Departure();
											departure.setId("Dep"+troute.getDepartures().size()+1);
											departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
											Vehicle vehicle = new Vehicle();
											vehicle.setID("Veh"+transit.getVehicles().size()+1);
											vehicle.setType(troute.getFirstStop().getTransportMode());//Assuming transport mode can be BUS, TRAIN, TRAM, PENDELTAG
											departure.setVehicle(vehicle);
											departure.setVehicleRefId(vehicle.getID());
											transit.addVehicle(vehicle);
											troute.addDeparture(departure);
											line.addRouteID(new String(troute.getID()));
											troute.setID(troute.getID()+line.countRouteIds(troute.getID()));
											line.addTransitRoute(troute);
										}
										else{
											TransitRoute route = line.getRouteAtIndex(index);
											line.removeRouteAtIndex(index);
											Departure departure = new Departure();
											departure.setId("Dep"+route.getDepartures().size()+1);
											departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
											Vehicle vehicle = new Vehicle();
											vehicle.setID("Veh"+transit.getVehicles().size()+1);
											vehicle.setType(troute.getFirstStop().getTransportMode());
											departure.setVehicle(vehicle);
											departure.setVehicleRefId(vehicle.getID());
											transit.addVehicle(vehicle);
											route.addDeparture(departure);
											line.addTransitRoute(index, route);
										}
									}
									else{
										int index = line_r.indexOfTransitRoute(troute);
										if(index == -1){//If line_r does not already has this route
											Departure departure = new Departure();
											departure.setId("Dep"+troute.getDepartures().size()+1);
											departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
											Vehicle vehicle = new Vehicle();
											vehicle.setID("Veh"+transit.getVehicles().size()+1);
											vehicle.setType(troute.getFirstStop().getTransportMode());
											departure.setVehicle(vehicle);
											departure.setVehicleRefId(vehicle.getID());
											transit.addVehicle(vehicle);
											troute.addDeparture(departure);
											line_r.addRouteID(new String(troute.getID()));
											troute.setID(troute.getID()+line_r.countRouteIds(troute.getID()));
											line_r.addTransitRoute(troute);
										}
										else{
											TransitRoute route = line_r.getRouteAtIndex(index);
											line_r.removeRouteAtIndex(index);
											Departure departure = new Departure();
											departure.setId("Dep"+route.getDepartures().size()+1);
											departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
											Vehicle vehicle = new Vehicle();
											vehicle.setID("Veh"+transit.getVehicles().size()+1);
											vehicle.setType(troute.getFirstStop().getTransportMode());
											departure.setVehicle(vehicle);
											departure.setVehicleRefId(vehicle.getID());
											transit.addVehicle(vehicle);
											route.addDeparture(departure);
											line_r.addTransitRoute(index, route);
										}
									}
								}
							}
							else if (!row.getCell(7).getStringCellValue().equals(tag)){
								//When tag turner is different, it is a different route or the same route at a different time
								troute.setID(troute.getFirstStop().getId() + "to" + troute.getLastStop().getId());
								// Set the departure offset of last stop to "00:00:00"
								Stop st = troute.getLastStop();
								troute.removeStop(st);
								st.setDepartureOffset("00:00:00");
								troute.addStop(st);
								troute.setTransportMode(st.getTransportMode());
								if(sheet.getRow(r-1).getCell(6).getStringCellValue().equals("1")){//check direction for the row before the row whose tag is different
									int index = line.indexOfTransitRoute(troute);
									if(index == -1){//If the line does not already has this route
										Departure departure = new Departure();
										departure.setId("Dep"+troute.getDepartures().size()+1);
										departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
										Vehicle vehicle = new Vehicle();
										vehicle.setID("Veh"+transit.getVehicles().size()+1);
										vehicle.setType(troute.getFirstStop().getTransportMode());
										departure.setVehicle(vehicle);
										departure.setVehicleRefId(vehicle.getID());
										transit.addVehicle(vehicle);
										troute.addDeparture(departure);
										line.addRouteID(new String(troute.getID()));
										troute.setID(troute.getID()+line.countRouteIds(troute.getID()));
										line.addTransitRoute(troute);
									}
									else{
										//get the specific route, remove it from line, add a departure to it and add it again in line
										TransitRoute route = line.getRouteAtIndex(index);
										line.removeRouteAtIndex(index);
										Departure departure = new Departure();
										departure.setId("Dep"+route.getDepartures().size()+1);
										departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
										Vehicle vehicle = new Vehicle();
										vehicle.setID("Veh"+transit.getVehicles().size()+1);
										vehicle.setType(troute.getFirstStop().getTransportMode());
										departure.setVehicle(vehicle);
										departure.setVehicleRefId(vehicle.getID());
										transit.addVehicle(vehicle);
										route.addDeparture(departure);
										line.addTransitRoute(index, route);
									}
								}
								else{
									int index = line_r.indexOfTransitRoute(troute);
									if(index == -1){//If line_r does not already has this route
										Departure departure = new Departure();
										departure.setId("Dep"+troute.getDepartures().size()+1);
										departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
										Vehicle vehicle = new Vehicle();
										vehicle.setID("Veh"+transit.getVehicles().size()+1);
										vehicle.setType(troute.getFirstStop().getTransportMode());
										departure.setVehicle(vehicle);
										departure.setVehicleRefId(vehicle.getID());
										transit.addVehicle(vehicle);
										troute.addDeparture(departure);
										line_r.addRouteID(new String(troute.getID()));
										troute.setID(troute.getID()+line_r.countRouteIds(troute.getID()));
										line_r.addTransitRoute(troute);
									}
									else{
										TransitRoute route = line_r.getRouteAtIndex(index);
										line_r.removeRouteAtIndex(index);
										Departure departure = new Departure();
										departure.setId("Dep"+route.getDepartures().size()+1);
										departure.setDepartureTime(troute.getFirstStop().getDepartureTime());
										Vehicle vehicle = new Vehicle();
										vehicle.setID("Veh"+transit.getVehicles().size()+1);
										vehicle.setType(troute.getFirstStop().getTransportMode());
										departure.setVehicle(vehicle);
										departure.setVehicleRefId(vehicle.getID());
										transit.addVehicle(vehicle);
										route.addDeparture(departure);
										line_r.addTransitRoute(index, route);
									}
								}
								troute = new TransitRoute();
								stop.setDepartureTime(row.getCell(18).getStringCellValue());
								troute.addStop(stop);//get the stop with the particular id and add it to route profile
								troute.addLink(link);
								tag = row.getCell(7).getStringCellValue();
								
							}
							//System.out.println(row.getCell(3).getStringCellValue());// Direction
							//System.out.println(row.getCell(7).getStringCellValue());//Tag Turnr
							//System.out.println(row.getCell(10).getNumericCellValue());//Sekvencr
							//System.out.println(row.getCell(11).getNumericCellValue());//Stopstallnumer
							//System.out.println(row.getCell(13).getStringCellValue());//Name
							//System.out.println(row.getCell(15).getStringCellValue());//Senaste ankomsttid
							//System.out.println(row.getCell(18).getStringCellValue());//Tidigaste Avgangstid
							//System.out.println(row.getCell(22).getStringCellValue());//Avstigningsrestriktion
							//System.out.println(row.getCell(23).getStringCellValue());//P�stigningsrestriktion
							//for(int c = 0; c < cols; c++) {
								//cell = row.getCell((short)c);
								//if(cell != null) {
									// Type 3 is missing below, it is for blank cells so not required
									//if (cell.getCellType() == 0)System.out.print(cell.getNumericCellValue() + "\t");//If the cell is a numeric type
									//else if (cell.getCellType() == 1)System.out.print(cell.getStringCellValue() + "\t");// If the cell is a string type
									//else if (cell.getCellType() == 2)System.out.print(cell.getCellFormula() + "\t");// If the cell is a formula
									//else if (cell.getCellType() == 4)System.out.print(cell.getBooleanCellValue() + "\t");// If the cell is a boolean type
									//else if (cell.getCellType() == 5)System.out.print(cell.getErrorCellValue() + "\t");// If the cell is an error
							//		}
						//		}
							}
						}
					if(line.getTransitRoutes().size()!=0)
						transit.addLine(line);
					if(line_r.getTransitRoutes().size()!=0)
						transit.addLine(line_r);
					}
				}
			}catch(Exception ioe) {
				ioe.printStackTrace();
			}
	}
}
