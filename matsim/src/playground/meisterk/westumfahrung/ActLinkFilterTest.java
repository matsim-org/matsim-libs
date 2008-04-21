package playground.meisterk.westumfahrung;

import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;

public class ActLinkFilterTest extends MatsimTestCase {

	/**
	 * @author komeiste
	 * @throws Exception 
	 */
	public void testActLinkFilter() throws Exception {

		final String GENERAL_HOME_ACT_TYPE = "h";
		final String NINETEEN_HOUR_HOME_ACT_TYPE = "h19";

		// fixture: 2 links and 2 persons
		NetworkLayer network = new NetworkLayer();
		network.createNode("1", "100.0", "100.0", "unknown");
		network.createNode("2", "200.0", "200.0", "unknown");
		network.createNode("3", "300.0", "300.0", "unknown");

		network.createLink("1", "1", "2", "1000.0", "20", "200", "2", "1", "unknown");
		network.createLink("2", "2", "3", "1000.0", "20", "200", "2", "2", "unknown");

		TreeMap<String, Person> persons = new TreeMap<String, Person>();

		String linkId = null;
		String actType = null;
		for (String personId : new String[]{"1", "2"}) {
			Person person = new Person(personId, "f", "30", "yes", "yes", "yes");
			Plan plan = person.createPlan("0.0", "yes");
			if (personId.equals("1")) {
				linkId = "1";
				actType = GENERAL_HOME_ACT_TYPE;
			} else if (personId.equals("2")) {
				linkId = "2";
				actType = NINETEEN_HOUR_HOME_ACT_TYPE;
			}
			plan.createAct(
					actType, 
					10.0, 
					10.0, 
					network.getLink(linkId), 
					Time.parseTime("08:00:00"), 
					Time.parseTime("10:00:00"), 
					Time.parseTime("02:00:00"), 
					false);
			persons.put(personId, person);
		}


		ActLinkFilter allHomeFilter = new ActLinkFilter(".*" + GENERAL_HOME_ACT_TYPE + ".*", null);
		allHomeFilter.addLink(new IdImpl("1"));
		assertTrue(allHomeFilter.judge(persons.get("1").getPlans().get(0)));
		assertFalse(allHomeFilter.judge(persons.get("2").getPlans().get(0)));
		allHomeFilter.addLink(new IdImpl("2"));
		assertTrue(allHomeFilter.judge(persons.get("1").getPlans().get(0)));
		assertTrue(allHomeFilter.judge(persons.get("2").getPlans().get(0)));

		ActLinkFilter home19Filter = new ActLinkFilter(NINETEEN_HOUR_HOME_ACT_TYPE, null);
		home19Filter.addLink(new IdImpl("1"));
		assertFalse(home19Filter.judge(persons.get("1").getPlans().get(0)));
		assertFalse(home19Filter.judge(persons.get("2").getPlans().get(0)));
		home19Filter.addLink(new IdImpl("2"));
		System.out.println();
		assertFalse(home19Filter.judge(persons.get("1").getPlans().get(0)));
		assertTrue(home19Filter.judge(persons.get("2").getPlans().get(0)));

	}

}
