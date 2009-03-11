package playground.ciarif.retailers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;

public class MakeATableFromXMLFacilities {
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private Object facilities;
			
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MakeATableFromXMLFacilities(String outfile) {
		super();
		//this.retailers = retailers;
		try {
			fw = new FileWriter(outfile);
			System.out.println(outfile);
			out = new BufferedWriter(fw);
			out.write("Fac_id\tfac_x\tfac_y\tLink_id\tcapacity\n");
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

	public void write(Facilities facilities) {
		try {
			this.facilities = facilities;
			
			for (Facility f : facilities.getFacilities().values()) {
				if (f.getActivityOption("shop")!=null){
					out.write(f.getId()+ "\t");
					out.write(f.getCenter().getX()+ "\t");
					out.write(f.getCenter().getY()+"\t");
					out.write(f.getLink().getId()+"\t");
					out.write(f.getActivityOption("shop").getCapacity()+"\n");
					}
				}
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
