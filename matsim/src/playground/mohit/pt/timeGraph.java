package playground.mohit.pt;

import java.io.File;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.map.LinkedMap;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.transitSchedule.TransitStopFacility;
import org.w3c.dom.*; 


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

import playground.marcel.pt.transitSchedule.TransitScheduleBuilderImpl;
import playground.marcel.pt.transitSchedule.api.TransitRouteStop;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.marcel.pt.transitSchedule.api.TransitScheduleBuilder;
import java.awt.*;




public class timeGraph  {
	private Choice listOfLineRoutes= new Choice() ;
	private int totalLineRoutes;
	public final Map<String , TransitLineRoute> lineRoutes = new TreeMap<String, TransitLineRoute>();
	TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
	TransitSchedule schedule = builder.createTransitSchedule();
	NodeList LineRoutes;
	public timeGraph(){
		
		try {


			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (new File("C:/Users/MOSES/Desktop/transitSchedulefina.xml"));
			doc.getDocumentElement ().normalize ();
			
			LineRoutes = doc.getElementsByTagName("transitRoute");
			totalLineRoutes = LineRoutes.getLength();				
			for(int i=0; i<totalLineRoutes ; i++){
			Element elm = (Element)LineRoutes.item(i);
			String name = elm.getAttribute("id");
			listOfLineRoutes.add(name);
			}
		

			
		}


			catch (SAXParseException err) {
			System.out.println ("** Parsing error" + ", line " + err.getLineNumber () + ", uri " + err.getSystemId ());
			System.out.println(" " + err.getMessage ());

			}catch (SAXException e) {
			Exception x = e.getException ();
			((x == null) ? e : x).printStackTrace ();

			}catch (Throwable t) {
			t.printStackTrace ();
			}
		
		
	}
	
	
	public double toDouble(final String s)
	{   Double d;
		d = Double.parseDouble(s.substring(0, 2))*60 + Double.parseDouble(s.substring(3, 5)) + Double.parseDouble(s.substring(6, 8))/60;
		return d;
	}

	public void read(){
		
		for(int i=0; i<totalLineRoutes ; i++){
			Element elm = (Element)LineRoutes.item(i);
			Map<String, TransitRouteStop> stops = new LinkedMap();
			
				NodeList Stops = elm.getElementsByTagName("stop");
				int totalStops = Stops.getLength();
				String stopId = null, arr = null, dep = null;
				for(int j=0; j<totalStops ; j++){
					Element el = (Element)Stops.item(j);
					stopId = el.getAttribute("refId");
					
					arr = el.getAttribute("arrivalOffset");
					dep = el.getAttribute("departureOffset");
					
					TransitStopFacility stop = schedule.getBuilder().createTransitStopFacility(new IdImpl(stopId),null );
					TransitRouteStop routeStop = schedule.getBuilder().createTransitRouteStop(stop,toDouble(arr),toDouble(dep));
					stops.put(stopId+"/"+(j+1),routeStop);
				}
				TransitLineRoute route = new  TransitLineRoute(new IdImpl(elm.getAttribute("id")),stops);
				lineRoutes.put(elm.getAttribute("id"), route);
			
		}
		
	
		new drawGraph(lineRoutes);
	}
	
	public static void main(String argv []){
	new timeGraph().read();	
	
	}
	
	
	public class TransitLineRoute{
		public Id id;
		
		Map<String, TransitRouteStop> stops = new LinkedMap();
		public TransitLineRoute(Id id, Map<String, TransitRouteStop> stops){
			this.stops= stops;
			this.id =id;
		}
		
		
	}
	





}
