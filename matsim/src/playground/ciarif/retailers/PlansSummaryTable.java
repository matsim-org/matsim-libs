package playground.ciarif.retailers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PlansSummaryTable implements PlanAlgorithm {
	
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansSummaryTable(String outfile) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
			out.write("pid\tact1\tlink1\tfacId1\t...\tactn\tlinkn\tfacIdn\tIteration\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	
	public void run(Person person, int iter) {
		try {
			Plan plan = person.getSelectedPlan();
			out.write(person.getId() + "\t");
			//out.write(plan.getScore() + "\t");
			for (int i=1; i<plan.getActsLegs().size()-2; i=i+2) {
				//Leg l = (Leg)plan.getActsLegs().get(i);
				Act a = (Act)plan.getActsLegs().get(i+1);
				//Link arr_link = a.getLink();
				out.write(a.getType() + "\t");
				out.write(a.getLinkId() + "\t");
				out.write(a.getFacilityId() + "\t");
				
				
			}
			out.write(iter +"\n");
			//out.write("\n");
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
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
	public void run(Plan plan) {
		
		
	}
}
