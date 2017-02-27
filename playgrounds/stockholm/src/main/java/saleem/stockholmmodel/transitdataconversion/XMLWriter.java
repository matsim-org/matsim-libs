package saleem.stockholmmodel.transitdataconversion;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/** 
 * A class to write loaded (translated from excel reports) transit schedule and vehicles 
 * into TransitSchedule.xml and Vehicles.xml file.
 * 
 * @author Mohammad Saleem
 */
public class XMLWriter {
	//To convert from WGS84 into SWEREF99, as recommended in Matsim specifications to avoid using spherical coordinates. 
	private CoordinateTransformation ct = StockholmTransformationFactory.getCoordinateTransformation(StockholmTransformationFactory.WGS84, StockholmTransformationFactory.WGS84_SWEREF99);
	public Document createDocument(String rootname){
		Element element = new Element(rootname);
		Document doc = new Document();
		doc.setRootElement(element);
		return doc;
	}
	public void writeDocument(Document doc, String path) throws IOException{
		XMLOutputter xmlOutput = new XMLOutputter();
		// display nice nice
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(path));
		System.out.println("File Saved!");
	}
	public void addStopFacility(Element tstops, Stop stop ){
		Element stopFacility  = new Element("stopFacility");
		stopFacility.setAttribute(new Attribute("isBlocking", Boolean.toString(stop.getIsBlocking())));
		stopFacility.setAttribute(new Attribute("name",stop.getName() ));
		Coord coord = stop.getCoord();
		coord = ct.transform(coord);
		stopFacility.setAttribute(new Attribute("x",Double.toString(coord.getX())));
		stopFacility.setAttribute(new Attribute("y",Double.toString(coord.getY())));
		stopFacility.setAttribute(new Attribute("id",stop.getId()));
		tstops.addContent(stopFacility);
	}
	public void addStop(Element transitRoute, Stop st ){
		Element stop  = new Element("stop");
		stop.setAttribute(new Attribute("awaitDeparture", Boolean.toString(st.getAwaitDeparture())));
		stop.setAttribute(new Attribute("departureOffset",st.getDepartureOffset()));
		stop.setAttribute(new Attribute("arrivalOffset",st.getArrivalOffset()));
		stop.setAttribute(new Attribute("refId",st.getId()));
		transitRoute.addContent(stop);
	}
	public void addLink(Element route, Link link ){
		Element link_element  = new Element("link");
		link_element.setAttribute(new Attribute("refId", ""));//Temporarily for teleportation
		route.addContent(link_element);
	}
	public void addDeparture(Element departures_element, Departure departure ){
		Element dep  = new Element("departure");
		dep.setAttribute(new Attribute("id", departure.getId()));
		dep.setAttribute(new Attribute("vehicleRefId", departure.getVehicleRefId()));
		dep.setAttribute(new Attribute("departureTime", departure.getDepartureTime()));
		departures_element.addContent(dep);
	}
	public void addTransitRoute(Element tline, TransitRoute troute ){
		Element transitroute = new Element("transitRoute");
		transitroute.setAttribute(new Attribute("id",troute.getID()));
		List<Stop> routeprofile = troute.getRouteProfile();
		List<Link> route = troute.getRoute();
		List<Departure> departures = troute.getDepartures();
		Element transportmode = new Element("transportMode");
		//transportmode.addContent(routeprofile.get(0).getTransportMode());//get transport mode from the first stop
		transportmode.addContent("pt");
		Element rprof = new Element("routeProfile");
		Element route_element = new Element("route");
		Element departures_element = new Element("departures");
		Iterator iter = routeprofile.iterator();
		while(iter.hasNext()){
			Stop stop = (Stop)iter.next();
			addStop(rprof, stop);
			
		}
		Iterator iter1 = route.iterator();
		while(iter1.hasNext()){
			Link link = (Link)iter1.next();
			addLink(route_element, link);
			
		}
		Iterator iter2 = departures.iterator();
		while(iter2.hasNext()){
			Departure departure = (Departure)iter2.next();
			addDeparture(departures_element, departure);
			
		}
		transitroute.addContent(transportmode);
		transitroute.addContent(rprof);
		//transitroute.addContent(route_element);
		transitroute.addContent(departures_element);
		tline.addContent(transitroute);
	}
	public void addTransitLine(Element doc, Line line ){
		Element tline = new Element("transitLine");
		tline.setAttribute(new Attribute("id",line.getLineId()));
		List<TransitRoute> troutes = line.getTransitRoutes();
		Iterator iter = troutes.iterator();
		while(iter.hasNext()){
			TransitRoute troute = (TransitRoute)iter.next();
			addTransitRoute(tline,troute);
		}
		doc.addContent(tline);
	}
	public void addVehicle(Element doc, Vehicle vehicle ){
		Element vehicle_element = new Element("vehicle");
		vehicle_element.setAttribute(new Attribute("id",vehicle.getID()));
		vehicle_element.setAttribute(new Attribute("type",vehicle.getType()));
		doc.addContent(vehicle_element);
	}
	public void addVehicleType(Element doc, VehicleType vehicletype ){
		Element vehicletype_element = new Element("vehicleType");
		vehicletype_element.setAttribute(new Attribute("id",vehicletype.getID()));
		Element capacity = new Element("capacity");
		Element seats = new Element("seats");
		seats.setAttribute(new Attribute("persons",Integer.toString(vehicletype.getNumberOfSeats())));
		Element standingroom = new Element("standingRoom");
		standingroom.setAttribute(new Attribute("persons",Integer.toString(vehicletype.getStandingCapacity())));
		capacity.addContent(seats);
		capacity.addContent(standingroom);
		vehicletype_element.addContent(capacity);
		Element length = new Element("length");
		length .setAttribute(new Attribute("meter",Double.toString(vehicletype.getLength())));
		vehicletype_element.addContent(length);
		Element width = new Element("width");
		width .setAttribute(new Attribute("meter",Double.toString(vehicletype.getWidth())));
		vehicletype_element.addContent(width);
		Element accesstime = new Element("accessTime");
		accesstime.setAttribute(new Attribute("secondsPerPerson",Double.toString(vehicletype.getAccessTime())));
		vehicletype_element.addContent(accesstime);
		Element egresstime  = new Element("egressTime");
		egresstime .setAttribute(new Attribute("secondsPerPerson",Double.toString(vehicletype.getEgressTime())));
		vehicletype_element.addContent(egresstime);
		Element dooroperation  = new Element("doorOperation");
		dooroperation .setAttribute(new Attribute("mode",vehicletype.getDoorOperationMode()));
		vehicletype_element.addContent(dooroperation);
		Element pce   = new Element("passengerCarEquivalents");
		pce.setAttribute(new Attribute("pce",Double.toString(vehicletype.getPassengerCarEquivalents())));
		vehicletype_element.addContent(pce);
		doc.addContent(vehicletype_element);
	}
	public void createVehiclesXML(TransitSchedule transit, String path){
		try {
			ExcelReportsReader ex = new ExcelReportsReader();
			List<VehicleType> vehicletypes = transit.getVehicleTypes();
			List<Vehicle> vehicles = transit.getVehicles();
			
			Document doc = createDocument("vehicleDefinitions");
			for(int i=0; i<vehicletypes.size();i++){
				addVehicleType(doc.getRootElement(), vehicletypes.get(i));
			}
			for(int i=0; i<vehicles.size();i++){
				addVehicle(doc.getRootElement(), vehicles.get(i));
			}
			writeDocument(doc, path);
			// new XMLOutputter().output(doc, System.out);
		  } catch (IOException io) {
			System.out.println(io.getMessage());
		  }
	}
	public void createTransitSchedule(TransitSchedule transit, String path){
		try {
			ExcelReportsReader ex = new ExcelReportsReader();
			List<Stop> stops = transit.getStops();
			List<Line> lines = transit.getLines();
			
			Document doc = createDocument("transitSchedule");
			Element tstops = new Element("transitStops");
			for(int i=0; i<stops.size();i++){
				addStopFacility(tstops, stops.get(i));
			}
			doc.getRootElement().addContent(tstops);
			for(int i=0; i<lines.size();i++){
				addTransitLine(doc.getRootElement(), lines.get(i));
			}
			writeDocument(doc, path);
			// new XMLOutputter().output(doc, System.out);
		  } catch (IOException io) {
			System.out.println(io.getMessage());
		  }
	}
}
