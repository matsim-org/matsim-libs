package playground.sergioo.OSMBusMRTReader;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import playground.sergioo.dataBase.*;


public class OSMParser {
	
	//Methods
	/**
	 * Parses the public transit stops from a OSM xml file
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoConnectionException
	 */
	public static void parsePublicTransitStops() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, SAXException, ParserConfigurationException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/DataBase.properties"));
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    int replays = 0;
	    int stops = 0;
	    Document docWest = dBuilder.parse(new File("./data/westSingapore.osm"));
	    NodeList allNodes = docWest.getElementsByTagName("node");
	    for(int n=0; n<allNodes.getLength(); n++) {
	    	Node node = allNodes.item(n);
	    	NodeList nodeTags = ((Element)node).getElementsByTagName("tag");
	    	String latitude =node.getAttributes().item(2).getNodeValue();
    		String longitude =node.getAttributes().item(3).getNodeValue();
    		boolean isBusStop = false;
    		boolean isMRT = false;
	    	String codeB = "";
	    	String codeM = "";
	    	String name = "";
    		String[] routes = null;
	    	for(int t=0; t<nodeTags.getLength(); t++) {
	    		if(nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue().equals("bus_stop"))
	    			isBusStop = true;
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("railway") && nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue().equals("station"))
	    			isMRT = true;
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("name"))
    				name = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue();
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("asset_ref"))
    				codeB = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue();
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("route_ref"))
    				routes = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue().split(";");
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("ref"))
    				codeM = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue();
	    	}
	    	if(isBusStop && !name.equals("") && !codeB.equals("")) {
		    	try {
		    		dba.executeStatement("INSERT INTO BusStops VALUES ('"+codeB+"','"+name+"',"+latitude+","+longitude+")");
		    		System.out.println("Bus "+stops);
		    		stops++;
				} catch (SQLException e) {
					System.out.println(e.getMessage() + " WEST");
					replays++;
				}
				if(routes!=null)
					try {
						for(int r=0; r<routes.length; r++)
							dba.executeStatement("INSERT INTO BusStopsRoutes VALUES ('"+codeB+"','"+routes[r]+"')");
					} catch (SQLException e) {
						System.out.println(e.getMessage() + " WEST");
					}
	    	}
			else if(isMRT && !name.equals(""))
		    	try {
		    		if(codeM.equals(""))
						dba.executeStatement("INSERT INTO MRTStops (Name,OSMLatitude,OSMLongitude) VALUES ('"+name+"',"+latitude+","+longitude+")");
		    		else
		    			dba.executeStatement("INSERT INTO MRTStops (RouteCode,Name,OSMLatitude,OSMLongitude) VALUES ('"+codeM+"','"+name+"',"+latitude+","+longitude+")");
		    		System.out.println("MRT "+stops);
		    		stops++;
				} catch (SQLException e) {
					System.out.println(e.getMessage() + " WEST");
					replays++;
				}
	    }
	    Document docEast = dBuilder.parse(new File("./data/eastSingapore.osm"));
	    allNodes = docEast.getElementsByTagName("node");
	    for(int n=0; n<allNodes.getLength(); n++) {
	    	Node node = allNodes.item(n);
	    	NodeList nodeTags = ((Element)node).getElementsByTagName("tag");
	    	String latitude =node.getAttributes().item(2).getNodeValue();
    		String longitude =node.getAttributes().item(3).getNodeValue();
    		boolean isBusStop = false;
    		boolean isMRT = false;
	    	String codeB = "";
	    	String codeM = "";
	    	String name = "";
    		String[] routes = null;
	    	for(int t=0; t<nodeTags.getLength(); t++) {
	    		if(nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue().equals("bus_stop"))
	    			isBusStop = true;
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("railway") && nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue().equals("station"))
	    			isMRT = true;
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("name"))
    				name = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue();
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("asset_ref"))
    				codeB = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue();
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("route_ref"))
    				routes = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue().split(";");
	    		else if(nodeTags.item(t).getAttributes().getNamedItem("k").getNodeValue().equals("ref"))
    				codeM = nodeTags.item(t).getAttributes().getNamedItem("v").getNodeValue();
	    	}
	    	if(isBusStop && !name.equals("") && !codeB.equals("")) {
		    	try {
		    		dba.executeStatement("INSERT INTO BusStops VALUES ('"+codeB+"','"+name+"',"+latitude+","+longitude+")");
		    		System.out.println("Bus "+stops);
		    		stops++;
				} catch (SQLException e) {
					System.out.println(e.getMessage() + " EAST");
					replays++;
				}
				if(routes!=null)
					try {
						for(int r=0; r<routes.length; r++)
							dba.executeStatement("INSERT INTO BusStopsRoutes VALUES ('"+codeB+"','"+routes[r]+"')");
					} catch (SQLException e) {
						System.out.println(e.getMessage() + " EAST");
					}
	    	}
			else if(isMRT && !name.equals(""))
		    	try {
		    		if(codeM.equals(""))
						dba.executeStatement("INSERT INTO MRTStops (Name,OSMLatitude,OSMLongitude) VALUES ('"+name+"',"+latitude+","+longitude+")");
		    		else
		    			dba.executeStatement("INSERT INTO MRTStops (RouteCode,Name,OSMLatitude,OSMLongitude) VALUES ('"+codeM+"','"+name+"',"+latitude+","+longitude+")");
		    		System.out.println("MRT "+stops);
		    		stops++;
				} catch (SQLException e) {
					System.out.println(e.getMessage() + " EAST");
					replays++;
				}
	    }
	    System.out.println(replays);
	}
	
}
