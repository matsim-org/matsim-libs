package playground.jhackney.activitySpaces;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

public class ActivitySpaces {

	private static final String CUSTOMIZABLE_KEY = "activitySpaces";
	
	public static final ActivitySpace createActivitySpace(final String type, final String act_type, final Person person) {
		ActivitySpace asp = null;
		if (type.equals("ellipse")) {
			asp = new ActivitySpaceEllipse(act_type);
		} else if (type.equals("cassini")) {
			asp = new ActivitySpaceCassini(act_type);
		}else if (type.equals("superellipse")) {
			asp = new ActivitySpaceSuperEllipse(act_type);
		}else if (type.equals("bean")) {
			asp = new ActivitySpaceBean(act_type);
		} else {
			Gbl.errorMsg("[type="+type+" not allowed]");
		}
		List<ActivitySpace> spaces = (List<ActivitySpace>) person.getCustomAttributes().get(CUSTOMIZABLE_KEY);
		if (spaces == null) {
			spaces = new ArrayList<ActivitySpace>(1);
			person.getCustomAttributes().put(CUSTOMIZABLE_KEY, spaces);
		}
		spaces.add(asp);
		return asp;
	}
	
	public static final List<ActivitySpace> getActivitySpaces(final Person person) {
		return (List<ActivitySpace>) person.getCustomAttributes().get(CUSTOMIZABLE_KEY);
	}
	
	public static final void resetActivitySpaces(final Person person) {
		person.getCustomAttributes().remove(CUSTOMIZABLE_KEY);
	}
}
