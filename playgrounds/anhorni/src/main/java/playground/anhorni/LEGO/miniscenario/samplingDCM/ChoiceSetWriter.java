package playground.anhorni.LEGO.miniscenario.samplingDCM;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.geometry.CoordImpl;


public class ChoiceSetWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriter.class);
	private ScenarioImpl scenario;
	private String outPath;
	
	
	public ChoiceSetWriter() {	
	}
	
	public ChoiceSetWriter(Scenario scenario, String outPath) {
		this.scenario = (ScenarioImpl) scenario;
		this.outPath = outPath;
	}
	
	public static void main (String args []){
		ChoiceSetWriter csWriter = new ChoiceSetWriter();	
		csWriter.init(args[0], args[1], args[2], args[3]);
		csWriter.write();
	}
	
	private void init(String plansFilePath, String facilitiesFilePath, String networkFilePath, String outPath) {
		this.outPath = outPath;
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(this.scenario).readFile(networkFilePath);		
		new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesFilePath);
		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}
		
	public void write()  {	
		log.info("Writting choice sets");
		String outfile = this.outPath + "universalCS.dat";
		String header = this.getHeader();
		
		int personCnt = 0;
		int nextMsg = 2;
		
		try {								
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();	
			for (Person p: this.scenario.getPopulation().getPersons().values()) {
				personCnt++;
				if (personCnt % nextMsg == 0) {
					nextMsg *= 2;
					log.info("Person: " + personCnt); 
				}
				
				double wp = 1.0;
				String choice = "";
				String outLine = p.getId().toString() + "\t" + wp;
				String alternatives="\t";
				
				int index = 0;
				for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
					ActivityImpl actHome = (ActivityImpl) p.getSelectedPlan().getPlanElements().get(0);
					
					double distance = 2 * ((CoordImpl)actHome.getCoord()).calcDistance(facility.getCoord());
					alternatives += facility.getId() + "\t" + "1" + "\t" + distance + "\t";
					
					ActivityImpl shopAct = (ActivityImpl) p.getSelectedPlan().getPlanElements().get(2);
					if (facility.getId().compareTo(shopAct.getFacilityId()) == 0) {
						choice = Integer.toString(index);
					}
					index++;
				}
				out.write(outLine + "\t" + choice + alternatives);
				out.newLine();
				out.flush();
			}
			out.flush();			
			out.flush();
			out.close();
			log.info("Choice sets written");
		}catch (final IOException e) {
					Gbl.errorMsg(e);
		}
	}
							
	private String getHeader() {
		String header="Id\tWP\tChoice\t" ;

		for (int i = 0; i < this.scenario.getActivityFacilities().getFacilities().size(); i++) {
			header += "SH" + i + "_Shop_id\t" +
					"SH" + i + "_AV\t" +
					"SH" + i + "_distance\t";
		}	
		return header;
	}
}
