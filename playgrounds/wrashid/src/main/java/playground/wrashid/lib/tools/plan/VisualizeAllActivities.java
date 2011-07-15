package playground.wrashid.lib.tools.plan;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class VisualizeAllActivities {

	public static void main(String[] args) {
		BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();

		String inputPlansFile = "K:/Projekte/herbie/output/demandCreation/plans_1pct.xml.gz";
		String inputNetworkFile = "K:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
		String inputFacilities = "K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		double samplingSize = 0.1;
		Random rand = new Random();

		String outputKmlFile = "C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/kmls/acts-selected agents.kml";

		Scenario scenario = GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (rand.nextDouble() > samplingSize) {
				continue;
			}
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					//TODO: selection of single agent does not work -> find out why...
					//if (person.getId().equals(new IdImpl("7507711"))) {						
						ActivityImpl activity = (ActivityImpl) pe;
						basicPointVisualizer.addPointCoordinate(activity.getCoord(), "act=" + activity.getType() + ";agentId="
								+ person.getId(), Color.GREEN);
					//}
				}
			}
		}

		System.out.println("writing kml file...");
		basicPointVisualizer.write(outputKmlFile);
	}

}
