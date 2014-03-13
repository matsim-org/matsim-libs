package playground.mzilske.cdranalysis;

public class Main {
	
	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/");
		final MultiRateRunResource uncongested = experiment.getRegime("uncongested");
		final MultiRateRunResource congested = experiment.getRegime("congested");
//		uncongested.allRates();
//		congested.allRates();
//		uncongested.distances();
//		congested.distances();
//		uncongested.personKilometers();
//		congested.personKilometers();
		experiment.personKilometers();
	}

}
