package playground.pieter.distributed;


import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import playground.pieter.pseudosimulation.util.CollectionUtils;
import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class RepeatableMasterControler implements AfterMobsimListener, ShutdownListener {
    private int numberOfPSimIterations = 5;
    private Config config;
    private final Logger masterLogger = Logger.getLogger(this.getClass());
    private Controler matsimControler;
    private Slave[] slaves;
    private WaitTimeCalculatorSerializable waitTimeCalculator;
    private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
    private SerializableLinkTravelTimes linkTravelTimes;
    private AtomicInteger numThreads;
    private final HashMap<String, Plan> newPlans = new HashMap<>();

    private RepeatableMasterControler(String[] args) throws NumberFormatException, IOException, ParseException {
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption("c", true, "Config file location");
        options.addOption("p", true, "Port number of MasterControler");
        options.addOption("s", false, "Switch to indicate if this is the Singapore scenario, i.e. events-based routing");
        options.addOption("n", true, "Number of slaves to distribute to.");
        options.addOption("i", true, "Number of PSim iterations for every QSim iteration.");
        options.addOption("r", false, "Perform initial routing of plans on slaves.");
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);
        int numSlaves = 0;
        if (commandLine.hasOption("n"))
            numSlaves = Integer.parseInt(commandLine.getOptionValue("n"));
        else {
            System.err.println("Unspecified number of slaves");
            System.out.println(options.toString());
            System.exit(1);
        }
        if (commandLine.hasOption("i")) {
            numberOfPSimIterations = Integer.parseInt(commandLine.getOptionValue("i"));
            masterLogger.warn("Running  " + numberOfPSimIterations + " PSim iterations for every QSim iteration run on the master");
        } else {
            masterLogger.warn("Unspecified number of PSim iterations for every QSim iteration run on the master.");
            masterLogger.warn("Using default value of " + numberOfPSimIterations);
        }
        slaves = new Slave[numSlaves];
        int socketNumber = 12345;
        if (commandLine.hasOption("p"))
            try {
                socketNumber = Integer.parseInt(commandLine.getOptionValue("p"));
            } catch (NumberFormatException e) {
                masterLogger.warn("Port number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            masterLogger.warn("Will accept connections on default port number 12345");
        }
        ServerSocket server = new ServerSocket(socketNumber);
        for (int i = 0; i < numSlaves; i++) {
            Socket s = server.accept();
            masterLogger.warn("Slave " + i + " out of " + numSlaves + " accepted.");
            slaves[i] = new Slave(s, i);
            //order is important
            slaves[i].sendNumber(i);
            slaves[i].sendNumber(numberOfPSimIterations);
        }
        server.close();
        if (commandLine.hasOption("c")) {
            config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));
            matsimControler = new Controler(ScenarioUtils.loadScenario(config));
        } else {
            masterLogger.warn("Config file not specified");
            System.out.println(options.toString());
            System.exit(1);
        }

        matsimControler.setOverwriteFiles(true);
        matsimControler.setMobsimFactory(new QSimFactory());
        Set<Id<Person>> ids = matsimControler.getScenario().getPopulation().getPersons().keySet();
        List<Id<Person>>[] split = CollectionUtils.split(ids, numSlaves);
        for (int i = 0; i < numSlaves; i++) {
            List<String> idStrings = new ArrayList<>();
            for (Id id : split[i])
                idStrings.add(id.toString());
            slaves[i].sendIds(idStrings);
        }
        if (config.scenario().isUseTransit()) {
            waitTimeCalculator = new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(), config.travelTimeCalculator().getTraveltimeBinSize(),
                    (int) (config.qsim().getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(waitTimeCalculator);
            stopStopTimeCalculator = new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim()
                    .getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
        }

        matsimControler.addPlanStrategyFactory("ReplacePlanFromSlave", new ReplacePlanFromSlaveFactory(newPlans));
        matsimControler.addControlerListener(this);
        if (commandLine.hasOption("s")) {
            masterLogger.warn("Singapore scenario: Doing events-based transit routing.");
            //our scoring function
            matsimControler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(config.planCalcScore(), matsimControler.getScenario()));
            //this qsim engine uses our boarding and alighting model, derived from smart card data
            matsimControler.setMobsimFactory(new PTQSimFactory());
        }
        if(commandLine.hasOption("r")) {
        //initialize link travel times if you want to do remote routing
            masterLogger.warn("ROUTING initial plans on slaves; received plans will be subjected to " + numberOfPSimIterations +
                    " PSim iterations before executing on master.");
            FreespeedTravelTimeAndDisutility disutility = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
            linkTravelTimes = new SerializableLinkTravelTimes(disutility, config
                    .travelTimeCalculator().getTraveltimeBinSize(), config.qsim().getEndTime(), matsimControler
                    .getNetwork().getLinks().values());
            numThreads = new AtomicInteger(slaves.length);
            for (Slave slave : slaves) new Thread(slave).start();
            while (numThreads.get() > 0)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            mergePlansFromSlaves();
        }
        masterLogger.warn("master inited");
    }

    public static void main(String[] args)   {
        RepeatableMasterControler master = null;
        try {
            master = new RepeatableMasterControler(args);
        } catch (IOException  | ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        master.run();
    }

    public Controler getMATSimControler() {
        return matsimControler;
    }

    void run() {
        matsimControler.run();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        linkTravelTimes = new SerializableLinkTravelTimes(event.getControler().getLinkTravelTimes(), config
                .travelTimeCalculator().getTraveltimeBinSize(), config.qsim().getEndTime(), matsimControler
                .getNetwork().getLinks().values());
        numThreads = new AtomicInteger(slaves.length);
        for (Slave slave : slaves) new Thread(slave).start();
        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        mergePlansFromSlaves();
    }

    private void mergePlansFromSlaves() {
        newPlans.clear();
        for (Slave slave : slaves) {
            newPlans.putAll(slave.plans);
        }

    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        for (Slave slave : slaves) {
            try {
                slave.shutdown();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class Slave implements Runnable {
        final Logger slaveLogger = Logger.getLogger(this.getClass());
        ObjectInputStream reader;
        ObjectOutputStream writer;
        final Map<String, Plan> plans = new HashMap<>();
        private int myNumber;

        public Slave(Socket socket, int i) throws IOException {
            super();
            myNumber = i;
            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.reader = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                slaveLogger.warn("About to send travel times to slave number " + myNumber);
                writer.writeBoolean(true);
                writer.writeObject(linkTravelTimes);
                if (config.scenario().isUseTransit()) {
                    writer.writeObject(stopStopTimeCalculator.getStopStopTimes());
                    writer.writeObject(waitTimeCalculator.getWaitTimes());
                }
                slaveLogger.warn("SENT travel times to slave number " + myNumber);
                slaveLogger.warn("waiting to receive plans from slave number " + myNumber);
                Map<String, PlanSerializable> serialPlans = (Map<String, PlanSerializable>) reader.readObject();
                slaveLogger.warn("RECEIVED plans from slave number " + myNumber);
                for (Entry<String, PlanSerializable> entry : serialPlans.entrySet()) {
                    plans.put(entry.getKey(), entry.getValue().getPlan(matsimControler.getPopulation()));
                }
                numThreads.decrementAndGet();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

        public void sendIds(Collection<String> idStrings) throws IOException {
            slaveLogger.warn("Sending ids.");
            writer.writeObject(idStrings);
        }

        public void shutdown() throws IOException {
            // kills the slave
            writer.writeBoolean(false);
        }

        public void sendNumber(int i) throws IOException {
            writer.writeInt(i);
        }

    }
}
