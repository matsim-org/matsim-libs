package matsimConnector.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.CAQLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QCALink;

import pedCA.engine.SimulationEngine;


public class CAEngine implements MobsimEngine{
	
	private final Scenario scenario;
	private final CAScenario scenarioCA;
	private Map<Id<CAEnvironment>, SimulationEngine> enginesCA;
	
	private final List<QCALink> hiResLinks = new ArrayList<QCALink>();
	private final List<CAQLink> lowResLinks = new ArrayList<CAQLink>();
	private double simCATime;
	
	public CAEngine(QSim qSim, CAAgentFactory agentFactoryCA){
		this.simCATime = 0.0;
		this.scenario = qSim.getScenario();
		this.scenarioCA = (CAScenario) scenario.getScenarioElement(Constants.CASCENARIO_NAME);
		this.enginesCA = new HashMap <Id<CAEnvironment>, SimulationEngine>();
		generateCAEngines();
		initGenerators(agentFactoryCA);
	}
		
	private void initGenerators(CAAgentFactory agentFactoryCA) {
		for (Id<CAEnvironment> key : enginesCA.keySet())
			agentFactoryCA.addAgentsGenerator(key, enginesCA.get(key).getAgentGenerator());		
	}

	private void generateCAEngines() {
		for(CAEnvironment environmentCA : scenarioCA.getEnvironments().values())
			addEngine(environmentCA);
	}

	@Override
	public void doSimStep(double time) {
		double stepDuration = Constants.CA_STEP_DURATION;
		//TODO FIX THIS
		for (; simCATime < time + stepDuration; simCATime+=stepDuration) 
			for (SimulationEngine engine : enginesCA.values()) 
				engine.doSimStep(simCATime);		
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub
		
	}
	
	private void addEngine(CAEnvironment environmentCA){
		enginesCA.put(environmentCA.getId(), new SimulationEngine(environmentCA.getContext()));
	}

	public void registerHiResLink(QCALink hiResLink) {
		hiResLinks.add(hiResLink);
	}

	public void registerLowResLink(CAQLink lowResLink) {
		lowResLinks.add(lowResLink);
	}
	
}