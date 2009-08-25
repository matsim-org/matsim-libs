package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.andreas.bln.NewPopulation;
import playground.wrashid.tryouts.plan.KeepOnlyMIVPlans;

public class AddParkings extends AbstractPersonAlgorithm {

	protected PopulationWriter popWriter;
	
	public AddParkings(PopulationImpl inPop, String outputPlansFile) {
		this.popWriter = new PopulationWriter(inPop, outputPlansFile, "v4");
		this.popWriter.writeStartPlans();
	}

	public static void main(String[] args) {
		String inputPlansFile = "test/input/playground/wrashid/PSF/converter/addParkings/plans2.xml";
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		String outputPlansFile = "output/plans1.xml";
		generatePlanWithParkingActs(inputPlansFile,networkFile,outputPlansFile);
	}
	
	/**
	 * As input this method receives a parking (file?), without parkings acts
	 * (and related lets) and adds these.
	 */
	public static void generatePlanWithParkingActs(String inputPlansFile, String networkFile, String outputPlansFile) {
		
		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());
		
		
		
		
		PopulationImpl inPop = new PopulationImpl();
		
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);
		
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inputPlansFile);
		
		AddParkings dp = new AddParkings(inPop, outputPlansFile);
		dp.run(inPop);
		dp.popWriter.writeEndPlans();
	}

	@Override
	public void run(PersonImpl person) {
		System.out.println(person.getAge());
		
	}
	
	

}
