package org.matsim.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QVehicle;

/**
 * QuerySpinneNOW is a special case of QuerySpinne, where not all agents passing the link are 
 * considered, but only the ones traveling the links at the time given.
 * 
 * @author dstrippgen
 *
 */
public class QuerySpinneNOW extends QuerySpinne {

	@Override
	protected List<Plan> getPersons(Population plans, QNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();
		QLink link = net.getLinks().get(queryLinkId);
		Collection<QVehicle> vehs = link.getAllVehicles();
		for( QVehicle veh : vehs) actPersons.add(veh.getDriver().getPerson().getSelectedPlan());
		
		return actPersons;
	}

}
