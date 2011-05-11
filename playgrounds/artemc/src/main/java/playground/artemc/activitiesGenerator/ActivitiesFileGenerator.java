package playground.artemc.activitiesGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.artemc.dataBase.DataBaseAdmin;
import playground.artemc.dataBase.NoConnectionException;

public class ActivitiesFileGenerator extends MatsimXmlWriter{

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 */

	  
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/ArtemDataBase.properties"));
		
//		ResultSet rs = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time, Journey_Time_Full, Journey_End_Time, start_lat,start_lon, end_lat,end_lon,TripID FROM trips_geo_clean WHERE TripID<10");
		
//		ActivitiesFileGenerator format = new ActivitiesFileGenerator();
//		format.setPrettyPrint(false);
		
		String StartTime="";
		String Duration="";
		String EndTime="";
		Double StartLat=0.0;
		Double StartLon=0.0;
		Double EndLat=0.0;
		Double EndLon=0.0;
		Integer PlanID=0;
		Long CARD_ID=0L;
		String EndTimeLastLeg="";
		
		ActivitiesFileGenerator activitiesFileGenerator = new ActivitiesFileGenerator();
		activitiesFileGenerator.writeHeader();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N"); 
		ResultSet agents = dba.executeQuery("SELECT * FROM allagents WHERE PlanID<101");
		while(agents.next()){
			CARD_ID = agents.getLong(1);
			PlanID = agents.getInt(2);
			Boolean newPerson=true;
			
			ResultSet rs = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time, Journey_Time_Full, Journey_End_Time, start_lat,start_lon, end_lat,end_lon FROM trips_geo_clean WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Journey_Start_Time");
			activitiesFileGenerator.writeNewPerson(Integer.toString(PlanID));
			while(rs.next()) {
				CARD_ID = rs.getLong(1);
				StartTime = rs.getString(2);
				Duration = rs.getString(3);
				EndTime = rs.getString(4);
				StartLat = rs.getDouble(5);
				StartLon = rs.getDouble(6);
				EndLat = rs.getDouble(7);
				EndLon = rs.getDouble(8);
//				TripID = rs.getInt(9);			
			
				Coord coordStart =new CoordImpl(StartLon, StartLat);
				Coord coordEnd =new CoordImpl(EndLon, EndLat);
				Coord UTMStart = ct.transform(coordStart);
				Coord UTMEnd = ct.transform(coordEnd);
				StartLon=UTMStart.getX();
				StartLat=UTMStart.getY();
				EndLon=UTMEnd.getX();
				EndLat=UTMEnd.getY();		
				activitiesFileGenerator.writeTrip(Long.toString(CARD_ID),StartTime,Duration,EndTimeLastLeg,Double.toString(StartLat),Double.toString(StartLon),Double.toString(EndLat),Double.toString(EndLon),newPerson);
				newPerson=false;
				EndTimeLastLeg=EndTime;
			}
			
			activitiesFileGenerator.writeClosePerson(EndTime, Double.toString(EndLat),Double.toString(EndLon));
		}
	    dba.close();
	    activitiesFileGenerator.writeEnd();
	    System.out.println("Done!");
	    
	}


	public void writeNewPerson(String PlanID) throws IOException{
		List<Tuple<String, String>> listEmpty = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listPerson = new ArrayList<Tuple<String,String>>();
		listPerson.add(new Tuple<String, String>("id", PlanID));
		this.writer.write(NL);
		this.writeStartTag("person", listPerson);
		this.writeStartTag("plan", listEmpty);
	}
	
	public void writeClosePerson(String EndTime, String EndLat,String EndLon) throws IOException  {

		List<Tuple<String, String>> listAct = new ArrayList<Tuple<String,String>>();
		listAct.add(new Tuple<String, String>("type", "unknown"));
		listAct.add(new Tuple<String, String>("x", EndLon));
		listAct.add(new Tuple<String, String>("y", EndLat));
		listAct.add(new Tuple<String, String>("start_time", EndTime));
		listAct.add(new Tuple<String, String>("end_time", "23:59:59"));
		this.writeStartTag("act", listAct, true);
		this.writeEndTag("plan");
		this.writeEndTag("person");
	}
	
	public void writeTrip(String CARD_ID,String StartTime,String Duration,String EndTimeLastLeg,String StartLat,String StartLon, String EndLat,String EndLon,Boolean newPerson) throws IOException {

		List<Tuple<String, String>> listAct = new ArrayList<Tuple<String,String>>();	
		List<Tuple<String, String>> listLeg = new ArrayList<Tuple<String,String>>();
		
		listAct.add(new Tuple<String, String>("type", "unknown"));
		listAct.add(new Tuple<String, String>("x", StartLon));
		listAct.add(new Tuple<String, String>("y", StartLat));		
		if (newPerson==false){
			listAct.add(new Tuple<String, String>("start_time", EndTimeLastLeg));
		}
		listAct.add(new Tuple<String, String>("end_time", StartTime));
		
		
		listLeg.add(new Tuple<String, String>("mode", "pt"));
		
		this.writeStartTag("act", listAct, true);
		this.writeStartTag("leg", listLeg, true);		
	
	}		
	
 public void writeHeader() throws IOException {
	 	List<Tuple<String, String>> listEmpty = new ArrayList<Tuple<String,String>>();
	 	this.openFile("./data/plans.xml");
		this.writeXmlHead();
		this.writeDoctype("plans", "http://www.matsim.org/files/dtd/plans_v4.dtd");
		this.writeStartTag("plans", listEmpty);
		
 }	
 
 public void writeEnd() throws IOException{
	 this.writeEndTag("plans");
		this.close();
 }
}
