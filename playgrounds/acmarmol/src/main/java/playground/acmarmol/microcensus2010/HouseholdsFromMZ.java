package playground.acmarmol.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsFactoryImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.acmarmol.utils.CoordConverter;



public class HouseholdsFromMZ{

//////////////////////////////////////////////////////////////////////
// member variables
//////////////////////////////////////////////////////////////////////
	
	private final String inputfile;
	private ObjectAttributes household_attributes;
	
	
//////////////////////////////////////////////////////////////////////
// constructors
//////////////////////////////////////////////////////////////////////

	public HouseholdsFromMZ(final String inputfile) {
		super();
		this.inputfile = inputfile;
		this.household_attributes  = new ObjectAttributes();
	}	
	
//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////	
	
	
	public void createHouseholds() throws Exception{
	
		HouseholdsFactory householdFactory = new HouseholdsFactoryImpl();
		
		
		System.out.println("      parsing households...");	
		
		FileReader fr = new FileReader(this.inputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		//household number
		String hhnr = entries[0].trim();
		
		//household weight 
		String hh_weight = entries[1];
		household_attributes.putAttribute(hhnr, "weight", hh_weight);
		
		//household size
		String size = entries[76].trim();
		household_attributes.putAttribute(hhnr, "size", size);
		
		//holsehold income
		String income = entries[98].trim();
		household_attributes.putAttribute(hhnr, "income", income);
		
		
		// location coordinate (round to hectare) - WGS84 (5,6) & CH1903 (7,8)
		Coord location = new CoordImpl(entries[7].trim(),entries[8].trim());
		location.setX(Math.round(location.getX()/100.0)*100);
		location.setY(Math.round(location.getY()/100.0)*100);
		household_attributes.putAttribute(hhnr, "location_coord", location);
		
		//municipality BFS number
		String municipality =  entries[10].trim();
		household_attributes.putAttribute(hhnr, "municipality", municipality);
					
		}
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households parsed = " );
		System.out.println();	
		
	
	//
	
		
	}
	
	
//////////////////////////////////////////////////////////////////////
// run method
//////////////////////////////////////////////////////////////////////
		
	public void run() throws Exception{
		
		Map<Id,String> person_strings = new TreeMap<Id,String>();
		int id = Integer.MIN_VALUE;
		int prev_id = Integer.MIN_VALUE;
		Id prev_pid = new IdImpl(prev_id);
		int line_nr = 1;
		String person_string = "";
		
		createHouseholds();
		
		ObjectAttributesXmlWriter oaxmlw = new ObjectAttributesXmlWriter(household_attributes);
		oaxmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		oaxmlw.writeFile("./output/MicroCensus2010/householdsAttributes.xml");

	
	}
	
	
	
	
	
	
	
}