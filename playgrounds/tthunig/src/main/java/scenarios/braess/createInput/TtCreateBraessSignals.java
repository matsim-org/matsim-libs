/**
 * 
 */
package scenarios.braess.createInput;

import org.apache.log4j.Logger;
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
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
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

	private static final Logger log = Logger
			.getLogger(TtCreateBraessSignals.class);
	
	private static final int CYCLE_TIME = 60;

	private Scenario scenario;
	private boolean retardMiddleRoute = false;

	private boolean simulateInflowCap7 = false;
	private boolean simulateInflowCap9 = false;
	private boolean lanesUsed;

	public TtCreateBraessSignals(Scenario scenario) {
		this.scenario = scenario;

		checkInflowSimulation();
		// check whether lanes are used or not
		this.lanesUsed = this.scenario.getConfig().qsim().isUseLanes();
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

		createSignalSystems();
		createSignalGroups();
		createAllGreenSignalControl();

		if (retardMiddleRoute) {
			changeSignalControlTo1Z();
		}
	}

	/**
	 * Creates signal systems depending on the network situation.
	 * 
	 * If lanes are used they already give the turning move restrictions such
	 * that no further turning move restrictions are necessary for the signal
	 * definitions. If no lanes are used this method defines the turning move
	 * restrictions for the signals.
	 * 
	 * Depending on the existence of inflow simulating nodes 7 and 9, the ids of
	 * lanes and links, the signals are located on or lead to, depend.
	 */
	private void createSignalSystems() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();

		// create signal system at node 2
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create(
				"signalSystem2", SignalSystem.class));

		SignalData signal = fac.createSignalData(Id.create("signal1_2.1",
				Signal.class));
		signal.setLinkId(Id.createLinkId("1_2"));
		if (this.lanesUsed) {
			signal.addLaneId(Id.create("1_2.1", Lane.class));
		} else { // no lanes used. turning move restrictions necessary
			if (this.simulateInflowCap7) {
				signal.addTurningMoveRestriction(Id.createLinkId("2_7"));
			} else {
				signal.addTurningMoveRestriction(Id.createLinkId("2_3"));
			}
		}
		signalSystem.addSignalData(signal);

		signal = fac.createSignalData(Id.create("signal1_2.2", Signal.class));
		signal.setLinkId(Id.createLinkId("1_2"));
		if (this.lanesUsed) {
			signal.addLaneId(Id.create("1_2.2", Lane.class));
		} else { // no lanes used. turning move restrictions necessary
			signal.addTurningMoveRestriction(Id.createLinkId("2_4"));
		}
		signalSystem.addSignalData(signal);

		signalSystems.addSignalSystemData(signalSystem);

		// create signal system at node 3
		signalSystem = fac.createSignalSystemData(Id.create("signalSystem3",
				SignalSystem.class));

		if (simulateInflowCap7) {
			signal = fac.createSignalData(Id
					.create("signal7_3.1", Signal.class));
			signal.setLinkId(Id.createLinkId("7_3"));
			if (this.lanesUsed) {
				signal.addLaneId(Id.create("7_3.1", Lane.class));
			} else { // no lanes used. turning move restrictions necessary
				signal.addTurningMoveRestriction(Id.createLinkId("3_5"));
			}
			signalSystem.addSignalData(signal);

			signal = fac.createSignalData(Id
					.create("signal7_3.2", Signal.class));
			signal.setLinkId(Id.createLinkId("7_3"));
			if (this.lanesUsed) {
				signal.addLaneId(Id.create("7_3.2", Lane.class));
			} else { // no lanes used. turning move restrictions necessary
				signal.addTurningMoveRestriction(Id.createLinkId("3_4"));
			}
			signalSystem.addSignalData(signal);
		} else { // no inflow capacity simulated at link 2_3
			signal = fac.createSignalData(Id
					.create("signal2_3.1", Signal.class));
			signal.setLinkId(Id.createLinkId("2_3"));
			if (this.lanesUsed) {
				signal.addLaneId(Id.create("2_3.1", Lane.class));
			} else { // no lanes used. turning move restrictions necessary
				signal.addTurningMoveRestriction(Id.createLinkId("3_5"));
			}
			signalSystem.addSignalData(signal);

			signal = fac.createSignalData(Id
					.create("signal2_3.2", Signal.class));
			signal.setLinkId(Id.createLinkId("2_3"));
			if (this.lanesUsed) {
				signal.addLaneId(Id.create("2_3.2", Lane.class));
			} else { // no lanes used. turning move restrictions necessary
				signal.addTurningMoveRestriction(Id.createLinkId("3_4"));
			}
			signalSystem.addSignalData(signal);
		}

		signalSystems.addSignalSystemData(signalSystem);

		// create signal system at node 4
		signalSystem = fac.createSignalSystemData(Id.create("signalSystem4",
				SignalSystem.class));

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal2_4", Signal.class), Id.createLinkId("2_4"),
				null);

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal3_4", Signal.class), Id.createLinkId("3_4"),
				null);

		signalSystems.addSignalSystemData(signalSystem);

		// create signal system at node 5
		signalSystem = fac.createSignalSystemData(Id.create("signalSystem5",
				SignalSystem.class));

		SignalUtils.createAndAddSignal(signalSystem, fac,
				Id.create("signal3_5", Signal.class), Id.createLinkId("3_5"),
				null);

		if (this.simulateInflowCap9) {
			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal9_5", Signal.class),
					Id.createLinkId("9_5"), null);
		} else {
			SignalUtils.createAndAddSignal(signalSystem, fac,
					Id.create("signal4_5", Signal.class),
					Id.createLinkId("4_5"), null);
		}

		signalSystems.addSignalSystemData(signalSystem);
	}

	private void createSignalGroups() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

		// create signal groups for each signal system
		for (SignalSystemData system : signalSystems.getSignalSystemData()
				.values()) {
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
		}
	}

	private void createAllGreenSignalControl() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory fac = new SignalControlDataFactoryImpl();

		// creates a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems
				.getSignalSystemData().values()) {

			SignalSystemControllerData signalSystemControl = fac
					.createSignalSystemControllerData(signalSystem.getId());

			// creates a default plan for the signal system (with defined cycle
			// time and offset 0)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
			// specifies signal group settings for all signal groups of this
			// signal system
			for (SignalGroupData signalGroup : signalGroups
					.getSignalGroupDataBySystemId(signalSystem.getId())
					.values()) {
				// uses all day green onset and dropping
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(
						fac, signalGroup.getId(), 0, CYCLE_TIME));
			}

			signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl
					.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalControl.addSignalSystemControllerData(signalSystemControl);
		}
	}	

	/**
	 * Sets the signal at link 3_4 (i.e. the middle route) green for only one
	 * second a cycle. (Green for no seconds is not possible.)
	 */
	private void changeSignalControlTo1Z() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData signalControl = signalsData.getSignalControlData();

		SignalSystemControllerData signalSystem4Control = signalControl
				.getSignalSystemControllerDataBySystemId().get(
						Id.create("signalSystem4", SignalSystem.class));
		for (SignalPlanData signalPlan : signalSystem4Control
				.getSignalPlanData().values()) {
			// note: every signal system has only one signal plan here

			// pick the signal at link 3_4 (which is the middle link) from the
			// signal plan
			SignalGroupSettingsData signalGroupZSetting;
			signalGroupZSetting = signalPlan
						.getSignalGroupSettingsDataByGroupId().get(
								Id.create("signal3_4", SignalGroup.class));

			// set the signal green for only one second
			signalGroupZSetting.setOnset(0);
			signalGroupZSetting.setDropping(1);
		}
	}

	/**
	 * If true the middle route gets green only for one second a cycle at node
	 * 3.
	 * 
	 * @param retardMiddleRoute
	 */
	public void setRetardMiddleRoute(boolean retardMiddleRoute) {
		this.retardMiddleRoute = retardMiddleRoute;
	}

}
