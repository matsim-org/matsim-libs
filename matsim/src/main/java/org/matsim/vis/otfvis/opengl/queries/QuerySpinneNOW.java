package org.matsim.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.ptproject.qsim.QueueVehicle;

/**
 * QuerySpinneNOW is a special case of QuerySpinne, where not all agents passing the link are 
 * considered, but only the ones traveling the links at the time given.
 * 
 * @author dstrippgen
 *
 */
public class QuerySpinneNOW extends QuerySpinne {

	@Override
	protected List<Plan> getPersons(Population plans, QueueNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();
		QueueLink link = net.getLinks().get(queryLinkId);
		Collection<QueueVehicle> vehs = link.getAllVehicles();
		for( QueueVehicle veh : vehs) actPersons.add(veh.getDriver().getPerson().getSelectedPlan());
		
		return actPersons;
	}

}
