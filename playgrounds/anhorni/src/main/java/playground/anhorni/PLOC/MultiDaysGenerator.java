package playground.anhorni.PLOC;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.core.scenario.ScenarioImpl;

import playground.anhorni.LEGO.miniscenario.create.ComputeMaxEpsilons;
import playground.anhorni.LEGO.miniscenario.create.HandleUnobservedHeterogeneity;
import playground.anhorni.random.RandomFromVarDistr;

public class MultiDaysGenerator {
	
	private Random randomNumberGenerator;
	private SinglePlanGenerator singlePlanGenerator;
	private PopulationImpl staticPopulation;
	private ScenarioImpl scenarioWriteOut;
	private NetworkImpl network;
	private boolean temporalVar;
	
	private final String LCEXP = "locationchoiceExperimental";
	
	public MultiDaysGenerator(Random randomNumberGenerator, PopulationImpl staticPopulation, ScenarioImpl scenarioWriteOut, 
			NetworkImpl network, boolean temporalVar, ObjectAttributes personAttributes) {
		this.randomNumberGenerator = randomNumberGenerator;
		for (int i = 0; i < 1000; i++) {
			this.randomNumberGenerator.nextDouble();
		}
		this.staticPopulation = staticPopulation;
		this.scenarioWriteOut = scenarioWriteOut;
		this.network = network;
		this.temporalVar = temporalVar;
		singlePlanGenerator = new SinglePlanGenerator(scenarioWriteOut.getActivityFacilities(), temporalVar);
	}
	
	public void generatePlans(int runId) {
		List<Integer> keyList = new Vector<Integer>();
		for (Id id : staticPopulation.getPersons().keySet()) {
			keyList.add(Integer.parseInt(id.toString()));
		}
		
		for (int day = 0; day < 5; day++) {
			
			double average = 0.0;
			for (int i = 0; i < 5; i++) {
				average += MultiplerunsControler.share[i] / 5.0;
			}
			double limit = average;
			if (temporalVar) {
				limit = MultiplerunsControler.share[day];
			}
			Collections.shuffle(keyList, randomNumberGenerator);
			
			this.generatePlan(keyList, limit, day);
			
			String path = Create3TownsScenario.outputFolder + "/runs/run" + runId + "/day" + day;
			new File(path).mkdirs();
			this.adaptForDestinationChoice();
			this.writePlansAndFacs(path + "/plans.xml", path + "/facilities.xml");
			scenarioWriteOut.getPopulation().getPersons().clear();
		}	
	}
	
	private void generatePlan(List<Integer> keyList, double limit, int day) {
		for (Integer id : keyList) {
			PersonImpl p = (PersonImpl)staticPopulation.getPersons().get(new IdImpl(id));

			// copy person -------
			PersonImpl pTmp = new PersonImpl(new IdImpl(id));
			// copy person -------
			
			boolean worker = false;
			if (this.randomNumberGenerator.nextDouble() < limit) {
				worker = true;
			}
			PlanImpl plan = singlePlanGenerator.generatePlan(worker, p, day);
			pTmp.addPlan(plan);
			scenarioWriteOut.getPopulation().addPerson(pTmp);
		}
	}
	
	private void adaptForDestinationChoice() {	
		ConfigReader configReader = new ConfigReader();
		configReader.read();
		
		Config config = (ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(
				"src/main/java/playground/anhorni/input/PLOC/3towns/config.xml").getScenario()).getConfig();
				
		RandomFromVarDistr rnd = new RandomFromVarDistr(this.randomNumberGenerator);
		HandleUnobservedHeterogeneity hhandler = new HandleUnobservedHeterogeneity(this.scenarioWriteOut, config, rnd);
		hhandler.assign(); 
			
		ComputeMaxEpsilons maxEpsilonComputer = new ComputeMaxEpsilons(10, scenarioWriteOut, "s", config,
				Long.parseLong(config.findParam(LCEXP, "randomSeed")));
		maxEpsilonComputer.prepareReplanning();
		for (Person p : this.scenarioWriteOut.getPopulation().getPersons().values()) {
			maxEpsilonComputer.handlePlan(p.getSelectedPlan());
		}
		maxEpsilonComputer.finishReplanning();
		
		maxEpsilonComputer = new ComputeMaxEpsilons(10, scenarioWriteOut, "l", config,
				Long.parseLong(config.findParam(LCEXP, "randomSeed")));
		maxEpsilonComputer.prepareReplanning();
		for (Person p : this.scenarioWriteOut.getPopulation().getPersons().values()) {
			maxEpsilonComputer.handlePlan(p.getSelectedPlan());
		}
		maxEpsilonComputer.finishReplanning();
	}	
		
	public void writePlansAndFacs(String plansFile, String facsFile) {	
		new PopulationWriter(scenarioWriteOut.getPopulation(), network).write(plansFile);
		new FacilitiesWriter(this.scenarioWriteOut.getActivityFacilities()).write(facsFile);
	}
}
