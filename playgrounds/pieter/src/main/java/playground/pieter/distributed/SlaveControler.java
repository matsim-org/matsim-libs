package playground.pieter.distributed;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;

import org.matsim.core.utils.io.UncheckedIOException;
import playground.pieter.pseudosimulation.mobsim.PSimFactory;
import playground.singapore.transitRouterEventsBased.TransitRouterWSImplFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTime;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTime;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
//IMPORTANT: PSim produces events that are not in chronological order. This controler
// will require serious overhaul if chronological order is enforced in all event manager implementations
public class SlaveControler implements IterationStartsListener, BeforeMobsimListener {
    private Scenario scenario;
    private Config config;

    class TimesReceiver implements Runnable {
        Logger timesLogger = Logger.getLogger(this.getClass());

        @Override
        public void run() {
            while (true) {
                boolean res = false;
                try {
                    timesLogger.warn("trying to read boolean from master");
                    res = reader.readBoolean();
                } catch (IOException e) {
                    System.out.println("Master terminated. Exiting.");
                    System.exit(0);
                }
                try {
                    if (res) {
                        timesLogger.warn("Receiving travel times.");
                        linkTravelTimes = (SerializableLinkTravelTimes) reader.readObject();
                        stopStopTimes = (StopStopTime) reader.readObject();
                        waitTimes = (WaitTime) reader.readObject();
                        timesLogger.warn("Checking to see if plans are ready to be sent");
                        while (!readyToSendPlans)
                            ;
                        timesLogger.warn("Sending plans...");
                        writer.writeObject(plansCopyForSending);
                        timesLogger.warn("Sending completed.");
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

    Controler matsimControler;
    Logger slaveLogger = Logger.getLogger(this.getClass());
    private SerializableLinkTravelTimes linkTravelTimes;
    private WaitTime waitTimes;
    private StopStopTime stopStopTimes;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private PSimFactory pSimFactory;
    private Map<String, PlanSerializable> plansCopyForSending;
    private boolean readyToSendPlans = false;


    public SlaveControler(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, ParseException {
        Options options = new Options();
        options.addOption("c", true, "Config file location");
        options.addOption("h", true, "Host name or IP");
        options.addOption("p", true, "Port number of MasterControler");
        options.addOption("s", false, "Switch to indicate if this is the Singapore scenario, i.e. events-based routing");
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("c")) {
            try{
            config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));

            }catch(UncheckedIOException e){
                System.err.println("Config file not found");
                System.out.println(options.toString());
                System.exit(1);
            }
        }
        else {
            System.err.println("Config file not specified");
            System.out.println(options.toString());
            System.exit(1);
        }

        Socket socket;
        int socketNumber = 12345;
        String hostname = "localhost";
        if (commandLine.hasOption("h")) {
            hostname = commandLine.getOptionValue("h");
        } else
            System.err.println("No host specified, using default (localhost)");
        if (commandLine.hasOption("p"))
            try {
                socketNumber = Integer.parseInt(commandLine.getOptionValue("p"));
            } catch (NumberFormatException e) {
                System.err.println("Port number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            System.err.println("Will accept connections on default port number 12345");
        }
        socket = new Socket(hostname, socketNumber);
        this.reader = new ObjectInputStream(socket.getInputStream());
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        int myNumber = reader.readInt();
        slaveLogger.warn("About to receive agent ids for removal from master");
        List<String> idStrings = (List<String>) reader.readObject();
        slaveLogger.warn("RECEIVED agent ids for removal from master. Starting TimesReceiver thread.");
//      The following line will make the controler use the events manager that doesn't check for event order
        config.parallelEventHandling().setSynchronizeOnSimSteps(false);
        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "_" + myNumber);
        scenario = ScenarioUtils.createScenario(config);
        new SlaveScenarioLoaderImpl(scenario).loadScenario(idStrings);
        matsimControler = new Controler(scenario);
        matsimControler.setOverwriteFiles(true);
        matsimControler.setCreateGraphs(false);
        matsimControler.addControlerListener(this);
        new Thread(new TimesReceiver()).start();
        if (commandLine.hasOption("s")) {
            slaveLogger.warn("Singapore scenario: Doing events-based transit routing.");
            matsimControler.setTransitRouterFactory(new TransitRouterWSImplFactory(matsimControler.getScenario(), waitTimes, stopStopTimes));
            matsimControler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(matsimControler.getScenario().getConfig().planCalcScore(), matsimControler.getScenario()));
        }
    }

    private void removeNonSimulatedAgents(List<String> idStrings) {
        Set<Id<Person>> noIds = new HashSet<>(matsimControler.getPopulation().getPersons().keySet());
        Set<String> noIdStrings = new HashSet<>();
        for (Id<Person> id : noIds)
            noIdStrings.add(id.toString());
        noIdStrings.removeAll(idStrings);
        slaveLogger.warn("removing ids");
        for (String idString : noIdStrings) {
            matsimControler.getPopulation().getPersons().remove(Id.create(idString, Person.class));
        }

    }

    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, ParseException {
        SlaveControler slave = new SlaveControler(args);
        slave.run();
    }

    private void run() {
        pSimFactory = new PSimFactory();
        matsimControler.setMobsimFactory(pSimFactory);
        // Collection<Plan> plans = new ArrayList<>();
        // for (Person person :
        // matsimControler.getPopulation().getPersons().values())
        // plans.add(person.getSelectedPlan());
        // // pSimFactory.setPlans(plans);
        matsimControler.run();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        // the time calculators are null until the master sends them, need
        // something to keep psim occupied until then
//        pSimFactory = new PSimFactory();
        if (linkTravelTimes == null)
            pSimFactory.setTravelTime(matsimControler.getLinkTravelTimes());
        else
            pSimFactory.setTravelTime(linkTravelTimes);
        if (matsimControler.getConfig().scenario().isUseTransit()) {
            if (stopStopTimes == null)
                pSimFactory.setStopStopTime(new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
                        matsimControler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (matsimControler.getConfig()
                        .qsim().getEndTime() - matsimControler.getConfig().qsim().getStartTime())).getStopStopTimes());
            else
                pSimFactory.setStopStopTime(stopStopTimes);
            if (waitTimes == null)
                pSimFactory.setWaitTime(new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
                        matsimControler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (matsimControler.getConfig()
                        .qsim().getEndTime() - matsimControler.getConfig().qsim().getStartTime())).getWaitTimes());
            else
                pSimFactory.setWaitTime(waitTimes);
        }
        Collection<Plan> plans = new ArrayList<>();
        for (Person person : matsimControler.getPopulation().getPersons().values())
            plans.add(person.getSelectedPlan());
        pSimFactory.setPlans(plans);
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        createPlansCopyForMaster();
    }

    public void createPlansCopyForMaster() {
        Map<String, PlanSerializable> tempPlansCopyForSending = new HashMap<>();
        for (Person person : matsimControler.getPopulation().getPersons().values())
            tempPlansCopyForSending.put(person.getId().toString(), new PlanSerializable(person.getSelectedPlan()));
        plansCopyForSending = tempPlansCopyForSending;
        readyToSendPlans = true;

    }

}
