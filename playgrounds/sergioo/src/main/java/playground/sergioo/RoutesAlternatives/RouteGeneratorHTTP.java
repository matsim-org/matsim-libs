package playground.sergioo.RoutesAlternatives;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import playground.sergioo.AddressLocator.BadAddressException;
import playground.sergioo.AddressLocator.Location;

/**
 * Hello world!
 */
public class RouteGeneratorHTTP extends TestCase {
	
	//Constants
	private static final String HTTP_SCHEME = "http";
	private static final String GOOGLE_MAPS_HOST = "maps.google.com";
	private static final String PATH_ROUTING = "/maps";
	private static final int HTTP_STATUS_OK = 200;
	//private static int i;
	
	//Attributes
	/**
	 * The origin address or location
	 */
	private String origin;
	/**
	 * The destination address or location
	 */
	private String destination;
	/**
	 * The mode of the route
	 */
	private ModeOption modeOption;
	/**
	 * The alternative routes
	 */
	private List<Route> routes;
	/**
	 * The response of the query
	 */
	private String allResponse;
	
	//Methods
	/**
	 * Constructs an address locator given the address
	 * @param modes 
	 * @param address
	 * @throws BadAddressException 
	 * @throws URISyntaxException 
	 * @throws NoRoutesException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws ClientProtocolException 
	 */
	public RouteGeneratorHTTP(String origin, String destination, ModeOption mode) throws ClientProtocolException, IllegalStateException, IOException, ParserConfigurationException, SAXException, NoRoutesException, URISyntaxException, BadAddressException {
		super();
		this.origin = origin;
		this.destination = destination;
		this.modeOption = mode;
		routes = new ArrayList<Route>();
		locate();
	}
	/**
	 * @return the origin
	 */
	public String getOrigin() {
		return origin;
	}
	/**
	 * @param origin the origin to set
	 */
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}
	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}
	/**
	 * @return the route
	 * @throws Exception 
	 */
	public List<Route> getRoutes() throws Exception {
		return routes;
	}
	/**
	 * @return the number of routes
	 * @throws Exception 
	 */
	public int getNumRoutes() throws Exception {
		return routes.size();
	}
	/**
	 * Locates the address
	 * @throws URISyntaxException 
	 * @throws NoRoutesException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws ClientProtocolException 
	 * @throws BadAddressException 
	 */
	private void locate() throws ClientProtocolException, IllegalStateException, IOException, ParserConfigurationException, SAXException, NoRoutesException, URISyntaxException, BadAddressException {
		/*AddressLocator addressLocator = new AddressLocator(origin);
		Location originLocation = addressLocator.getLocation();
		addressLocator = new AddressLocator(destination);
		Location destinationLocation = addressLocator.getLocation();*/
		Document response=getResponse(getRequest());
		String markersText=allResponse.split("markers:")[1];
		markersText=markersText.substring(0,markersText.indexOf("]}"));
		String[] markersParts=markersText.split("latlng:\\{lat:");
		Location originLocation = new Location(Double.parseDouble(markersParts[1].substring(0,markersParts[1].indexOf(",lng:"))),Double.parseDouble(markersParts[1].substring(markersParts[1].indexOf(",lng:")+5,markersParts[1].indexOf("}"))));
		Location destinationLocation = new Location(Double.parseDouble(markersParts[2].substring(0,markersParts[2].indexOf(",lng:"))),Double.parseDouble(markersParts[2].substring(markersParts[2].indexOf(",lng:")+5,markersParts[2].indexOf("}"))));
		NodeList results=null;
		switch(modeOption) {
		case DRIVING:
			results = response.getElementsByTagName("li");
			for(int i=0; i<results.getLength(); i++) {
				String textTime = ((Element)((Element)results.item(i)).getElementsByTagName("div").item(0)).getElementsByTagName("div").item(1).getChildNodes().item(0).getNodeValue();
				routes.add(new Route(originLocation,destinationLocation,calculateSeconds(textTime)));
			}
			break;
		case WALKING:
			results = response.getElementsByTagName("li");
			for(int i=0; i<results.getLength(); i++) {
				String textTime = ((Element)((Element)results.item(i)).getElementsByTagName("div").item(0)).getElementsByTagName("div").item(1).getChildNodes().item(0).getNodeValue();
				routes.add(new Route(originLocation,destinationLocation,calculateSeconds(textTime)));
			}
			break;
		case PUBLIC_TRANSIT:
			NodeList alternatives = response.getChildNodes().item(0).getChildNodes();
			for(int r=0; r<alternatives.getLength(); r++) {
				NodeList alternativeParts = alternatives.item(r).getChildNodes();
				String textTime = alternativeParts.item(1).getChildNodes().item(1).getChildNodes().item(1).getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
				PublicTransitRoute publicTransitRoute = new PublicTransitRoute(originLocation,destinationLocation,calculateSeconds(textTime));
				List<String> points = new ArrayList<String>();
				List<Mode> modes = new ArrayList<Mode>();
				List<String> originStationIds = new ArrayList<String>();
				List<String> destinationStationIds = new ArrayList<String>();
				List<String> vehicleRouteIds = new ArrayList<String>();
				points.add(origin);
				NodeList legs = alternativeParts.item(0).getChildNodes();
				for(int i=1; i<legs.getLength(); i+=2) {
					String info = legs.item(i).getTextContent();
					Mode modeA=null;
					for(Mode mode:Mode.values())
						if(info.toUpperCase().startsWith(mode.name())) {
							modeA=mode;
							break;
						}
					modes.add(modeA);
					if(modeA.equals(Mode.WALK)) {
						originStationIds.add(null);
						destinationStationIds.add(null);
						vehicleRouteIds.add(null);
						String legDestination = info.substring(8, info.indexOf("About "));
						points.add(legDestination);
					}
					else {
						String[] infoParts = info.split("-");
						vehicleRouteIds.add(infoParts[1].trim());
						String[] infoParts2 = info.split(" Arrive ");
						String[] infoParts3 = infoParts2[0].split(" \\(Stop ID: ");
						String[] infoParts4 = infoParts2[1].split(" \\(Stop ID: ");
						if(infoParts4.length>1) {
							points.add(infoParts4[0]);
							originStationIds.add(infoParts3[1].substring(0, infoParts4[1].indexOf(')')));
							destinationStationIds.add(infoParts4[1].substring(0, infoParts4[1].indexOf(')')));
						}
						else {
							points.add(infoParts2[1].split("PreviousZoom")[0]);
							originStationIds.add(null);
							destinationStationIds.add(null);
						}
					}
				}
				try {
					publicTransitRoute.addLegs(points, modes, originStationIds, destinationStationIds, vehicleRouteIds);
				} catch (Exception e) {
					e.printStackTrace();
				}
				routes.add(publicTransitRoute);
			}
			break;
		}
	}
	/**
	 * @param textTime
	 * @return the time in seconds accorting to the text
	 */
	private double calculateSeconds(String textTime) {
		double totalTime=0;
		String[] textTimeParts = textTime.split(" ");
		int pos=0;
		while(pos+1<textTimeParts.length)
			if(textTimeParts[pos+1].startsWith("day")) {
				totalTime+=Integer.parseInt(textTimeParts[pos])*24*60*60;
				pos+=2;
			}
			else if(textTimeParts[pos+1].startsWith("hour")) {
				totalTime+=Integer.parseInt(textTimeParts[pos])*60*60;
				pos+=2;
			}
			else if(textTimeParts[pos+1].startsWith("min")) {
				totalTime+=Integer.parseInt(textTimeParts[pos])*60;
				pos+=2;
			}
		return totalTime;
	}
	/**
	 * @return The get HTTP protocol request
	 * @throws URISyntaxException 
	 */
	private HttpGet getRequest() throws URISyntaxException {
		List<NameValuePair> qParams = new ArrayList<NameValuePair>();
		qParams.add(new BasicNameValuePair("saddr", origin));
		qParams.add(new BasicNameValuePair("daddr", destination));
		qParams.add(new BasicNameValuePair("dirflg", modeOption.getParam()));
		URI uri = URIUtils.createURI(HTTP_SCHEME, GOOGLE_MAPS_HOST, -1, PATH_ROUTING, URLEncodedUtils.format(qParams, "UTF-8"), null);
		System.out.println("URL: "+uri);
		return new HttpGet(uri);
	}
	/**
	 * @param request
	 * @return The processed response
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws IllegalStateException
	 * @throws SAXException
	 * @throws NoRoutesException 
	 */
	private Document getResponse(HttpGet request) throws ClientProtocolException, IOException, ParserConfigurationException, IllegalStateException, SAXException, NoRoutesException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(request);
		if(response.getStatusLine().getStatusCode()!=HTTP_STATUS_OK)
			throw new ClientProtocolException("The HTTP request is wrong.");
		allResponse = EntityUtils.toString(response.getEntity());
		String responseText = "";
		/*PrintWriter writerAux = new PrintWriter(new File("./data/response.html"));
		writerAux.println(allResponse);
		writerAux.close();*/
		switch(modeOption) {
		case DRIVING:
			try {
				responseText=allResponse.substring(allResponse.indexOf("<ol"), allResponse.indexOf("</ol>")+5);
			}
			catch(Exception e) {
				throw new NoRoutesException();
			}
			break;
		case WALKING:
			try {
				responseText=allResponse.substring(allResponse.indexOf("<ol"), allResponse.indexOf("</ol>")+5);
			}
			catch(Exception e) {
				throw new NoRoutesException();
			}
			break;
		case PUBLIC_TRANSIT:
			String responseWithErrors="";
			try {
				responseWithErrors = allResponse.substring(allResponse.indexOf("<div id=\"tsp\">"), allResponse.indexOf("<div class=\"ddwpt\" id=\"panel_ddw1\" oi=\"wi1\">"));
			}
			catch(Exception e) {
				throw new NoRoutesException();
			}
			responseText=responseWithErrors.replaceAll(".gif\">", ".gif\"/>").replaceAll("colspan=2", "colspan=\"2\"").replaceAll("nowrap", "").replaceAll("&laquo;", "").replaceAll("&nbsp;", "").replaceAll("&raquo;", "");
			break;
		}
		File xmlFile = new File("./data/temp/response.xml");
		PrintWriter writer = new PrintWriter(xmlFile);
		writer.println(responseText);
		writer.close();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    return dBuilder.parse(xmlFile);
	}
	
	//Main method
	/**
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws ParseException 
	 * @throws Exception 
	 */
	public static void main( String[] args ) throws ParseException, ClientProtocolException, IOException {
		try {
			String[] origins = {"New York","Clementi Rd, Singapore","Seattle"};
			String[] destinations = {"Boston","Still Rd, Singapore","New York"};
			int option=1;
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Insert the required origin:");
			String origin = origins[option];//reader.readLine();
			System.out.println("Insert the required destination:");
			String destination = destinations[option];//reader.readLine();
			System.out.println("Insert the required mode (Driving=1, Walking=2, Public transit=3, Biking=4):");
			int mode = 2;//Integer.parseInt(reader.readLine())-1;
			reader.close();
			long initialTime = System.currentTimeMillis(), initialTimeIteration=initialTime;
			for(int i=0;true;) {
				RouteGeneratorHTTP routeGenerator = new RouteGeneratorHTTP(origin,destination,ModeOption.values()[mode]);
				List<Route> routes = routeGenerator.getRoutes();
				for(Route route:routes)
					System.out.println(route);
				i++;
				long actualTime = System.currentTimeMillis();
				System.out.println(i + "	"+(actualTime-initialTimeIteration)+"	"+(actualTime-initialTime)/i );
				initialTimeIteration = System.currentTimeMillis();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: "+e.getMessage());
		}
	}
	
}
