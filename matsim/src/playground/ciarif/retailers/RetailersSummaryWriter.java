package playground.ciarif.retailers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.facilities.Facility;
import org.matsim.population.Plan;

import playground.ciarif.models.subtours.PersonSubtour;
import playground.ciarif.models.subtours.Subtour;

public class RetailersSummaryWriter {
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private Retailers retailers;
			
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public RetailersSummaryWriter(String outfile, Retailers retailers) {
		super();
		this.retailers = retailers;
		try {
			fw = new FileWriter(outfile);
			System.out.println(outfile);
			out = new BufferedWriter(fw);
			out.write("Fac_id \t fac_x \t fac_y\t Link_id\t Link_x \t Link_y\n");
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

	private void writeRetailers (Retailers retailers) {
		try {
					
			this.retailers = retailers;
					System.out.println("retailers "+ this.retailers.getRetailers().values());
					Iterator<Facility>	ret_iter = this.retailers.getRetailers().values().iterator();
					
					while (ret_iter.hasNext()) {
					Facility f = ret_iter.next();
					out.write(f.getId()+"\t");
					out.write(f.getActivity("shop").getLocation().getCenter().getX()+ "\t");
					out.write(f.getActivity("shop").getLocation().getCenter().getY()+"\t");
					out.write(f.getLink().getId()+"\t");
					out.write(f.getLink().getCenter().getX()+ "\t");
					out.write(f.getLink().getCenter().getY()+"\n");
					}
										
					out.flush();
				
		} 
		
		catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		
	}
	
	
	public void write(Retailers retailers) {
		try {
			//Iterator<Facility> ret_it = this.retailers.getRetailers().values().iterator();
//			while (ps_it.hasNext()) {
//				PersonSubtour personSubtour = ps_it.next();
			writeRetailers (retailers);
			out.flush();
			//this.out.close();
		} 
		
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}

	public void run(Plan plan) {
		
	}
}
