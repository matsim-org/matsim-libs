package saleem.stockholmscenario.teleportation.ptoptimisation;

import opdytsintegration.MATSimState;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * Considering the "day-to-day" iterations of MATSim as the stages of a
 * discrete-time stochastic process, this class represents the state of this
 * process. This state consists of the plan choice sets of all agents, including
 * scores and information about the selected plan.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see SimulatorState
 */
public class PTMatsimState extends MATSimState {

	private final TransitSchedule schedule;
	
	private final Vehicles vehicles;
	
	private final Scenario scenario;
	
	
	// -------------------- CONSTRUCTION --------------------

	/**
	 * Takes over a <em>deep copy</em> of the population's plans and a
	 * <em>reference</em> to the vector representation.
	 * 
	 * @param population
	 *            the current MATSim population
	 * @param vectorRepresentation
	 *            a real-valued vector representation of the current MATSim
	 *            state.
	 */
	public PTMatsimState(final Population population,
			final Vector vectorRepresentation, final Scenario scenario, final PTSchedule ptscehedule) {
		super(population,vectorRepresentation);
		this.scenario=scenario;
		ScenarioHelper helper = new ScenarioHelper();
		this.schedule=helper.deepCopyTransitSchedule(ptscehedule.getPreSchedule());
		this.vehicles=helper.deepCopyVehicles(ptscehedule.getPreVehicles());
	}

	// -------------------- HELPERS AND INTERNALS --------------------

	public void implementInSimulation() {
		ScenarioHelper helper = new ScenarioHelper();
//		System.out.println("Statistics:\t" + helper.getNumberOfRoutes(scenario.getTransitSchedule())+ "\t" + (scenario.getTransitVehicles().getVehicles().size()-helper.getUnusedVehs(scenario.getTransitSchedule())));
		helper.removeEntireScheduleAndVehicles(scenario);//Removes all vehicle types, vehicles, stop facilities and transit lines from a transit schedule
		helper.addVehicles(scenario, vehicles);//Adds all vehicle types and vehicles from an updated stand alone vehicles object into the current scenario vehicles object
		helper.addTransitSchedule(scenario, schedule);//Add all stop facilities and transit lines from a stand alone updated transit schedule into the current scenario transit schedule
		super.implementInSimulation();
//		System.out.println("Statistics:\t" + helper.getNumberOfRoutes(scenario.getTransitSchedule())+ "\t" + (scenario.getTransitVehicles().getVehicles().size()-helper.getUnusedVehs(scenario.getTransitSchedule())));
	
	}
}

