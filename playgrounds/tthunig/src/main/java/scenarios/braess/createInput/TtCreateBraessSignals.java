/**
 * 
 */
package scenarios.braess.createInput;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.data.v20.Lane;

/**
 * Class to create signals (signal systems, signal groups and signal control)
 * for the Braess scenario.
 * 
 * @author tthunig
 * 
 */
public class TtCreateBraessSignals {

	private static final Double START_TIME = 0.0;
	private static final Double END_TIME = 0.0;
	private static final Integer CYCLE_TIME = 60;

	private Scenario scenario;

	private boolean simulateInflowCap7 = false;
	private boolean simulateInflowCap9 = false;

	public TtCreateBraessSignals(Scenario scenario) {
		this.scenario = scenario;

		checkInflowSimulation();
	}

	/**
	 * Checks whether the network simulates inflow capacity at links 2_3 and 4_5
	 * or not.
	 * 
	 * If the network contains nodes 7 or 9, it simulates inflow capacity;
	 * otherwise it doesn't.
	 * 
	 * The boolean simulateInflowCap is necessary for creating initial plans in
	 * createPersons(...)
	 */
	private void checkInflowSimulation() {

		Network net = this.scenario.getNetwork();

		if (net.getNodes().containsKey(Id.createNodeId(7)))
			this.simulateInflowCap7 = true;
		if (net.getNodes().containsKey(Id.createNodeId(9)))
			this.simulateInflowCap9 = true;
	}

	public void createSignals() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);

		createSignalSystems(signalsData);
		createSignalControl(signalsData);
		createSignalGroups(signalsData);

		// TODO handle the case, when no lanes are used!
	}

	private void createSignalSystems(SignalsData signalsData) {

		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

		SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();

		// create signal system at node 2
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create(
				"signalSystem2", SignalSystem.class));

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal1_2.1", Signal.class), Id.createLinkId("1_2"),
				Id.create("1_2.1", Lane.class));
//		SignalData signal = fac.createSignalData(Id.create("signal1_2.1",
//				Signal.class));
//		signal.setLinkId(Id.createLinkId("1_2"));
//		signal.addLaneId(Id.create("1_2.1", Lane.class));
//		signalSystem.addSignalData(signal);

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal1_2.2", Signal.class), Id.createLinkId("1_2"),
				Id.create("1_2.2", Lane.class));
//		signal = fac.createSignalData(Id.create("signal1_2.2", Signal.class));
//		signal.setLinkId(Id.createLinkId("1_2"));
//		signal.addLaneId(Id.create("1_2.2", Lane.class));
//		signalSystem.addSignalData(signal);

		signalSystems.addSignalSystemData(signalSystem);

		// create signal system at node 3
		signalSystem = fac.createSignalSystemData(Id.create("signalSystem3",
				SignalSystem.class));

		if (simulateInflowCap7) {
			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal7_3.1", Signal.class), Id.createLinkId("7_3"),
					Id.create("7_3.1", Lane.class));
//			signal = fac.createSignalData(Id
//					.create("signal7_3.1", Signal.class));
//			signal.setLinkId(Id.createLinkId("7_3"));
//			signal.addLaneId(Id.create("7_3.1", Lane.class));
//			signalSystem.addSignalData(signal);

			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal7_3.2", Signal.class), Id.createLinkId("7_3"),
					Id.create("7_3.2", Lane.class));
//			signal = fac.createSignalData(Id
//					.create("signal7_3.2", Signal.class));
//			signal.setLinkId(Id.createLinkId("7_3"));
//			signal.addLaneId(Id.create("7_3.2", Lane.class));
//			signalSystem.addSignalData(signal);
		} else {
			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal2_3.1", Signal.class), Id.createLinkId("2_3"),
					Id.create("2_3.1", Lane.class));
//			signal = fac.createSignalData(Id
//					.create("signal2_3.1", Signal.class));
//			signal.setLinkId(Id.createLinkId("2_3"));
//			signal.addLaneId(Id.create("2_3.1", Lane.class));
//			signalSystem.addSignalData(signal);

			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal2_3.2", Signal.class), Id.createLinkId("2_3"),
					Id.create("2_3.2", Lane.class));
