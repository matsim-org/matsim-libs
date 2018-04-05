package ft.cemdap4H.planspreprocessing;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class AddOutsideCommuters {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public void run(String inputpopulationFile, String oldInputpopulationFil, String outPutpopulationFile) {
		Set<String> ids = new HashSet<>();
		ids.add("BR_WB");
		ids.add("BR_BS");

		ids.add("AS_WB");
		ids.add("AS_BS");
		
		ids.add("SG_WB");
		ids.add("SG_BS");
		
		ids.add("HH_WB");
		ids.add("HH_BS");
		
		ids.add("RH_WB");
		ids.add("RH_BS");
	
		ids.add("SL_WB");
		ids.add("SL_BS");
		
		ids.add("SD_WB");
		ids.add("SD_BS");
		
		ids.add("MB_WB");
		ids.add("MB_BS");
		
		ids.add("HZ_WB");
		ids.add("HZ_BS");
		
		ids.add("OR_WB");
		ids.add("OR_BS");
		
		StreamingPopulationWriter spw = new StreamingPopulationWriter();
		spw.startStreaming(outPutpopulationFile);
		StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		spr.addAlgorithm(spw);
		spr.readFile(inputpopulationFile);
		StreamingPopulationReader spr2 = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		MutableInt i = new MutableInt();
		spr2.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				String idb = person.getId().toString().substring(0, 5);
				if (ids.contains(idb)) {
					for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
						if (pe instanceof Activity) {
							if (((Activity) pe).getType().equals("private")){
								((Activity) pe).setType("other");
							}
							if (((Activity) pe).getType().startsWith("home")){
								((Activity) pe).setType("home");
							}
							if (((Activity) pe).getType().startsWith("work")){
								((Activity) pe).setType("work");
							}
						}
					}
				i.increment();	
				spw.writePerson(person);
				}
			}
		});
		
		spr2.readFile(oldInputpopulationFil);
		
		spw.closeStreaming();
		Logger.getLogger(getClass()).info("Added "+i.intValue()+" persons.");
	}
	

}
