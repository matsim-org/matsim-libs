package playground.ciarif.retailers.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;
import playground.ciarif.retailers.utils.CountFacilityCustomers;


public class RetailersSummaryWriter {
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private Retailers retailers;
	//private int iter = 0;
			
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public RetailersSummaryWriter(String outfile) {
		super();
		//this.retailers = retailers;
		try {
			fw = new FileWriter(outfile);
			System.out.println(outfile);
			out = new BufferedWriter(fw);
			out.write("Ret_Id\tFac_id\tfac_x\tfac_y\tLink_id\tNr_Cust\tIteration\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void write(Retailers retailers, int iter) {
		try {
			this.retailers = retailers;
			
			out.write("ITERATION N." + iter +"\n");
			for (Retailer r : this.retailers.getRetailers().values()) {
				for (ActivityFacility f : r.getFacilities().values()) {
					System.out.println("fac Id = "+ f.getId());
					out.write(f.getId()+"\t");
					out.write(f.getCoord().getX()+ "\t");
					out.write(f.getCoord().getY()+"\t");
//					BasicLink l = (BasicLink)f.getDownMapping().values().iterator().next();
//					out.write(l.getId()+"\t");
					out.write(f.getLinkId()+"\t");
					out.write(iter +"\n");
				}
			}
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(Retailer retailer, int iter, CountFacilityCustomers cfc) {
		try {
			//this.retailers = retailers;
			
			//out.write("ITERATION N." + iter +"\n");
			//for (Retailer r : this.retailers.getRetailers().values()) {
				for (ActivityFacilityImpl f : retailer.getFacilities().values()) {
					System.out.println("fac Id = "+ f.getId());
					out.write(retailer.getId() + "\t");
					out.write(f.getId()+"\t");
					out.write(f.getCoord().getX()+ "\t");
					out.write(f.getCoord().getY()+"\t");
//					BasicLink l = (BasicLink)f.getDownMapping().values().iterator().next();
//					out.write(l.getId()+"\t");
					out.write(f.getLinkId()+"\t");
					out.write(cfc.countCustomers(f)+"\t");
					out.write(iter +"\n");
				}
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
