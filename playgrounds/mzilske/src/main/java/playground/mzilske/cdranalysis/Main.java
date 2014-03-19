package playground.mzilske.cdranalysis;

public class Main {
	
	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/");
		final RegimeResource uncongested = experiment.getRegime("uncongested");
		final RegimeResource congested = experiment.getRegime("congested");
		congested.getMultiRateRun("sense").rate("2");
	
//		uncongested.getMultiRateRun("regular").detourFactor();
		
//		uncongested.allRates();
//		congested.allRates();
//		uncongested.distances();
//		congested.distances();
//		uncongested.personKilometers();
//		congested.personKilometers();
//		experiment.personKilometers();
	}

}
