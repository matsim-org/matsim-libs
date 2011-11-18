package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * general remarks on the fields:
 * interview number used as a person id
 */
class MzPopulation {
	private static final Logger log =
		Logger.getLogger(MzPopulation.class);

	private final MzActivityChainsExtractor.Interval interval;
	private final Map<Id, MzPerson> persons = new HashMap<Id, MzPerson>();

	public MzPopulation(final MzActivityChainsExtractor.Interval interval) {
		this.interval = interval;
	}

	public Scenario getScenario() {
		ScenarioImpl scen = (ScenarioImpl) ScenarioUtils.createScenario(
					ConfigUtils.createConfig());

		Population population = scen.getPopulation();

		Person matsimPerson;
		for (MzPerson person : persons.values()) {
			if (!interval.contains( person.getDayOfWeek() )) continue;
			try {
				matsimPerson = person.getPerson();
			} catch (UnhandledMzRecordException e) {
				// entry is inconsistent: inform user and pass to the next
				// log.info( "got unconsistent entry: "+e.getMessage() );
				continue;
			}
			population.addPerson( matsimPerson );
		}

		MzPerson.printStatistcs();
		return scen;
	}

	public void addPerson(final MzPerson person) {
		if (persons.put( person.getId() , person ) != null) {
			throw new RuntimeException( "same person created twice" );
		}
	}

	public void addWeg(final MzWeg weg) {
		MzPerson enclosingPerson = persons.get( weg.getPersonId() );

		if (enclosingPerson == null) throw new RuntimeException( "trying to add a weg before the person" );

		enclosingPerson.addWeg( weg );
	}

	public void addEtappe(final MzEtappe etappe) {
		MzPerson enclosingPerson = persons.get( etappe.getPersonId() );

		if (enclosingPerson == null) throw new RuntimeException( "trying to add a etappe before the person" );

		enclosingPerson.addEtappe( etappe );
	}
}