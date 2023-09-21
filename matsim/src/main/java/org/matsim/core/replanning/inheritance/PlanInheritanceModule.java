package org.matsim.core.replanning.inheritance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonUtils;

import com.google.inject.Singleton;

@Singleton
public class PlanInheritanceModule extends AbstractModule implements StartupListener, BeforeMobsimListener, ShutdownListener  {
	

	public static final String PLAN_ID = "planId";
	public static final String ITERATION_CREATED = "iterationCreated";
	public static final String PLAN_MUTATOR = "planMutator";
	
	public static final String INITIAL_PLAN = "initialPlan";
	
	public static final String FILENAME_PLAN_INHERITANCE_RECORDS = "planInheritanceRecords";
	
	long numberOfPlanInheritanceRecordsCreated = 0;
	Map<String, PlanInheritanceRecord> planId2planInheritanceRecords = new ConcurrentHashMap<>();
	
	PlanInheritanceRecordWriter planInheritanceRecordWriter;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		CompressionType compressionType = event.getServices().getConfig().controler().getCompressionType();
		this.planInheritanceRecordWriter = new PlanInheritanceRecordWriter(event.getServices().getControlerIO().getOutputFilename(FILENAME_PLAN_INHERITANCE_RECORDS + ".csv", compressionType)); 
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		Set<String> activePlanIds = new HashSet<>();
		
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				
				if (plan.getPlanMutator() == null) {
					// initial plan - set initial plan defaults
					plan.setPlanMutator(INITIAL_PLAN);
					plan.setIterationCreated(event.getIteration());
				}
				
				if (plan.getIterationCreated() == event.getIteration()) {
					// it's a new plan created in this iteration - create a new record
					
					PlanInheritanceRecord planInheritanceRecord = new PlanInheritanceRecord();
					planInheritanceRecord.agentId = person.getId().toString();
					planInheritanceRecord.planId = Long.toString(++this.numberOfPlanInheritanceRecordsCreated, 36);
					planInheritanceRecord.ancestorId = plan.getPlanId();
					plan.setPlanId(planInheritanceRecord.planId);
					planInheritanceRecord.iterationCreated = plan.getIterationCreated();
					planInheritanceRecord.mutatedBy = plan.getPlanMutator();
					
					this.planId2planInheritanceRecords.put(planInheritanceRecord.planId, planInheritanceRecord);
				}
				
				if (PersonUtils.isSelected(plan)) {
					this.planId2planInheritanceRecords.get(plan.getPlanId()).iterationsSelected.add(event.getIteration());
				}
				
				activePlanIds.add(plan.getPlanId());
			}
		}
		
		List<String> deletedPlans = new ArrayList<>();
		for (String planId : this.planId2planInheritanceRecords.keySet()) {
			if (!activePlanIds.contains(planId)) {
				deletedPlans.add(planId);
			}
		}
		
		for (String deletedPlanId : deletedPlans) {
			PlanInheritanceRecord deletedPlanInheritanceRecord = this.planId2planInheritanceRecords.remove(deletedPlanId);
			deletedPlanInheritanceRecord.iterationRemoved = event.getIteration();
			this.planInheritanceRecordWriter.write(deletedPlanInheritanceRecord);
		}
		
		this.planInheritanceRecordWriter.flush();
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		for (PlanInheritanceRecord planInheritanceRecord : this.planId2planInheritanceRecords.values()) {
			this.planInheritanceRecordWriter.write(planInheritanceRecord);
		}
		
		this.planInheritanceRecordWriter.flush();
		this.planInheritanceRecordWriter.close();
		
		this.planId2planInheritanceRecords.clear();
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(PlanInheritanceModule.class);
	}
}
