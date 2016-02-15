package playground.wrashid.lib.tools.plan;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacility;

public class CorrectActFacilityLinkInPlans {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputPlansFile = "H:/data/experiments/ARTEMIS/input/plans_census2000v2_zrhCutC_1pct.xml.gz";
		// String inputPlansFile =
		// "H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputNetworkFile = "H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String facilitiesToLinkFile = "H:/data/cvs/ivt/studies/switzerland/f2l/teleatlas-ivtcheu/f2l.txt";

		String outputPlansFile = "H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu/census2000v2_dilZh30km_10pct/plans_new.xml.gz";

		Matrix matrix = GeneralLib.readStringMatrix(facilitiesToLinkFile);

		// factilityId, linkId
		HashMap<Id<ActivityFacility>, Id<Link>> f2l = new HashMap<>();
		for (int i = 1; i < matrix.getNumberOfRows(); i++) {
			f2l.put(Id.create(matrix.getString(i, 0), ActivityFacility.class),
					Id.create(matrix.getString(i, 1), Link.class));
		}

		MutableScenario scenario = (MutableScenario) GeneralLib.readScenario(
				inputPlansFile, inputNetworkFile);

		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();

			for (PlanElement pe : selectedPlan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					Id<Link> newLinkId = f2l.get(((ActivityImpl) pe).getFacilityId());

					if (!newLinkId.toString().equalsIgnoreCase(
							act.getLinkId().toString())) {
						act.setLinkId(newLinkId);
					}

				}
			}

		}

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(),
				1.0).write(outputPlansFile);
	}

}
