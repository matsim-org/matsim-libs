package playground.mzilske.ant2014;

public class Main {

	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/");
		final RegimeResource uncongested = experiment.getRegime("uncongested");
		final RegimeResource congested = experiment.getRegime("congested");
		//congested.getMultiRateRun("sense").rate("2");

//        Scenario base = uncongested.getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
//        Scenario experiencedPlansAndNetwork = uncongested.getMultiRateRun("regular").getRateRun("actevents").getLastIteration().getExperiencedPlansAndNetwork();
//        IdImpl id = new IdImpl("40258");
//        Person person = experiencedPlansAndNetwork.getPopulation().getPersons().get(id);
//        Person basePerson = base.getPopulation().getPersons().get(id);
//        double distance = PowerPlans.distance(experiencedPlansAndNetwork.getNetwork(), person.getSelectedPlan());
//        double baseDistance = PowerPlans.distance(base.getNetwork(), basePerson.getSelectedPlan());
//        System.out.println(distance);
//        System.out.println(baseDistance);


		congested.getMultiRateRun("regular").distances2();
//		uncongested.allRates();
//		congested.allRates();
//		uncongested.distances();
//		congested.distances();
//		uncongested.getMultiRateRun("regular").personKilometers();
//		congested.getMultiRateRun("regular").personKilometers();
//		experiment.personKilometers();

//        uncongested.durationsSimulated();

	}

}
