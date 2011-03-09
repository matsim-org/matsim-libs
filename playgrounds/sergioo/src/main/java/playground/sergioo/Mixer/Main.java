package playground.sergioo.Mixer;

import java.io.IOException;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.sergioo.AddressLocator.BadAddressException;
import playground.sergioo.dataBase.NoConnectionException;



public class Main {
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws NoConnectionException 
	 * @throws BadAddressException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, NoConnectionException, BadAddressException, InterruptedException {
		//OSMParser.parsePublicTransitStops();
		Geocoder.completeBusStops2();
	}
}
