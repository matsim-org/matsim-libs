package playground.ciarif.retailers.IO;


import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class RetailersWriter extends MatsimXmlWriter implements MatsimFileWriter {

	public void writeFile(final String filename) {
		// TODO Auto-generated method stub
		
	}
//	private RetailersWriterHandler handler = null;
//	private final Retailers retailers;
//
////	public RetailersWriter(final Retailers retailers) {
////		this(retailers,
////				Gbl.getConfig().retailers().getOutputFile());
////	}
//
//	public RetailersWriter(final Retailers retailers, final String filename) {
//		this.retailers = retailers;
//		this.outfile = filename;
//		this.dtd = null;
//
//		// use the newest writer-version by default
//		this.handler = new RetailersWriterHandlerImpl_1();
//	}
	
//	@Override
//	public final void write() {
//		try {
//
//			this.out = IOUtils.getBufferedWriter(this.outfile);
//
//			// write custom header
//			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
//			this.out.flush();
//
//			this.handler.startRetailers(this.retailers, this.out);
//			this.handler.writeSeparator(this.out);
//
//			//retailers iterator
//			Iterator<Retailer> r_it = this.retailers.getRetailers().values().iterator();
//			while (r_it.hasNext()) {
//				Retailer r = r_it.next();
//				this.handler.startRetailer(r,this.out);
//
//				// volume iterator
//				Iterator<Facility> fac_it = r.getFacilities().values().iterator();
//				while (fac_it.hasNext()) {
//					Facility f = fac_it.next();
//					this.handler.startFacility(f, this.out);
//					this.handler.endFacility(this.out);
//				}
//				this.handler.endRetailer(this.out);
//				this.handler.writeSeparator(this.out);
//				this.out.flush();
//			}
//			this.handler.endRetailers(this.out);
//			this.out.close();
//		}
//		catch (IOException e) {
//			Gbl.errorMsg(e);
//		}
//	}

//	public final void writeFile(final String filename) {
//		this.outfile = filename;
//		write();
//	}
//
//	@Override
//	public final String toString() {
//		return super.toString();
//	}

}
