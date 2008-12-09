package playground.ciarif.retailers;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

// RetailerReaderV1
public class RetailersReader extends MatsimXmlParser {
	
	private final static String RETAILERS = "retailers";
	private final static String RETAILER = "retailer";
	private final static String FACILITY = "facility";

	private final Retailers_Old retailers;
	private Retailer curr_retailer = null;

	private static final Logger log = Logger.getLogger(RetailersReader.class);

	public RetailersReader (final Retailers_Old retailers) {
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
	}

	private void startRetailer(final Attributes meta) {
		int locId = Integer.parseInt(meta.getValue("id"));
		int cust_sqm = Integer.parseInt(meta.getValue("cust_sqm"));
		//this.curr_retailer = this.retailers.createRetailer(new IdImpl(locId), cust_sqm);
		if (this.curr_retailer == null) {
			log.warn("There is already a retailers object for location " + locId +
					". The retailers for loc_id=" + locId + ", cs_id=" + meta.getValue("cs_id") + " will be ignored.");
			return;
		}
		
	}

	private void startFacility(final Attributes meta) {
		if (this.curr_retailer != null) {
			String fac_id = meta.getValue("id");
			String min_cust_sqm = meta.getValue("min_cust_sqm");
			boolean ok = this.curr_retailer.setFacility(new IdImpl(fac_id), Double.parseDouble(min_cust_sqm));
			if (!ok) { Gbl.errorMsg("Fac id=" + fac_id + " does not exist in the facilities DB!"); }
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
