package playground.mmoyo.algorithms;

import java.io.File;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.XY2Links;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;

public class MyXYtoLinksLauncher extends AbstractPersonAlgorithm{
	final Scenario scn;
	private XY2Links xy2Links;
	private ActFacilityNullifier actFacilityNullifier = new ActFacilityNullifier();
	private ActLinkNullifier actLinkNullifier = new ActLinkNullifier();
	
	public MyXYtoLinksLauncher(final Scenario scn){
		this.scn = scn;
		xy2Links = new XY2Links((ScenarioImpl) scn);	
	}
	
	@Override
	public void run(Person person) {
		actLinkNullifier.run(person);			  //nullify link
		actFacilityNullifier.run(person);		  //nullify facility
		xy2Links.run(person);
	}
	
	public static void main(String[] args) {
		String popFilePath;
		String netFilePath;
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
		}else{
			popFilePath = "../../";
			netFilePath = "../../";
		}

		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);
		Population pop= scn.getPopulation();
		new MyXYtoLinksLauncher(scn).run(pop);
		
		//write file
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(pop, scn.getNetwork());
		File file = new File(popFilePath);
		String outFile = file.getParent() + "/" + file.getName() + "w_xy2Links.xml.gz"; 
		popwriter.write(outFile) ;
		System.out.println("done");
		
		//write a sample of first 10 persons
		Population popSample = new FirstPersonsExtractor().run(pop, 10);
		System.out.println("writing sample plan file...");
		popwriter = new PopulationWriter(popSample, scn.getNetwork());
		popwriter.write(outFile + "sample.xml") ;
		System.out.println("done");
	}
	
}
