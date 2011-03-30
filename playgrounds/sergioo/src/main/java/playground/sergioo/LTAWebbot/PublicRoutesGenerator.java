/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.sergioo.LTAWebbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.sergioo.AddressLocator.AddressLocator;
import playground.sergioo.AddressLocator.BadAddressException;
import playground.sergioo.dataBase.DataBaseAdmin;
import playground.sergioo.dataBase.NoConnectionException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

/**
 * Hello world!
 */
public class PublicRoutesGenerator {
	
	public static void getBusRoutes() throws FailingHttpStatusCodeException, MalformedURLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IndexOutOfBoundsException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		final WebClient webClient = new WebClient();
		webClient.setCssEnabled(false);
		webClient.setJavaScriptEnabled(true);
		HtmlPage page = webClient.getPage("http://www.publictransport.sg/publish/ptp/en/getting_around/bus_service.html?");
		HtmlElement selector = page.getElementByName("qs_busservice");
		Iterable<HtmlElement> options = selector.getChildElements();
		Iterator<HtmlElement> i = options.iterator();
		i.next();
		while(i.hasNext()) {
			HtmlElement option = i.next();
			page = option.click();
			List<HtmlDivision> routeTableContainers = (List<HtmlDivision>) page.getByXPath("//div[@class='BusService']");
			char directionChar='>';
			for(HtmlDivision routeTableContainer:routeTableContainers) {
				HtmlTable routeTable = (HtmlTable) routeTableContainer.getHtmlElementDescendants().iterator().next();
				for(int s=3; s<routeTable.getRowCount(); s+=2) {
					String name = routeTable.getRow(s).getCell(2).getTextContent().replaceAll("'", "''");
					HtmlAnchor busStopInfo = (HtmlAnchor) routeTable.getRow(s).getCell(1).getHtmlElementDescendants().iterator().next();
					String propTemp = busStopInfo.getAttribute("href");
					String[] coords = propTemp.substring(propTemp.indexOf('(')+1, propTemp.indexOf(')')).split(",");
					try {
						dba.executeStatement("INSERT INTO BusStops VALUES ('"+routeTable.getRow(s).getCell(1).getTextContent()+"','"+name+"',"+coords[0]+","+coords[1].trim()+")");
					} catch (SQLException e) {
						if(!e.getMessage().startsWith("Duplicate entry"))
							throw e;
					}
					try {
						dba.executeStatement("INSERT INTO BusRoutesItems VALUES ("+((s-3)/2)+",'"+routeTable.getRow(s).getCell(1).getTextContent()+"','"+option.asText()+"','"+directionChar+"')");
					} catch (SQLException e) {
						throw e;
					}
				}
				directionChar='<';
			}
			System.out.println("Ready route: "+option.asText());
		}
		webClient.closeAllWindows();
		dba.close();
	}
	public static void getMRTRoutes() throws FailingHttpStatusCodeException, MalformedURLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IndexOutOfBoundsException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		final WebClient webClient = new WebClient();
		webClient.setCssEnabled(false);
		webClient.setJavaScriptEnabled(true);
		HtmlPage page = webClient.getPage("http://www.publictransport.sg/publish/ptp/en.html");
		HtmlElement selector = page.getElementByName("qs_mrtlrt");
		Iterable<HtmlElement> options = selector.getChildElements();
		Iterator<HtmlElement> i = options.iterator();
		i.next();
		while(i.hasNext()) {
			HtmlElement option = i.next();
			page = option.click();
			HtmlTableRow rowInfo = ((HtmlTable)((List<HtmlDivision>) page.getByXPath("//div[@class='getard_article contentSub']")).get(0).getHtmlElementsByTagName("table").get(0)).getRow(0);
			String[] infoParts = rowInfo.getCell(1).getTextContent().replaceAll("'", "''").split("\\(");
			String[] infoParts2 = infoParts[0].split(" - ");
			HtmlAnchor stopInfo = (HtmlAnchor) rowInfo.getCell(0).getHtmlElementDescendants().iterator().next();
			String propTemp = stopInfo.getAttribute("href");
			String[] coords = propTemp.substring(propTemp.indexOf('(')+1, propTemp.indexOf(')')).split(",");
			if(infoParts2[1].trim().endsWith("MRT"))
				dba.executeStatement("INSERT INTO MRTStops (RouteCode,Name,Address,Latitude,Longitude) VALUES ('"+infoParts2[0]+"','"+infoParts2[1].substring(0,infoParts2[1].length()-5)+"','"+infoParts[1].substring(0,infoParts[1].length()-1)+"',"+coords[0]+","+coords[1].trim()+")");
			else
				dba.executeStatement("INSERT INTO LRTStops (RouteCode,Name,Address,Latitude,Longitude) VALUES ('"+infoParts2[0]+"','"+infoParts2[1].substring(0,infoParts2[1].length()-5)+"','"+infoParts[1].substring(0,infoParts[1].length()-1)+"',"+coords[0]+","+coords[1].trim()+")");
			System.out.println("Ready route: "+option.asText());
		}
		webClient.closeAllWindows();
		dba.close();
	}
	public static void mergeBusStops() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParserConfigurationException, SAXException, BadAddressException, InterruptedException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		ResultSet rsLTA = dba.executeQuery("SELECT * FROM BusStops");
		while(rsLTA.next()) {
			String code = "";
			try {
				code=Integer.parseInt(rsLTA.getString(1))+"";
			} catch (NumberFormatException e) {
				code=rsLTA.getString(1);
			}
			dba.executeStatement("INSERT INTO BusStopsMerge (Code,NameLTA,LatitudeLTA,LongitudeLTA) VALUES('"+code+"','"+rsLTA.getString(2).replaceAll("'","''")+"',"+rsLTA.getDouble(3)+","+rsLTA.getDouble(4)+")");
		}
		ResultSet rsOSM = dba.executeQuery("SELECT * FROM BusStops2");
		while(rsOSM.next())
			try {
				dba.executeStatement("INSERT INTO BusStopsMerge (Code,NameOSM,LatitudeOSM,LongitudeOSM) VALUES('"+rsOSM.getInt(1)+"','"+rsOSM.getString(2).replaceAll("'","''")+"',"+rsOSM.getDouble(3)+","+rsOSM.getDouble(4)+")");
			} catch (SQLException e){
				if(!e.getMessage().startsWith("Duplicate entry"))
					throw e;
				dba.executeUpdate("UPDATE BusStopsMerge SET NameOSM='"+rsOSM.getString(2).replaceAll("'","''")+"',LatitudeOSM="+rsOSM.getDouble(3)+",LongitudeOSM="+rsOSM.getDouble(4)+" WHERE Code='"+rsOSM.getInt(1)+"'");
			}
		File busStopsFile = new File("./data/Bus Stop Code and Description.csv");
		BufferedReader reader = new BufferedReader(new FileReader(busStopsFile));
		reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] busStopParts = line.split(",");
			String code = null;
			try {
				code=Integer.toString(Integer.parseInt(busStopParts[0]));
			} catch (NumberFormatException e) {
				code=busStopParts[0];
			}
			String nameAddress = busStopParts[1].replaceAll("'","''");
			ResultSet rs = dba.executeQuery("SELECT * FROM BusStopsMerge WHERE Code='"+code+"'");
			if(!rs.next()) {
				AddressLocator addressLocator = new AddressLocator(busStopParts[1]+", Singapore");
				Thread.sleep(400);
				dba.executeStatement("INSERT INTO BusStopsMerge (Code,AddressName,LatitudeTransit,LongitudeTransit) VALUES('"+code+"','"+nameAddress+"',"+addressLocator.getLocation().getLatitude()+","+addressLocator.getLocation().getLongitude()+")");
			}
			else
				dba.executeUpdate("UPDATE BusStopsMerge SET AddressName='"+nameAddress+"' WHERE Code='"+code+"'");
			line = reader.readLine();
		}
	}
		
	public static void main( String[] args ) throws FailingHttpStatusCodeException, MalformedURLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IndexOutOfBoundsException, NoConnectionException, ParserConfigurationException, SAXException, BadAddressException, InterruptedException {
		getMRTRoutes();
	}
}
