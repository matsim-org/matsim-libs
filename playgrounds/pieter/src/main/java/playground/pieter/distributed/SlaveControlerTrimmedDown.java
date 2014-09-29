package playground.pieter.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.PlanSelectorFactoryRegister;
import org.matsim.core.controler.PlanStrategyFactoryRegister;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.LinkStatsControlerListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.population.VspPlansCleaner;
import org.matsim.pt.counts.PtCountControlerListener;

import playground.pieter.pseudosimulation.mobsim.PSimFactory;

public class SlaveControlerTrimmedDown extends AbstractController implements IterationStartsListener, BeforeMobsimListener {
	class TimesReceiver implements Runnable {

		@Override
		public void run() {
			while (true) {
				boolean res = false;
				try {
					res = reader.readBoolean();
				} catch (IOException e) {
					System.out.println("Master terminated. Exiting.");
					System.exit(0);
				}
				try {
					if (res) {
						linkTravelTimes = (SerializableLinkTravelTimes) reader.readObject();
						writer.writeObject(plansCopyForSending);
					} else {
						System.out.println("Master terminated. Exiting.");

						System.exit(0);
					}
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private SerializableLinkTravelTimes linkTravelTimes;
	private ObjectInputStream reader;
	private ObjectOutputStream writer;
	private PSimFactory pSimFactory;
	private Map<String, PlanSerializable> plansCopyForSending;
	private Scenario scenarioData;
	private boolean dumpDataAtEnd;
	private ScoringFunctionFactory scoringFunctionFactory;
	private EventsManager events;
	private PlanStrategyFactoryRegister planStrategyFactoryRegister;
	private PlanSelectorFactoryRegister planSelectorFactoryRegister;
	private StrategyManager strategyManager;
	private Population population;
	private Config config;
	private ScoreStatsControlerListener scoreStats;
	private boolean scenarioLoaded;
	private Network network;
	private MobsimFactory mobsimFactory;

	public SlaveControlerTrimmedDown(String[] args) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		this.addControlerListener(this);
		Socket socket = new Socket(args[1], Integer.parseInt(args[2]));
		this.reader = new ObjectInputStream(socket.getInputStream());
		this.writer = new ObjectOutputStream(socket.getOutputStream());
		int myNumber = reader.readInt();
		this.getConfig().controler().setOutputDirectory(this.getConfig().controler().getOutputDirectory() + "_" + myNumber);
		removeNonSimulatedAgents((List<String>) reader.readObject());
		new Thread(new TimesReceiver()).start();
	}

	private void removeNonSimulatedAgents(List<String> idStrings) {
		Set<Id<Person>> noIds = new HashSet<>(this.getPopulation().getPersons().keySet());
		Set<String> noIdStrings = new HashSet<>();
		for (Id<Person> id : noIds)
			noIdStrings.add(id.toString());
		noIdStrings.removeAll(idStrings);

		for (String idString : noIdStrings) {
			this.getPopulation().getPersons().remove(Id.create(idString, Person.class));
		}

	}

	private Population getPopulation() {
		return population;
	}

	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		SlaveControlerTrimmedDown slave = new SlaveControlerTrimmedDown(args);
		slave.run();
	}

	private void run() {
		pSimFactory = new PSimFactory();
		this.setMobsimFactory(pSimFactory);
		Collection<Plan> plans = new ArrayList<>();
		for (Person person : this.getPopulation().getPersons().values())
			plans.add(person.getSelectedPlan());
	}

	private void setMobsimFactory(MobsimFactory mobsimFactory) {
		this.mobsimFactory = mobsimFactory;

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == 0 || linkTravelTimes == null)
			pSimFactory.setTravelTime(new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(network,
					this.getConfig().travelTimeCalculator()).getLinkTravelTimes());
		else
			pSimFactory.setTravelTime(linkTravelTimes);
		Collection<Plan> plans = new ArrayList<>();
		for (Person person : this.getPopulation().getPersons().values())
			plans.add(person.getSelectedPlan());
		pSimFactory.setPlans(plans);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Map<String, PlanSerializable> tempPlansCopyForSending = new HashMap<>();
		for (Person person : this.getPopulation().getPersons().values())
			tempPlansCopyForSending.put(person.getId().toString(), new PlanSerializable(person.getSelectedPlan()));
		plansCopyForSending = tempPlansCopyForSending;
	}

	@Override
	protected final void runMobSim(int iteration) {
		runMobSim();
	}
	protected void runMobSim() {
		Mobsim sim = pSimFactory.createMobsim(scenarioData, events);
		sim.run();
	}

	@Override
	protected void prepareForSim() {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean continueIterations(int iteration) {
		// TODO Auto-generated method stub
		return false;
	}

	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(getScenario(), getControlerIO(), getEvents(), manager, this.planStrategyFactoryRegister,
				this.planSelectorFactoryRegister);
		return manager;
	}

	private EventsManager getEvents() {
		return this.events;
	}

	private Scenario getScenario() {
		return this.scenarioData;
	}

	/**
	 * Loads a default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide basic functionality.
	 * <p/>
	 * Method is final now. If you think that you need to over-write this
	 * method, start from AbstractController instead.
	 */
	@Override
	protected final void loadCoreListeners() {
		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important.
		 * 
		 * IMPORTANT: The execution order is reverse to the order the listeners
		 * are added to the list.
		 */

		if (this.scoringFunctionFactory == null) {
			this.scoringFunctionFactory = ControlerDefaults.createDefaultScoringFunctionFactory(this.scenarioData);
		}

		PlansScoring plansScoring = new PlansScoring(this.scenarioData, this.events, getControlerIO(), this.scoringFunctionFactory);
		this.addCoreControlerListener(plansScoring);

		this.strategyManager = loadStrategyManager();
		this.addCoreControlerListener(new PlansReplanning(this.strategyManager, population));

		this.addCoreControlerListener(new EventsHandling(this.events, this.getConfig().controler().getWriteEventsInterval(), this
				.getConfig().controler().getEventsFileFormats(), this.getControlerIO()));
		// must be last being added (=first being executed)

		loadControlerListeners();
	}

	/**
	 * Loads the default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide some more basic functionality. Unlike the
	 * core ControlerListeners the order in which the listeners of this method
	 * are added must not affect the correctness of the code.
	 */
	protected void loadControlerListeners() {

		// optional: score stats
		this.scoreStats = new ScoreStatsControlerListener(config, this.population, this.getControlerIO().getOutputFilename(
				Controler.FILENAME_SCORESTATS), this.config.controler().isCreateGraphs());
		this.addControlerListener(this.scoreStats);

	}

	public final Config getConfig() {
		return this.config;
	}

	/**
	 * Loads the Scenario if it was not given in the constructor.
	 */
	protected void loadData() {
		// yyyy cannot make this final since it is overridden about 16 times.
		// kai, jan'13

		if (!this.scenarioLoaded) {
			ScenarioUtils.loadScenario(this.scenarioData);
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
		}
	}
}
