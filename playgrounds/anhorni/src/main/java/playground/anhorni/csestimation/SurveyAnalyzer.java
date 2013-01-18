package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;

public class SurveyAnalyzer {
	private final static Logger log = Logger.getLogger(SurveyAnalyzer.class);	
	private TreeMap<Id, EstimationPerson> population;
	private String outdir;
	
	public SurveyAnalyzer(TreeMap<Id, EstimationPerson> population, String outdir) {
		this.population = population;
		this.outdir = outdir;
	}
	
	public SurveyAnalyzer(Population population, String outdir) {
		this.population = new TreeMap<Id, EstimationPerson>();
		for (Person p:population.getPersons().values()) {
			this.population.put(p.getId(), (EstimationPerson)p);
		}
		this.outdir = outdir;
	} 
	
	public void analyze() {
		log.info("Analyzing " + this.population.size() + " persons");
//		interval, maxVal
		Bins ageBins = new Bins(10, 100, "age");
		Bins incomeBins = new Bins(2000, 18000, "income");
		
		for (Person p:this.population.values()) {
			if (((PersonImpl)p).getAge() > 0) {
				ageBins.addVal(((PersonImpl)p).getAge(), 1.0);
			}
			
			if (((EstimationPerson)p).getHhIncome() > 0.0) {
				incomeBins.addVal(((EstimationPerson)p).getHhIncome(), 1.0);
			}
		}
		ageBins.plotBinnedDistribution(outdir, "age bins", " year");
		incomeBins.plotBinnedDistribution(outdir, "income bins", " SFr.");
	}
}
