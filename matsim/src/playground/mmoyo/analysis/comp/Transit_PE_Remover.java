package playground.mmoyo.analysis.comp;

import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;

import playground.mrieser.pt.router.TransitActsRemover;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**removes the Plan elements of a routed OevModel File*/
public class Transit_PE_Remover {
	final String routedPlansFile = "../shared-svn/studies/ptsimmanuel/input/pt_only.routedOevModell.xml"; 
	final String transitSchedule = "../shared-svn/studies/ptsimmanuel/input/transitSchedule.networkOevModellBln.xml";
	final String multimodalNetworkFile = "../shared-svn/studies/ptsimmanuel/input/network.multimodal.xml";
	final String ptRemovedPlansFile ="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/plans_without_PtRoute.xml";
	final NetworkLayer streetNetwork = new NetworkLayer();
	private final PopulationImpl population;
	
	private TransitActsRemover transitLegsRemover = new TransitActsRemover();

	public Transit_PE_Remover(){
		NetworkLayer streetNetwork= new NetworkLayer(new NetworkFactoryImpl());
		new MatsimNetworkReader(streetNetwork).readFile(multimodalNetworkFile);
		
		population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population, streetNetwork);
		plansReader.readFile(routedPlansFile);

		for (Person person : population.getPersons().values()){
			for (Plan plan : person.getPlans()){
				transitLegsRemover.run(plan);
			}
		}
		new PopulationWriter(population).writeFile(ptRemovedPlansFile);
	}
	

	public static void main(String[] args) {
		new Transit_PE_Remover ();
	}
}
