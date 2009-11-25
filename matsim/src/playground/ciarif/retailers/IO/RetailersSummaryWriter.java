package playground.ciarif.retailers.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;


public class RetailersSummaryWriter {
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private Retailers retailers;
	private int iter = 0;
			
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
			out.write("Fac_id\tfac_x\tfac_y\tLink_id\tIteration\n");
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

	public void write(Retailers retailers) {
		try {
			this.retailers = retailers;
			iter = iter+1;
			
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
}
