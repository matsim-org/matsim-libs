package playground.toronto;

/**
 * An enumeration of special link types flagged in the Toronto network.
 * 
 * @author pkucirek
 *
 */
public class TorontoLinkTypes {

	public static final String highway = "Highway";
	public static final String loop = "LOOP"; //Transit stops at nodes have a LOOP link.
	public static final String ramp = "On/Off Ramp";
	public static final String hovTransfer = "HOV transfer";
	public static final String hov = "HOV";
	public static final String centroidConnector = "CC";
	public static final String ETR407 = "Toll Highway";
	public static final String streetcarROW = "Streetcar ROW";
	public static final String transfer = "Transfer";
	public static final String turn = "TURN"; //For turns created using ManeuverCreation
	public static final String tunnel = "Tunnel"; //Reserved for future use. I don't think there are many tunnels in Toronto, apart from underground infrastructure.
	public static final String bridge = "Bridge"; //Reserved for future use.
	public static final String busROW = "Bus ROW"; //Reserved for future use.
	public static final String trucksOnly = "Truck Corridor"; //Reserved for future use.
	
}
