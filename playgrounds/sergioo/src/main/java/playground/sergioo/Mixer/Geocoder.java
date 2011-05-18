package playground.sergioo.Mixer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.sergioo.AddressLocator.*;
import playground.sergioo.dataBase.*;


public class Geocoder {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, InterruptedException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		ResultSet rs = dba.executeQuery("SELECT * from PublicTransitStops");
		while(rs.next()) {
			try {
				AddressLocator ad1 = new AddressLocator(rs.getString(2)+", Singapore");
				dba.executeUpdate("UPDATE PublicTransitStops SET latitude="+ad1.getLocation().getLatitude()+", longitude="+ad1.getLocation().getLongitude()+" WHERE `Bus Stop Code`="+rs.getInt(1));
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (BadAddressException e) {
				e.printStackTrace();
			}
		}
		dba.close();
	}
	/**
	 * 
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws NoConnectionException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws BadAddressException
	 * @throws InterruptedException
	 */
	public static void completeBusStops() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParserConfigurationException, SAXException, BadAddressException, InterruptedException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		File busStopsFile = new File("./data/Bus Stop Code and Description.csv");
		BufferedReader reader = new BufferedReader(new FileReader(busStopsFile));
		reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] busStopParts = line.split(",");
			ResultSet rs = dba.executeQuery("SELECT * FROM BusStops2 WHERE Code="+busStopParts[0]);
			if(!rs.next()) {
				AddressLocator addressLocator = new AddressLocator(busStopParts[1]+", Singapore");
				String nameAddress = busStopParts[1].replaceAll("'","''");
				dba.executeStatement("INSERT INTO BusStops (Code,AddressName,OSMLatitude,OSMLongitude) VALUES ("+busStopParts[0]+",'"+nameAddress+"',"+addressLocator.getLocation().getLatitude()+","+addressLocator.getLocation().getLongitude()+")");
				Thread.sleep(400);
			}
			else
				try {
					dba.executeStatement("INSERT INTO BusStops (Code,Name,OSMLatitude,OSMLongitude) VALUES ("+rs.getInt(1)+",'"+rs.getString(2)+"',"+rs.getDouble(3)+","+rs.getDouble(4)+")");
				} catch (SQLException e) {
					System.out.println(e.getMessage());
				}
			line = reader.readLine();
		}
		reader.close();
		dba.close();
	}
	
	public static void completeBusStops2() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParserConfigurationException, SAXException, BadAddressException, InterruptedException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		ResultSet rs = dba.executeQuery("SELECT * FROM BusStops2");
		while(rs.next()) {
			ResultSet rs2 = dba.executeQuery("SELECT * FROM BusStops1 WHERE Code="+rs.getInt(1));
			if(rs2.next())
				try {
					dba.executeStatement("INSERT INTO BusStops (Code,Name,AddressName,OSMLatitude,OSMLongitude) VALUES ("+rs2.getInt(1)+",'"+rs2.getString(2)+"','"+rs2.getString(3)+"',"+rs2.getDouble(4)+","+rs2.getDouble(5)+")");
				} catch (SQLException e) {
					System.out.println(e.getMessage());
				}
			else
				try {
					dba.executeStatement("INSERT INTO BusStops (Code,Name,OSMLatitude,OSMLongitude) VALUES ("+rs.getInt(1)+",'"+rs.getString(2)+"',"+rs.getDouble(3)+","+rs.getDouble(4)+")");
				} catch (SQLException e) {
					System.out.println(e.getMessage());
				}
		}
		dba.close();
	}
}
