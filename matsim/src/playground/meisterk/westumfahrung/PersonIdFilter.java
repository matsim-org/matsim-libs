package playground.meisterk.westumfahrung;

import java.util.regex.Pattern;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.filters.AbstractPersonFilter;

public class PersonIdFilter extends AbstractPersonFilter {

	private String personIdPattern = null;
	private boolean isRecordPlans = false;
	private Plans plans = new Plans();
	
	public PersonIdFilter(final String personIdPattern, final boolean isRecordPlans) {
		super();
		this.personIdPattern = personIdPattern;
		this.isRecordPlans = isRecordPlans;
	}

	@Override
	public boolean judge(Person person) {

		String personId = person.getId().toString();
		
		if (Pattern.matches(this.personIdPattern, personId)) {
			return true;
		}
		
		return false;
	}

	@Override
	public void run(Person person) {
		// simply: do nothing :-)
		if (judge(person)) {
			count();
			if (this.isRecordPlans) {
				try {
					plans.addPerson(person);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public boolean isRecordPlans() {
		return isRecordPlans;
	}

	public Plans getPlans() {
		return plans;
	}

	
	
}
