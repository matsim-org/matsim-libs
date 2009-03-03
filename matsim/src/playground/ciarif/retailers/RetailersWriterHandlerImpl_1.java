package playground.ciarif.retailers;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.interfaces.core.v01.Facility;

// RetailersWriterHandlerImplV1
public class RetailersWriterHandlerImpl_1 implements RetailersWriterHandler {
	// interface implementation
	//////////////////////////////////////////////////////////////////////
	// <retailers ... > ... </retailers>
	//////////////////////////////////////////////////////////////////////
	public void startRetailers(final Retailers_Old retailers, final BufferedWriter out) throws IOException {
		out.write("<retailers ");
		out.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
		out.write("xsi:noNamespaceSchemaLocation=\"http://matsim.org/files/dtd/retailers_v1.xsd\"\n");

		if (retailers.getName() != null) {
			out.write(" name=\"" + retailers.getName() + "\"");
		}
		if (retailers.getDescription() != null) {
			out.write(" desc=\"" + retailers.getDescription() + "\"");
		}
		out.write(" > \n");
	}
	public void endRetailers(final BufferedWriter out) throws IOException {
		out.write("</retailers>\n");
	}
	//////////////////////////////////////////////////////////////////////
	// <retailer ... > ... </retailer>
	//////////////////////////////////////////////////////////////////////
	public void startRetailer(final Retailer retailer, final BufferedWriter out) throws IOException {
		out.write("\t<retailer");
//		out.write(" loc_id=\"" + retailer.getRetailerId() + "\"");
		out.write(">\n");
	}
	public void endRetailer(final BufferedWriter out) throws IOException {
		out.write("\t</retailer>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <facility ... />
	//////////////////////////////////////////////////////////////////////
	public void startFacility(final Facility facility, final BufferedWriter out) throws IOException {
		out.write("\t\t<facility");
		out.write(" h=\"" + facility.getId() + "\"");
		out.write(" />\n");
	}
	public void endFacility(final BufferedWriter out) throws IOException {
	}
	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
