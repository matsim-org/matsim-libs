package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.mmoyo.utils.DataLoader;

/** modifies the activity link Id*/
public class ActLinkIdChanger extends AbstractPersonAlgorithm {
	private Network net;
	private final String prefix;
	
	public ActLinkIdChanger (final Network net, final String prefix){
		this.net = net;
		this.prefix = prefix;
	}

	@Override
	public void run(Person person) {
		for (Plan plan :person.getPlans()){
			for (PlanElement pe :plan.getPlanElements()){
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					Id newId =  new IdImpl(this.prefix +  act.getLinkId()); 

					//verify that the link exists in net
					if (!this.net.getLinks().keySet().contains(newId)){
						throw new NullPointerException("The link does not exist in network: " + newId );
					}
					act.setLinkId(newId);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../input/juni/newDemand/bvg.run190.25pct.100Cleaned.plans.xml.gz";
		String netFilePath = "../../input/juni/newDemand/multimodalNet.xml.gz";
		String suffix ="miv_"; 

		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);
		new ActLinkIdChanger(scn.getNetwork(), suffix).run(scn.getPopulation());

		//write file
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(scn.getPopulation(), scn.getNetwork());
		popwriter.write(new File(popFilePath).getParent() + "/popCleanedWithMivSuffix.xml.gz") ;
		System.out.println("done");
	}
}
