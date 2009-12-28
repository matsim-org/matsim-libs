package playground.ciarif.retailers.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;


public class MakeATableFromXMLFacilities {
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	//private Object facilities;
			
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

	public void write(Map<Id,? extends ActivityFacility> facilities) {
		try {
			//this.facilities = facilities;
			
			for (ActivityFacility f : facilities.values()) {
				if (f.getActivityOptions().get("shop")!=null){
					out.write(f.getId()+ "\t");
					out.write(f.getCoord().getX()+ "\t");
					out.write(f.getCoord().getY()+"\t");
					out.write(f.getLinkId()+"\t");
					out.write(f.getActivityOptions().get("shop").getCapacity()+"\n");
					}
				}
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
