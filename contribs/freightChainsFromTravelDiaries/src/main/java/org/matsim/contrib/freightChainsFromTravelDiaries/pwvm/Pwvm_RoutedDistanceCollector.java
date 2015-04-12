/**
 * 
 */
package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.w3c.dom.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;

import org.w3c.dom.Document;


/**
 * Retrieves the routed distance via the Google Maps API
 * 
 * @author schn_se
 *
 */
public class Pwvm_RoutedDistanceCollector {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Connection con = Pwvm_DatabaseConnection.dbconnect();
		
		int counter = 0;

		try {

			Statement st = con.createStatement();

			// Turn use of the cursor on.
			st.setFetchSize(1000);

			String stmt = "SELECT id, " +
			"purpose, " +
			"ROUND(ST_Distance(source_geom, dest_geom)) as airlinedistance, " +
			"ST_AsText(ST_Transform(source_geom,4326)) AS from, " +
			"ST_AsText(ST_Transform(dest_geom,4326)) AS dest " +
			"FROM pwvm_matrix_durchlauf5 " +
			"WHERE routed_distance IS NULL " +
			"ORDER BY RANDOM() " +
	//		"LIMIT 3000";
			"LIMIT 1000";

			System.out.println(stmt);
			ResultSet rs = st.executeQuery(stmt);

			// jede zeile nach der Reihe laden
			while (rs.next()) {
				
				counter++;
				System.out.println(counter+":");
				
				String origin = formatFromWKT(rs.getString(4));
				String destination = formatFromWKT(rs.getString(5));

				int airline_distance = rs.getInt(3);
				int routed_distance = retrieveDistance(origin, destination);

				System.out.println("airline distance: "+ airline_distance);
				System.out.println("routed distance: "+ routed_distance);

				Statement st2 = con.createStatement();
				st2.executeUpdate("UPDATE pwvm_matrix_durchlauf5 SET routed_distance="+routed_distance+" WHERE id="+rs.getString(1));

				int wait  = 30;
				System.out.println("Waiting for "+wait+" seconds...");
				Thread.sleep(wait*1000);

			}

		} catch (Exception e){
			e.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * Changes a coordinate in the form
	 * "POINT(13.5134106412818 52.4832093707195)"
	 * into the representation used by google
	 * @param string
	 * @return
	 */
	private static String formatFromWKT(String wkt) {
		String s = wkt.substring(wkt.indexOf('(')+1, wkt.indexOf(' '));
		s = wkt.substring(wkt.indexOf(' ')+1, wkt.length()-1) + "%20" + s;
		return s;
	}

	private static int retrieveDistance(String origin, String destination) throws Exception {

		int distance = -1;

		// Send data
		URL url = new URL("http://maps.google.com/maps/api/directions/xml?origin="+origin+"&destination="+destination+"&sensor=false");
		System.out.println(url.toString());
		System.out.println("Browserlink: http://maps.google.de/maps?f=d&source=s_d&saddr="+origin+"&daddr="+destination);


		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true); 
		DocumentBuilder builder = domFactory.newDocumentBuilder();

		Document doc = builder.parse(url.toString());

		XPath xpath = XPathFactory.newInstance().newXPath();
		// XPath Query for showing all nodes value
		XPathExpression expr = xpath.compile("//DirectionsResponse/route/leg[1]/distance/value/text()");

		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
//		for (int i = 0; i < nodes.getLength(); i++) {
//			distance = Integer.parseInt(nodes.item(i).getNodeValue());
//		}
		distance = Integer.parseInt(nodes.item(0).getNodeValue());


		return distance;
	}

}
