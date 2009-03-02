package playground.ciarif.retailers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.facilities.Facility;

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
			
			//out.write("ITERATION N." + iter +"\n");
			for (Retailer r : retailers.getRetailers().values()) {
				System.out.println("retailer = "+ r.getId());
				for (Facility f : r.getFacilities().values()) {
					out.write(f.getId()+"\t");
					//out.write(f.getCenter().getX()+ "\t");
					//out.write(f.getCenter().getY()+"\t");
					out.write(f.getLink().getId()+"\t");
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