//			signal = fac.createSignalData(Id
//					.create("signal2_3.2", Signal.class));
//			signal.setLinkId(Id.createLinkId("2_3"));
//			signal.addLaneId(Id.create("2_3.2", Lane.class));
//			signalSystem.addSignalData(signal);			
		}

		signalSystems.addSignalSystemData(signalSystem);

		// create signal system at node 4
		signalSystem = fac.createSignalSystemData(Id.create("signalSystem4",
				SignalSystem.class));

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal2_4", Signal.class), Id.createLinkId("2_4"));
//		signal = fac.createSignalData(Id.create("signal2_4", Signal.class));
//		signal.setLinkId(Id.createLinkId("2_4"));
//		signalSystem.addSignalData(signal);

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal3_4", Signal.class), Id.createLinkId("3_4"));
//		signal = fac.createSignalData(Id.create("signal3_4", Signal.class));
//		signal.setLinkId(Id.createLinkId("3_4"));
//		signalSystem.addSignalData(signal);

		signalSystems.addSignalSystemData(signalSystem);

		// create signal system at node 5
		signalSystem = fac.createSignalSystemData(Id.create("signalSystem5",
				SignalSystem.class));

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal3_5", Signal.class), Id.createLinkId("3_5"));
//		signal = fac.createSignalData(Id.create("signal3_5", Signal.class));
//		signal.setLinkId(Id.createLinkId("3_5"));
//		signalSystem.addSignalData(signal);

		if (this.simulateInflowCap9) {
			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal9_5", Signal.class), Id.createLinkId("9_5"));
//			signal = fac.createSignalData(Id.create("signal9_5", Signal.class));
//			signal.setLinkId(Id.createLinkId("9_5"));
//			signalSystem.addSignalData(signal);
		} else {
			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal4_5", Signal.class), Id.createLinkId("4_5"));
//			signal = fac.createSignalData(Id.create("signal4_5", Signal.class));
//			signal.setLinkId(Id.createLinkId("4_5"));
//			signalSystem.addSignalData(signal);
		}

		signalSystems.addSignalSystemData(signalSystem);
	}

	private void createSignalGroups(SignalsData signalsData) {

		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		
		// create signal groups for each signal system
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()){
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
		}
	}

	private void createSignalControl(SignalsData signalsData) {

		SignalControlData signalControl = signalsData.getSignalControlData();

		SignalControlDataFactory fac = new SignalControlDataFactoryImpl();

		// create signal control for signal system 2
		SignalSystemControllerData signalSystemController = fac
				.createSignalSystemControllerData(Id.create("signalSystem2",
						SignalSystem.class));
		
		SignalPlanData signalPlan = createDefaultSignalPlan(fac);
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal1_2.1", SignalGroup.class), 0, CYCLE_TIME));
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal1_2.2", SignalGroup.class), 0, CYCLE_TIME));

		signalSystemController.addSignalPlanData(signalPlan);
		signalSystemController
				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemController);
		
		// create signal control for signal system 3
		signalSystemController = fac.createSignalSystemControllerData(Id
				.create("signalSystem3", SignalSystem.class));

		signalPlan = createDefaultSignalPlan(fac);
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal2_3.1", SignalGroup.class), 0, CYCLE_TIME));
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal2_3.2", SignalGroup.class), 0, CYCLE_TIME));

		signalSystemController.addSignalPlanData(signalPlan);
		signalSystemController
				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemController);
		
		// create signal control for signal system 4
		signalSystemController = fac.createSignalSystemControllerData(Id
				.create("signalSystem4", SignalSystem.class));

		signalPlan = createDefaultSignalPlan(fac);
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal3_4.1", SignalGroup.class), 0, CYCLE_TIME));
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal2_4.2", SignalGroup.class), 0, CYCLE_TIME));

		signalSystemController.addSignalPlanData(signalPlan);
		signalSystemController
				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemController);
		
		// create signal control for signal system 5
		signalSystemController = fac.createSignalSystemControllerData(Id
				.create("signalSystem5", SignalSystem.class));

		signalPlan = createDefaultSignalPlan(fac);
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal3_5.1", SignalGroup.class), 0, CYCLE_TIME));
		signalPlan.addSignalGroupSettings(createSetting4SignalGroup(fac,
				Id.create("signal4_5.2", SignalGroup.class), 0, CYCLE_TIME));

		signalSystemController.addSignalPlanData(signalPlan);
		signalSystemController
				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemController);
	}

	/**
	 * Creates a default signal plan with offset 0.
	 * 
	 * @param fac
	 * @return the signalPlan
	 */
	private SignalPlanData createDefaultSignalPlan(SignalControlDataFactory fac) {
		SignalPlanData signalPlan = fac.createSignalPlanData(Id.create(1,
				SignalPlan.class));
		signalPlan.setStartTime(START_TIME);
		signalPlan.setEndTime(END_TIME);
		signalPlan.setCycleTime(CYCLE_TIME);
		signalPlan.setOffset(0);
		return signalPlan;
	}

	/**
	 * Creates a signal group setting for the given signal group id with the
	 * given onset and dropping time.
	 * 
	 * @param fac
	 * @param signalGroupId
	 * @param onset
	 * @param dropping
	 * @return
	 */
	private SignalGroupSettingsData createSetting4SignalGroup(
			SignalControlDataFactory fac, Id<SignalGroup> signalGroupId,
			int onset, int dropping) {

		SignalGroupSettingsData signalGroupSettings = fac
				.createSignalGroupSettingsData(signalGroupId);
		signalGroupSettings.setOnset(onset);
		signalGroupSettings.setDropping(dropping);
		return signalGroupSettings;
	}

}
