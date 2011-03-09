package playground.sergioo.RoutesAlternatives;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import playground.sergioo.AddressLocator.BadAddressException;

import junit.framework.TestCase;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlListItem;
import com.gargoylesoftware.htmlunit.html.HtmlOrderedList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Hello world!
 */
public class RouteGeneratorHTML extends TestCase {
	
	//Constants
	private static final String HTTP_SCHEME = "http";
	private static final String GOOGLE_MAPS_HOST = "maps.google.com";
	private static final String PATH_ROUTING = "/maps";
	
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
	private ModeOption mode;
	/**
	 * The alternative routes
	 */
	private List<Route> routes;
	/**
	 * If the address is already located
	 */
	private boolean routed;
	
	//Methods
	/**
	 * Constructs an address locator given the address
	 * @param modes 
	 * @param address
	 */
	public RouteGeneratorHTML(String origin, String destination, ModeOption mode) {
		super();
		this.origin = origin;
		this.destination = destination;
		this.mode = mode;
		routes = new ArrayList<Route>();
		routed = false;
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
	 * @return the routes
	 * @throws Exception 
	 */
	public List<Route> getRoute() throws Exception {
		if(routed)
			return routes;
		else
			throw new Exception("The address is not located yet.");
	}
	/**
	 * @return the number of routes
	 * @throws Exception 
	 */
	public int getNumRoutes() throws Exception {
		if(routed)
			return routes.size();
		else
			throw new Exception("The address is not located yet.");
	}
	/**
	 * @return the routed
	 */
	public boolean isRouted() {
		return routed;
	}
	/**
	 * Locates the address
	 * @throws URISyntaxException 
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws IllegalStateException 
	 * @throws BadAddressException 
	 */
	public void locate() throws ElementNotFoundException, URISyntaxException, IOException, ParserConfigurationException, IllegalStateException, BadAddressException {
		final WebClient webClient = new WebClient();
		webClient.setCssEnabled(false);
		webClient.setJavaScriptEnabled(true);
		HtmlPage page = webClient.getPage(getUrlRequest());
		HtmlOrderedList alternatives = page.getHtmlElementById("dir_altroutes_body");
		for(HtmlElement element:alternatives.getElementsByTagName("li")) {
			//TODO
			HtmlListItem alternative = (HtmlListItem) element;
			Iterator<HtmlElement> i = alternative.getChildElements().iterator();
			while(i.hasNext())
				System.out.println(i.next());
		}
	}
	/**
	 * @return The get HTTP protocol request
	 * @throws URISyntaxException 
	 */
	public String getUrlRequest() throws URISyntaxException {
		List<NameValuePair> qParams = new ArrayList<NameValuePair>();
		qParams.add(new BasicNameValuePair("saddr", origin));
		qParams.add(new BasicNameValuePair("daddr", destination));
		qParams.add(new BasicNameValuePair("dirflg", mode.getParam()));
		URI uri = URIUtils.createURI(HTTP_SCHEME, GOOGLE_MAPS_HOST, -1, PATH_ROUTING, URLEncodedUtils.format(qParams, "UTF-8"), null);
		System.out.println("URL: "+uri);
		return uri.toString();
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
	public static void main( String[] args ) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Insert the required origin:");
			String origin = reader.readLine();
			System.out.println("Insert the required destination:");
			String destination = reader.readLine();
			System.out.println("Insert the required mode (Driving=1, Walking=2, Public transit=3, Biking=4):");
			int mode = Integer.parseInt(reader.readLine())-1;
			RouteGeneratorHTML routeGenerator = new RouteGeneratorHTML(origin,destination,ModeOption.values()[mode]);
			routeGenerator.locate();
			List<Route> routes = routeGenerator.getRoute();
			for(Route route:routes)
				System.out.println(route);
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("Error: "+e.getMessage());
		}
	}
	
}
