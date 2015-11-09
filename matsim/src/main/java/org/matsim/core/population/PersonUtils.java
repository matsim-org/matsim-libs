package org.matsim.core.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.Iterator;
import java.util.TreeSet;

public class PersonUtils {
	private final static String SEX_ATTRIBUTE="sex";
	private final static String HAS_LICENSE= "hasLicense";
	private static final String CAR_AVAIL = "carAvail";
	public static final String EMPLOYED = "employed";
	public static final String AGE = "age";
	private static final String TRAVELCARDS = "travelcards";
	private final static Logger log = Logger.getLogger(PersonImpl.class);

	@Deprecated // use methods of interface Person
	public static PlanImpl createAndAddPlan(Person person, final boolean selected) {
		PlanImpl p = new PlanImpl(person);
		person.addPlan(p);
		if (selected) {
			person.setSelectedPlan(p);
		}
		return p;
	}

	public static void removeUnselectedPlans(Person person) {
		for (Iterator<? extends Plan> iter = person.getPlans().iterator(); iter.hasNext(); ) {
			Plan plan = iter.next();
			if (!plan.isSelected()) {
				iter.remove();
			}
		}
	}

	@Deprecated // use PersonAttributes
	public static String getSex(Person person) {
		return (String) person.getCustomAttributes().get(SEX_ATTRIBUTE);
	}

	@Deprecated // use PersonAttributes
	public static Integer getAge(Person person) {
		return (Integer) person.getCustomAttributes().get(AGE);
	}

	@Deprecated // use PersonAttributes
	public static String getLicense(Person person) {
		return (String) person.getCustomAttributes().get(HAS_LICENSE);
	}

	@Deprecated // use PersonAttributes
	public static boolean hasLicense(Person person) {
		return ("yes".equals(getLicense(person))) || ("true".equals(getLicense(person)));
	}

	@Deprecated // use PersonAttributes
	public static String getCarAvail(Person person) {
		return (String) person.getCustomAttributes().get(CAR_AVAIL);
	}

	@Deprecated // use PersonAttributes
	public static Boolean isEmployed(Person person) {
		return (Boolean) person.getCustomAttributes().get(EMPLOYED);
	}

	@Deprecated // use PersonAttributes
	public static void setAge(Person person, final Integer age) {
		person.getCustomAttributes().put(AGE, age);
	}

	@Deprecated // use PersonAttributes
	public static void setSex(Person person, final String sex) {
		person.getCustomAttributes().put(SEX_ATTRIBUTE, sex);
	}

	@Deprecated // use PersonAttributes
	public static void setLicence(Person person, final String licence) {
		person.getCustomAttributes().put(HAS_LICENSE, licence);
	}

	@Deprecated // use PersonAttributes
	public static void setCarAvail(Person person, final String carAvail) {
		person.getCustomAttributes().put(CAR_AVAIL, carAvail);
	}

	@Deprecated // use PersonAttributes
	public static void setEmployed(Person person, final Boolean employed) {
		person.getCustomAttributes().put(EMPLOYED, employed);
	}

	@Deprecated // use PersonAttributes
	public static void addTravelcard(Person person, final String type) {
		if (getTravelcards(person) == null) {
			person.getCustomAttributes().put(TRAVELCARDS, new TreeSet<String>());
		}
		if (getTravelcards(person).contains(type)) {
			log.info(person + "[type=" + type + " already exists]");
		} else {
			getTravelcards(person).add(type.intern());
		}
	}

	@Deprecated // use PersonAttributes
	public static TreeSet<String> getTravelcards(Person person) {
		return (TreeSet<String>) person.getCustomAttributes().get(TRAVELCARDS);
	}
}
