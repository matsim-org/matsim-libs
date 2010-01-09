package playground.balmermi.datapuls.modules;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;

public class PopulationWriteTable {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PopulationWriteTable.class);
	private final Set<String> actOptTypes = new TreeSet<String>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PopulationWriteTable(final ActivityFacilitiesImpl facilities) {
		log.info("init " + this.getClass().getName() + " module...");
		log.info("  extract actOptTypes...");
		for (ActivityFacilityImpl facility : facilities.getFacilities().values()) {
			for (ActivityOptionImpl actOpt : facility.getActivityOptions().values()) {
				if (!actOpt.getType().startsWith("B")) {
					actOptTypes.add(actOpt.getType());
				}
			}
		}
		log.info("  => "+actOptTypes.size()+" actOpts found.");
		log.info("  done.");
		log.info("done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////
	
	public void run(Population population, final String outdir) {
		log.info("running " + this.getClass().getName() + " module...");
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outdir+"/population.txt.gz");
			out.write("ID\tlicense\tcaravail\ttravelcard");
			for (String type : actOptTypes) { out.write("\t"+type+"Fid"); }
			out.write("\n");
			out.flush();
			for (Person pp : population.getPersons().values()) {
				PersonImpl p = (PersonImpl) pp;
				out.write(p.getId().toString()+"\t");
				if (p.hasLicense()) { out.write("yes\t"); } else { out.write("no\t"); }
				out.write(p.getCarAvail()+"\t");
				if (p.getTravelcards() != null) { out.write("yes"); } else { out.write("no"); }

				Map<String,Id> facs = new TreeMap<String, Id>();
				for (String type : actOptTypes) { facs.put(type,null); }
				List<PlanElement> pe = p.getSelectedPlan().getPlanElements();
				for (int i=0; i<pe.size(); i=i+2) {
					ActivityImpl a = (ActivityImpl)pe.get(i);
					facs.put(a.getType(),a.getFacilityId());
				}
				for (String type : facs.keySet()) { out.write("\t"+facs.get(type)); }
				out.write("\n");
			}
			out.close();
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		log.info("done.");
	}
}
