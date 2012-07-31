package playground.acmarmol.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;


public class MZHouseholdPersonParser {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	
	private static final String NOT_KNOWN = "not known";
	private static final String UNSPECIFIED = "unspecified";

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZHouseholdPersonParser(Households households, ObjectAttributes householdAttributes) {
	super();
	this.households = households;
	this.householdAttributes = householdAttributes;
	}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////
	
	public void parse(String haushaltspersonenFile) throws Exception{
		
		FileReader fr = new FileReader(haushaltspersonenFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		int nr_persons = 0; 
		
		while ((curr_line = br.readLine()) != null) {
			nr_persons++;	
			String[] entries = curr_line.split("\t", -1);
			
			//household number
			String hhnr = entries[0].trim();
			
			//household person number (hpnr)
			String hpnr = entries[2].trim();
			
			
			//filling person data into matsim households
			IdImpl hhid = new IdImpl(hhnr);
			if(!this.households.getHouseholds().containsKey(hhid)){
				Gbl.errorMsg("This should never happen!  Household hhnr: " + hhnr+ " doesn't exist");
			}		
			this.households.getHouseholds().get(hhid).getMemberIds().add(new IdImpl(hhnr.concat(hpnr)));  // id = hhnr + hpnr??
		}
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households people parsed = "  + nr_persons);
		System.out.println();
		
	}
	
}
