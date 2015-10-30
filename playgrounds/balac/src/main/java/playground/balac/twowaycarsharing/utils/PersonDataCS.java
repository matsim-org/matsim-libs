package playground.balac.twowaycarsharing.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class PersonDataCS {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[0]);	
		
		final BufferedWriter outLinkrb = IOUtils.getBufferedWriter(args[2]);
		final BufferedWriter outLinkff = IOUtils.getBufferedWriter(args[3]);
		
		for(Person p: scenario.getPopulation().getPersons().values()){
			boolean rb = false;
			boolean ff = false;
			Person pImpl = p;
			Plan plan = p.getSelectedPlan();
			
			for (PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Leg){
					
					if (((Leg) pe).getMode().equals("cs_fix_gas") && !rb) {
						outLinkrb.write(p.getId().toString() + " ");
						outLinkrb.write(Integer.toString(PersonUtils.getAge(pImpl)) + " ");
						outLinkrb.write(PersonUtils.getSex(pImpl) + " ");
						outLinkrb.write(PersonUtils.getCarAvail(pImpl));
						outLinkrb.newLine();
						rb = true;
						
					}
					else if (((Leg) pe).getMode().equals("cs_flex_gas") && !ff) {
						outLinkff.write(p.getId().toString() + " ");
						outLinkff.write(Integer.toString(PersonUtils.getAge(pImpl)) + " ");
						outLinkff.write(PersonUtils.getSex(pImpl) + " ");
						outLinkff.write(PersonUtils.getCarAvail(pImpl));
						outLinkff.newLine();
						ff = true;
					}
				}
			}
		}
		outLinkrb.flush();
		outLinkrb.close();
		outLinkff.flush();
		outLinkff.close();

	}

}
