package link2link;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.data.Lane;

public class Link2LinkTestSignalsCreator {
	
	private static final int CYCLE_TIME = 60;

	private Scenario scenario;
	private boolean lanesUsed;
	
	public Link2LinkTestSignalsCreator(Scenario scenario, boolean lanesUsed) {
		this.scenario = scenario;
		// check whether lanes are used or not
		this.lanesUsed = lanesUsed;
	}
	
	public void createSignals(){
		createSignalSystems();
		createSignalGroups();
		createSignalControl();
	}
	
	private void createSignalSystems() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();

		// create signal system at node 2
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create(
				"signalSystem2", SignalSystem.class));

		SignalData signal = fac.createSignalData(Id.create("signal2to3",
				Signal.class));
		signal.setLinkId(Id.createLinkId("Link2"));
		if (this.lanesUsed) {
			signal.addLaneId(Id.create("2to3", Lane.class));
		}
		else{
			// no lanes used. turning move restrictions necessary
				signal.addTurningMoveRestriction(Id.createLinkId("Link3"));
		}
		
		signalSystem.addSignalData(signal);

		signal = fac.createSignalData(Id.create("signal2to5", Signal.class));
		signal.setLinkId(Id.createLinkId("Link2"));
		if (this.lanesUsed) {
			signal.addLaneId(Id.create("2to5", Lane.class));
		}
		else{
			// no lanes used. turning move restrictions necessary
			signal.addTurningMoveRestriction(Id.createLinkId("Link5"));
		}
		signalSystem.addSignalData(signal);
		signalSystems.addSignalSystemData(signalSystem);
	}
	
	private void createSignalGroups() {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

		// create signal groups for each signal system
		for (SignalSystemData system : signalSystems.getSignalSystemData()
				.values()) {
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
		}
	}
	
	
	private void createSignalControl() {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory fac = new SignalControlDataFactoryImpl();

		// creates a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems.getSignalSystemData().values()) {
			SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());

			// creates a default plan for the signal system (with defined cycle
			// time = 60s and offset 0)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
			
			for (SignalGroupData signalGroup : signalGroups	.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroup.getId(), 0, 45));
			}
			signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalControl.addSignalSystemControllerData(signalSystemControl);
		}
	}
	
}
