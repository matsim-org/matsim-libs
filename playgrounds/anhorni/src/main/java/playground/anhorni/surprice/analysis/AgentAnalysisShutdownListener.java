package playground.anhorni.surprice.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class AgentAnalysisShutdownListener implements ShutdownListener {
	
	private String day;
	private String outPath;
	
	public AgentAnalysisShutdownListener(String day, String outPath) {
		this.day = day;
		this.outPath = outPath;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
        Population population = event.getServices().getScenario().getPopulation();
		
		ObjectAttributes oa = new ObjectAttributes();			
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(oa);
						
		for (Person person : population.getPersons().values()) {
			if (person.getCustomAttributes().get(day + ".tollScore") != null) {
				oa.putAttribute(person.getId().toString(), day + ".tollScore", person.getCustomAttributes().get(day + ".tollScore"));
			}
			else {
				oa.putAttribute(person.getId().toString(), day + ".tollScore", 0.0);
			}
			oa.putAttribute(person.getId().toString(), day + ".actScore", person.getCustomAttributes().get(day + ".actScore"));
			oa.putAttribute(person.getId().toString(), day + ".legScore", person.getCustomAttributes().get(day + ".legScore"));	
			oa.putAttribute(person.getId().toString(), day + ".legMonetaryCosts", person.getCustomAttributes().get(day + ".legMonetaryCosts"));	
			oa.putAttribute(person.getId().toString(), day + ".legScoreLag", person.getCustomAttributes().get(day + ".legScoreLag"));
		}
		attributesWriter.writeFile(outPath + "/" + day + ".perAgent.txt");
	}
}
