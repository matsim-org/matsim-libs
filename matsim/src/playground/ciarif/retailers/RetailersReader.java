package playground.ciarif.retailers;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class RetailersReader extends MatsimXmlParser {
	
	private final static String RETAILERS = "retailers";
	private final static String RETAILER = "retailer";
	private final static String FACILITY = "facility";

	private final Retailers retailers;
	private Retailer curr_retailer = null;

	private static final Logger log = Logger.getLogger(RetailersReader.class);

	public RetailersReader (final Retailers retailers) {
		this.retailers = retailers;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (FACILITY.equals(name)) {
			startFacility(atts);
		} else if (RETAILER.equals(name)) {
			startRetailer(atts);
		} else if (RETAILERS.equals(name)) {
			startRetailers(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {

	}

	private void startRetailers(final Attributes meta) {
		this.retailers.setName(meta.getValue("name"));
		//this.retailers.setDescription(meta.getValue("desc"));
		this.retailers.setLayer(meta.getValue("aggregation_layer"));
	}

	private void startRetailer(final Attributes meta) {
		int locId = Integer.parseInt(meta.getValue("id"));
		int cust_sqm = Integer.parseInt(meta.getValue("cust_sqm"));
		this.curr_retailer = this.retailers.createRetailer(new Id(locId), cust_sqm);
		if (this.curr_retailer == null) {
			log.warn("There is already a retailers object for location " + locId +
					". The retailers for loc_id=" + locId + ", cs_id=" + meta.getValue("cs_id") + " will be ignored.");
			return;
		}
		
	}

	private void startFacility(final Attributes meta) {
		if (this.curr_retailer != null) {
			String x = meta.getValue("x");
			String y = meta.getValue("y");
			if (x != null && y != null) {
				CoordI coord = new Coord(new Double(x),new Double(y));
				Facilities f = new Facilities(); //TODO Change the xml scheme or is it correct like that?
				this.curr_retailer.createFacility(f,meta.getValue("id"),coord,Integer.parseInt(meta.getValue("min_cust_sqm")));
			}
		}
	}


	/**
	 * Parses the specified retailers xml file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
