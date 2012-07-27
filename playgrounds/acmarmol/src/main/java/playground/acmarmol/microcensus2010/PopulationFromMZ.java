package playground.acmarmol.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.households.HouseholdsImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleUtils;

public class PopulationFromMZ {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////	
	
	private final String populationInputfile;
	private final String wegeInputfile;
	private ObjectAttributes population_attributes;
	
//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////	
	
	public PopulationFromMZ(final String peopleInputfile, final String wegeInputfile) {
		super();
		this.populationInputfile = peopleInputfile;
		this.wegeInputfile = wegeInputfile;
		this.population_attributes  = new ObjectAttributes();
	}	
	
	
	
	
//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////	
	
	public void createPopulation(Population population) throws Exception{
		
		System.out.println("      parsing population from " + this.populationInputfile);	
		
		FileReader fr = new FileReader(this.populationInputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		//household number
		String hhnr = entries[0].trim();
		
		//person number (zielpnr)
		String zielpnr = entries[1].trim();
		
		//person weight 
		String person_weight = entries[2];
		population_attributes.putAttribute(hhnr.concat(zielpnr), "person weight", person_weight);
		
		//person age 
		String age = entries[188];
		population_attributes.putAttribute(hhnr.concat(zielpnr), "age", age);
		
		//person gender
		String gender = entries[190];
		if(gender.equals("1")){gender = "male";}
		else if(gender.equals("2")){gender = "female";}
		else Gbl.errorMsg("This should never happen!  Gender: " + gender+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "gender", gender);
		
		//HalbTax
		String halbtax = entries[48];
		if(halbtax.equals("1")){halbtax = "yes";}
		else if(halbtax.equals("2")){halbtax = "no";}
		else if(halbtax.equals("-98")){halbtax = "noAnswer";}
		else if(halbtax.equals("-97")){halbtax = "notKnown";}
		else Gbl.errorMsg("This should never happen!  Halbtax: " + halbtax+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "Halbtax Abonnament", halbtax);
		
		//GA first class
		String gaFirstClass = entries[49];
		if(gaFirstClass.equals("1")){gaFirstClass = "yes";}
		else if(gaFirstClass.equals("2")){gaFirstClass = "no";}
		else if(gaFirstClass.equals("-98")){gaFirstClass = "noAnswer";}
		else if(gaFirstClass.equals("-97")){gaFirstClass = "notKnown";}
		else Gbl.errorMsg("This should never happen!  GA First Class: " + gaFirstClass+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "GA First Class Abbonement", gaFirstClass);
		
		//GA second class
		String gaSecondClass = entries[50];
		if(gaSecondClass.equals("1")){gaSecondClass = "yes";}
		else if(gaSecondClass.equals("2")){gaSecondClass = "no";}
		else if(gaSecondClass.equals("-98")){gaSecondClass = "noAnswer";}
		else if(gaSecondClass.equals("-97")){gaSecondClass = "notKnown";}
		else Gbl.errorMsg("This should never happen!  GA Second Class: " + gaSecondClass+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "GA Second Class Abbonement", gaSecondClass);
		
		
		
		//creating matsim person
		PersonImpl person = new PersonImpl(new IdImpl(hhnr.concat(zielpnr)));
		person.setAge(Integer.parseInt(age));
		//person.setLicence(licence);
		person.setSex(gender);
		population.addPerson(person);
		}
	
	
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # persons parsed = "  + population.getPersons().size());
		System.out.println();
	
	
	}
	
	public void createTrips() throws Exception{
		
		System.out.println("      parsing weges from " + this.wegeInputfile);	
		
		FileReader fr = new FileReader(this.wegeInputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
	
		String[] entries = curr_line.split("\t", -1);
			
		//household number
		String hhnr = entries[0].trim();
		
		//wege number
		String wegnr = entries[3].trim();	
		
		//mode
		String mode = entries[80].trim();
		//if(mode.equals("1")){mode = "plane";}
		//else if(mode.equals("2")){mode = "train";}
		//else if(mode.equals("3")){mode = "postauto";}
		//else if(mode.equals("4")){mode = "ship";}
		//else if(mode.equals("5")){mode = "tram";}
		//else if(mode.equals("6")){mode = "bus";}
		//else if(mode.equals("7")){mode = "sonstigerOeV";}
		//else if(mode.equals("-97")){mode = "notKnown";}
		//else if(mode.equals("-99")){mode = "FrageNurBeiAuto";}
				
		}
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # weges parsed = "  );
		System.out.println();
		
	}
	
	
//////////////////////////////////////////////////////////////////////
//run method
//////////////////////////////////////////////////////////////////////

	public void run(Population population) throws Exception{
		
	createPopulation(population);	
	createTrips();
		
	
	System.out.println("############################################ \n " +
			   		   "Writing complete population xml file \n" +
					   "############################################");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/completePopulation.xml");
	System.out.println("  done.");
	
	System.out.println("############################################ \n " +
					   "Writing complete population's attributes xml file \n" +
			   		   "############################################");	
	new ObjectAttributesXmlWriter(population_attributes).writeFile("./output/MicroCensus2010/completePopulationAttributes.xml");
	System.out.println("  done.");

	
	
	
	
	
	}	
	
}
