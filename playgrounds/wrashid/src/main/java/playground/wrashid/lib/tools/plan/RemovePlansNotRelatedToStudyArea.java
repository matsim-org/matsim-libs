package playground.wrashid.lib.tools.plan;

import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

public class RemovePlansNotRelatedToStudyArea {

	public static void main(final String[] args) {
		String inputPlansFile = "C:/data/parkingSearch/zurich/input/1pml_plans_ktiClean.xml.gz";
		String inputNetworkFile = "C:/data/parkingSearch/zurich/input/network.xml.gz";
		String inputFacilities = "C:/data/parkingSearch/zurich/input/facilities.xml.gz";

		Coord center = new Coord(683235.0, 247497.0);
		double radiusInMeters = 1000;
		boolean cut = false;

		String outputPlansFile = "C:/data/parkingSearch/zurich/input/1pml_dil_1km_plans_ktiClean.xml.gz";

		Scenario scenario = GeneralLib.readScenario(inputPlansFile,
				inputNetworkFile, inputFacilities);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		HashSet<Id> isRelevantForStudy = new HashSet<Id>();
		HashSet<Id> notRelevantForStudy = new HashSet<Id>();
		for (Person person : scenario.getPopulation().getPersons().values()) {

			PersonImpl p = (PersonImpl) person;
			PersonUtils.removeUnselectedPlans(p);

			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;

					if (GeneralLib.getDistance(
							network.getLinks().get(act.getLinkId()).getCoord(),
							center) < radiusInMeters) {
						isRelevantForStudy.add(person.getId());
					}

				}

				if (!cut) {
					if (pe instanceof LegImpl) {
						LegImpl leg = (LegImpl) pe;
						if (leg.getMode().equalsIgnoreCase("car")) {
							LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg
									.getRoute();

							for (Id linkId : route.getLinkIds()) {
								if (GeneralLib.getDistance(network.getLinks()
										.get(linkId).getCoord(), center) < radiusInMeters) {
									isRelevantForStudy.add(person.getId());
								}
							}

						}
					}
				}
			}
			
			if (!isRelevantForStudy.contains(person.getId())) {
				notRelevantForStudy.add(person.getId());
			}
			
			
		}

		
		
		for (Id personId: notRelevantForStudy) {
				scenario.getPopulation().getPersons().remove(personId);
		}

		GeneralLib.writePopulation(scenario.getPopulation(),
				scenario.getNetwork(), outputPlansFile);
	}

}
