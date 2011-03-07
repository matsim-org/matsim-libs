package playground.anhorni.scenarios;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

public class MultiDaysGenerator {
	
	private Random randomNumberGenerator;
	private SinglePlanGenerator singlePlanGenerator = new SinglePlanGenerator();
	private PopulationImpl staticPopulation;
	private ScenarioImpl scenarioWriteOut;
	private NetworkImpl network;
	
	public MultiDaysGenerator(long seed, PopulationImpl staticPopulation, ScenarioImpl scenarioWriteOut, NetworkImpl network) {
		this.randomNumberGenerator = new Random(seed);
		for (int i = 0; i < 1000; i++) {
			this.randomNumberGenerator.nextDouble();
		}
		this.staticPopulation = staticPopulation;
		this.scenarioWriteOut = scenarioWriteOut;
		this.network = network;
	}
	
	public void generatePlans(int runId) {
		List<Integer> keyList = new Vector<Integer>();
		for (Id id : staticPopulation.getPersons().keySet()) {
			keyList.add(Integer.parseInt(id.toString()));
		}
		for (int i = 0; i < 5; i++) {
			double limit = 0.75 - i * 0.05;
			Collections.shuffle(keyList, randomNumberGenerator);
			
			this.generateRandomPlansPerDay(keyList, limit);
			
			String path = Create3TownsDemand.outputFolder + "/plans/run" + runId + "/day" + i;
			new File(path).mkdirs();
			this.writePlans(path + "/plans.xml");
			scenarioWriteOut.getPopulation().getPersons().clear();
		}	
	}
	
	private void generateRandomPlansPerDay(List<Integer> keyList, double limit) {
		int cnt = 0;
		for (Integer id : keyList) {
			PersonImpl p = (PersonImpl)staticPopulation.getPersons().get(new IdImpl(id));

			// copy person -------
			PersonImpl pTmp = new PersonImpl(new IdImpl(cnt));
			pTmp.createDesires(p.getDesires().getDesc());
			// copy person -------
			
			boolean worker = false;
			if (this.randomNumberGenerator.nextDouble() > limit) {
				worker = true;
			}
			int homeId = 1;
			if ((Integer)p.getCustomAttributes().get("townId") == 1) {
				homeId = 8;
			}
			PlanImpl plan = singlePlanGenerator.generatePlan(homeId, worker, p);
			pTmp.addPlan(plan);
			scenarioWriteOut.getPopulation().addPerson(pTmp);
			cnt++;
		}
	}
	public void writePlans(String file) {	
		new PopulationWriter(scenarioWriteOut.getPopulation(), network).write(file);
	}
}
