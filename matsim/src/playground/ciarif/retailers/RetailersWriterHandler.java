package playground.ciarif.retailers;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.interfaces.core.v01.Facility;
import org.matsim.writer.WriterHandler;

interface RetailersWriterHandler extends WriterHandler {
	//////////////////////////////////////////////////////////////////////
	// <Retailers ... > ... </retailers>
	//////////////////////////////////////////////////////////////////////
	public void startRetailers(final Retailers_Old counts, final BufferedWriter out) throws IOException;
	public void endRetailers(final BufferedWriter out) throws IOException;
	//////////////////////////////////////////////////////////////////////
	// <Retailer ... > ... </retailer>
	//////////////////////////////////////////////////////////////////////
	public void startRetailer(final Retailer count, final BufferedWriter out) throws IOException;
	public void endRetailer(final BufferedWriter out) throws IOException;
	//////////////////////////////////////////////////////////////////////
	// <facility ... />
	//////////////////////////////////////////////////////////////////////
	public void startFacility(final Facility volume, final BufferedWriter out) throws IOException;
	public void endFacility(final BufferedWriter out) throws IOException;

}
