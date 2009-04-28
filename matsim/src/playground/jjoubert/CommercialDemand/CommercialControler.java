package playground.jjoubert.CommercialDemand;

import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.vis.netvis.NetVis;

public class CommercialControler {
	
	public static void main(String[] args){
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";
		
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(new String[] {"./examples/tutorial/myConfigScoring.xml"});
		
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		
		Population population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(plansFilename);
		
		Events events = new Events();
		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
		events.addHandler(eventWriter);
		
		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.openNetStateWriter("./output/simout", netFilename, 10);
			
		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		EventsToScore scoring = new EventsToScore(population, factory);
		events.addHandler(scoring);
		
		sim.run();
		
		scoring.finish();
		
		eventWriter.closeFile();
		
		Gbl.setConfig(null);
		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
		
	}

}
