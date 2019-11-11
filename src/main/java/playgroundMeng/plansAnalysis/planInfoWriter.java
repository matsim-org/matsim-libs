package playgroundMeng.plansAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class planInfoWriter {
	public static void main(String[] args) throws IOException {
		
		//String inputPlanFile = "C:/Users/VW3RCOM/Desktop/vw280_100pct.output_plans_selectedOnly.xml.gz";
		
		String outputPlan = "C:/Users/VW3RCOM/Desktop/output_plans.xml.gz";
		String ZeroOutputPlan = "C:/Users/VW3RCOM/Desktop/0.plans.xml.gz";
		String inputPlan = "C:/Users/VW3RCOM/Desktop/vw280_100pct.output_plans_selectedOnly.xml.gz";
		
		String outputString = "C:/Users/VW3RCOM/Desktop/outputWriter.xml";
		String zeroOutputString = "C:/Users/VW3RCOM/Desktop/0.OutputWriter.xml";
		String InputString = "C:/Users/VW3RCOM/Desktop/InputWriter.xml";
		
		
		
		Population population = PlansFileComparator.readPlansFile(outputPlan);
		writer(population, outputString);
	}
	
	static void writer(Population population , String s) throws IOException {
		File file = new File(s);
		Writer writer = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(writer);
		
		for(Person person: population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			bufferedWriter.write(person.getId() + "+" + plan.getScore() + "+" + plan.getPlanElements().toString());
			bufferedWriter.newLine();
		}
		bufferedWriter.close();
	
	}
}
