package saleem.stockholmscenario.teleportation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import saleem.stockholmscenario.utils.CoordinateSystemConverter;
import saleem.stockholmscenario.utils.DistanceCalculation;
import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import au.com.bytecode.opencsv.CSVWriter;

public class XMLReaderWriter {
	Map<String, Element> nodesmap = new HashMap<String, Element>();
	public Document readFile(String path){
		SAXBuilder saxBuilder = new SAXBuilder();
		File xmlFile = new File(path);
		try{
			Document document = (Document) saxBuilder.build(xmlFile);
			return document;
		}catch (IOException io) {
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            System.out.println(jdomex.getMessage());
        }
		return null;
		
	}
	public Document modifyPopulation(Document document){//Converts Population into RT90 from SWEREF99
		CoordinateTransformation ct = StockholmTransformationFactory.getCoordinateTransformation(StockholmTransformationFactory.WGS84_SWEREF99, StockholmTransformationFactory.WGS84_RT90);
		Element rootElement = document.getRootElement();
		List persons = rootElement.getChildren("person");
		for (int i = 0; i < persons.size(); i++) {
			 Element person = (Element) persons.get(i);
			 List activs = ((Element)person).getChild("plan").getChildren("act");
			 for(int j = 0; j < activs.size(); j++){
				 Element act = (Element) activs.get(j);
				 Coord coord = new CoordImpl(Double.parseDouble(act.getAttributeValue("x")), Double.parseDouble(act.getAttributeValue("y")));
				 coord = ct.transform(coord);
				 act.setAttribute("x", Double.toString(coord.getX()));
				 act.setAttribute("y", Double.toString(coord.getY()));
			 }
		}
		return document;
	} 
	public List<String> createStops(Document document){//For creating stops for ptStops.csv
		List<String> stops = new LinkedList<String>();
		List<Element> stopfacilities = ((Element)document.getRootElement().getChild("transitStops")).getChildren("stopFacility");
		Iterator<Element> iter = stopfacilities.iterator();
		while(iter.hasNext()){
			Element stopfacility = iter.next();
			String stop = stopfacility.getAttributeValue("id") + "," + stopfacility.getAttributeValue("x") + "," + stopfacility.getAttributeValue("y");
			stops.add(stop);
		}
		return stops;
	}
	public void createPTNetworkStops(Document transit, Document network){//For creating stops for PT Network
		Element element = new Element("nodes");
		List<Element> stopfacilities = ((Element)transit.getRootElement().getChild("transitStops")).getChildren("stopFacility");
		Iterator<Element> iter = stopfacilities.iterator();
		while(iter.hasNext()){
			Element node = new Element("node");
			Element stopfacility = iter.next();
			node.setAttribute("y", stopfacility.getAttributeValue("y"));
			node.setAttribute("x", stopfacility.getAttributeValue("x"));
			node.setAttribute("id", stopfacility.getAttributeValue("id"));
			element.addContent(node);
			nodesmap.put(stopfacility.getAttributeValue("id"), node);
		}
		network.getRootElement().addContent(element);
	}
	public Element getStopThroughID(List<Element> stopfacilities, String id){
		Iterator<Element> iter = stopfacilities.iterator();
		while(iter.hasNext()){
			Element stopfacility = iter.next();
			if(stopfacility.getAttributeValue("id").equals(id)){
				return stopfacility;
			}
		}
		return null;
	}
	public void addLink(Element route, String refId ){
		Element link_element  = new Element("link");
		link_element.setAttribute(new Attribute("refId", refId));
		//link_element.setAttribute(new Attribute("refId", link.getRefID()));
		route.addContent(link_element);
	}
	public void createPTNetworkLinks(Document transit, Document network){//For creating Links for PT Network, and adding them into Transit Schedule Routes
		List<Element> stopfacilities = ((Element)transit.getRootElement().getChild("transitStops")).getChildren("stopFacility");
		Element links = new Element("links");
		List<String> linkids = new LinkedList<String>();
		List<Element> tlines = transit.getRootElement().getChildren("transitLine");
		Iterator<Element> iter = tlines.iterator();
		while(iter.hasNext()){
			Element tline = iter.next();
			List<Element> troutes = tline.getChildren("transitRoute");
			Iterator<Element> routeiter = troutes.iterator();
			while(routeiter.hasNext()){
				Element troute = routeiter.next();
				List<Element> stops = troute.getChild("routeProfile").getChildren("stop");
				Element route = troute.getChild("route");
				route.removeChildren("link");
				for(int i=0;i<stops.size()-1;i++){
					Element origin = (Element)stops.get(i);
					Element destination = (Element)stops.get(i+1);	
					String from = origin.getAttributeValue("refId");
					String to = destination.getAttributeValue("refId");
					Element fromstop = getStopThroughID(stopfacilities, from);//To set linkRefId for stopfacilitites
					Element tostop = getStopThroughID(stopfacilities, to);
					String id=from+"to"+to;
					if(fromstop.getAttributeValue("linkRefId") == null || fromstop.getAttributeValue("linkRefId").equals("")){
						//fromstop.setAttribute("linkRefId", id);
					}
					if(tostop.getAttributeValue("linkRefId") == null || tostop.getAttributeValue("linkRefId").equals("")){//The check makes sure that if a link already exists, do not add it again
						//tostop.setAttribute("linkRefId", id);
					}
					//routelinks.get(i).setAttribute("refId", id);
					//addLink(route, fromstop.getAttribute("linkRefId").getValue());
					addLink(route, id);
					//addLink(route, tostop.getAttribute("linkRefId").getValue());
					if(!linkids.contains(id)){
						
						linkids.add(id);
						Element link = new Element("link");
						double x = Double.parseDouble(nodesmap.get(from).getAttributeValue("x"));//To get x, y coordinates from stopsfacilities, as the stops in route profile don't have those details
						double y = Double.parseDouble(nodesmap.get(from).getAttributeValue("y"));
						double x1 = Double.parseDouble(nodesmap.get(to).getAttributeValue("x"));
						double y1 = Double.parseDouble(nodesmap.get(to).getAttributeValue("y"));
						link.setAttribute("id", id);
						link.setAttribute("from", from);
						link.setAttribute("to", to);
						link.setAttribute("length", Double.toString(DistanceCalculation.calculateDistanceUTM(x, y, x1, y1)));
						link.setAttribute("freespeed", "20.0");
						link.setAttribute("capacity", "600");
						link.setAttribute("permlanes", "1.0");
						link.setAttribute("oneway", "1");
						link.setAttribute("modes", "car");
						links.addContent(link);
					}
				}
				removeRepetetiveLinksAndEmptyLinks(route);//To remove repetetive links from routes or links with no refIds
			}
		}
		network.getRootElement().addContent(links);
		writeDocumentInXML(transit, "H:\\Matsim\\Stockholm Scenario\\teleportation\\transitSchedule.xml");
		
	}
	public void removeRepetetiveLinksAndEmptyLinks(Element route){//To remove repetetive and empty links from routes
		List<Element> routelinks = route.getChildren("link");
		List<Element> newroutelinks = new LinkedList<Element>();
		Iterator<Element> routelinksiter  = routelinks.iterator();
		while (routelinksiter.hasNext()){
			Element link = routelinksiter.next();
			if(!link.getAttributeValue("refId").equals("")) {
				if(newroutelinks.size() == 0){
					newroutelinks.add(link);
				}
				else{
					if(!newroutelinks.get(newroutelinks.size()-1).getAttributeValue("refId").equals(link.getAttributeValue("refId")) ){
						newroutelinks.add(link);
					}
					else{
						System.out.println("Repetetive Link: " + link.getAttributeValue("refId"));
					}
				}
			}
		}
		routelinks.removeAll(routelinks);
		routelinks.addAll(newroutelinks);
	}
	public void writePTNetworkStopsAndLinks(Document transit, String path){//For creating stops for ptStops.csv
		Document network = createDocument("network");
		createPTNetworkStops(transit, network);//Create stops elements and add to network
		createPTNetworkLinks(transit, network);//Create stops elements and add to network
		writeDocumentInXML(network, path);
	}
	public void writeStops(List<String> stops, String path){//For creating stops for ptStops.csv
		boolean alreadyExists = new File(path).exists();
		String firstline = "id,x,y";
		Iterator<String> iter = stops.iterator();
		// if the file didn't already exist then we need to write out the header line
		try {
			CSVWriter csvOutput = new CSVWriter(new FileWriter(path, true), ',', '\0');
			if (!alreadyExists)
			{
				csvOutput.writeNext(firstline.split(","));
			}
			while(iter.hasNext()){
				csvOutput.writeNext(iter.next().split(","));
			}
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public Document modifyNetwork(Document document){
		CoordinateSystemConverter converter = new CoordinateSystemConverter();
		Element rootElement = document.getRootElement();
		List listElement = ((Element)(rootElement.getChildren("nodes").get(0))).getChildren("node");
		for (int i = 0; i < listElement.size(); i++) {
			 Element node = (Element) listElement.get(i);
			 Coord coord = new CoordImpl(Double.parseDouble(node.getAttributeValue("x")), Double.parseDouble(node.getAttributeValue("y")));
			 coord=converter.deg2UTM(coord);
			 node.setAttribute("x", Double.toString(coord.getX()));
			 node.setAttribute("y", Double.toString(coord.getY()));
			 System.out.println("X : "+ node.getAttributeValue("x") + " Y : " + node.getAttributeValue("y"));
		}
		return document;
	}
	public Document createDocument(String rootname){
		Element element = new Element(rootname);
		Document doc = new Document();
		doc.setRootElement(element);
		return doc;
	}
	public void writeDocumentInXML(Document doc, String path){
		try{
		XMLOutputter xmlOutput = new XMLOutputter();
		// display nice nice
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(path));
		System.out.println("File Saved!");
		}catch(IOException io){
			io.printStackTrace();
		}
	}
}
