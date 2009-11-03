package playground.balmermi.datapuls.modules;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;

public class PopulationWriteTable {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PopulationWriteTable.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PopulationWriteTable() {
		log.info("init " + this.getClass().getName() + " module...");
		log.info("done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////
	
	public void run(PopulationImpl population, final String outdir) {
		log.info("running " + this.getClass().getName() + " module...");
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outdir+"/population.txt.gz");
			out.write("ID\tlicense\tcaravail\ttravelcard\thomefid\n");
			out.flush();
			for (PersonImpl p : population.getPersons().values()) {
				out.write(p.getId().toString()+"\t");
				if (p.hasLicense()) { out.write("yes\t"); } else { out.write("no\t"); }
				out.write(p.getCarAvail()+"\t");
				if (p.getTravelcards() != null) { out.write("yes\t"); } else { out.write("no\t"); }
				out.write(p.getSelectedPlan().getFirstActivity().getFacility().getId().toString()+"\n");
			}
			out.close();
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		log.info("done.");
	}
}
