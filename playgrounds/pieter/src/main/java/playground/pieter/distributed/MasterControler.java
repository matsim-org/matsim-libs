package playground.pieter.distributed;


import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import playground.pieter.pseudosimulation.util.CollectionUtils;
import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterControler implements AfterMobsimListener, ShutdownListener, StartupListener, IterationStartsListener {
    private final Logger masterLogger = Logger.getLogger(this.getClass());
    private final HashMap<String, Plan> newPlans = new HashMap<>();
    private int numSlaves;
    private boolean initialRoutingOnSlaves = false;
    private int numberOfPSimIterations = 5;
    private Config config;
    private Controler matsimControler;
    private Slave[] slaves;
    private WaitTimeCalculatorSerializable waitTimeCalculator;
    private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
    private SerializableLinkTravelTimes linkTravelTimes;
    private AtomicInteger numThreads = new AtomicInteger(0);
    private List<PersonSerializable> personPool;
    private int loadBalanceInterval;
    private boolean isLoadBalanceIteration;
    private boolean somethingWentWrong = false;

    private MasterControler(String[] args) throws NumberFormatException, IOException, ParseException {
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption("c", true, "Config file location");
        options.addOption("p", true, "Port number of MasterControler");
        options.addOption("s", false, "Switch to indicate if this is the Singapore scenario, i.e. events-based routing");
        options.addOption("n", true, "Number of slaves to distribute to.");
        options.addOption("i", true, "Number of PSim iterations for every QSim iteration.");
        options.addOption("r", false, "Perform initial routing of plans on slaves.");
        options.addOption("l", true, "Number of iterations between load balancing. Default = 5");
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);
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
        loadBalanceInterval = 5;
        if (commandLine.hasOption("l"))
            try {
                loadBalanceInterval = Integer.parseInt(commandLine.getOptionValue("l"));
            } catch (NumberFormatException e) {
                masterLogger.warn("loadBalanceInterval number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            masterLogger.warn("Will perform load Balancing every 5 iterations as per default");
        }
        if (commandLine.hasOption("r")) {
            masterLogger.warn("ROUTING initial plans on slaves.");
            initialRoutingOnSlaves = true;
        }
        if (commandLine.hasOption("c")) {
            config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));
        } else {
            masterLogger.warn("Config file not specified");
            System.out.println(options.toString());
            System.exit(1);
        }

        ServerSocket writeServer = new ServerSocket(socketNumber);
        for (int i = 0; i < numSlaves; i++) {
            Socket socket = writeServer.accept();
            masterLogger.warn("Slave " + i + " out of " + numSlaves + " accepted.");
            slaves[i] = new Slave(socket, i);

            //order is important
            slaves[i].sendNumber(i);
            slaves[i].sendNumber(numberOfPSimIterations);
            slaves[i].sendBoolean(initialRoutingOnSlaves);
        }
        writeServer.close();

        matsimControler = new Controler(ScenarioUtils.loadScenario(config));
        matsimControler.setOverwriteFiles(true);
        matsimControler.setMobsimFactory(new QSimFactory());
        Set<Id<Person>> ids = matsimControler.getScenario().getPopulation().getPersons().keySet();
        List<Id<Person>>[] split = CollectionUtils.split(ids, numSlaves);

        for (int i = 0; i < numSlaves; i++) {
            List<String> idStrings = new ArrayList<>();
            for (Id id : split[i])
                idStrings.add(id.toString());
            slaves[i].writer.writeObject(idStrings);
            slaves[i].writer.flush();
        }

        if (config.scenario().isUseTransit()) {
//            linkTimeCalculator =
            waitTimeCalculator = new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(), config.travelTimeCalculator().getTraveltimeBinSize(),
                    (int) (config.qsim().getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(waitTimeCalculator);
            stopStopTimeCalculator = new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim()
                    .getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
            //tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;
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
        masterLogger.warn("MASTER inited");
    }

    public static void main(String[] args) {
        MasterControler master = null;
        try {
            master = new MasterControler(args);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            Runtime.getRuntime().halt(0);
        }
        master.run();
        Runtime.getRuntime().halt(0);
    }

    void run() {
        matsimControler.run();
    }

    public void startSlavesInMode(CommunicationsMode mode) {
        if (numThreads.get() > 0)
            masterLogger.warn("All slaves have not finished previous operation but they are being asked to " + mode.toString());
        numThreads = new AtomicInteger(numSlaves);
        for (Slave slave : slaves) {
            slave.communicationsMode = mode;
            new Thread(slave).start();

        }
    }

    public void waitForSlaveThreads() {
        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        if (somethingWentWrong) {
            masterLogger.error("Something went wrong. Exiting.");
            throw new RuntimeException();
        }
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        //wait for previous transmissions to complete, if necessary
        if (initialRoutingOnSlaves) {
            startSlavesInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
            waitForSlaveThreads();
            mergePlansFromSlaves();
            //this code is a copy of the replanning strategy
            for (Person person : matsimControler.getPopulation().getPersons().values()) {
                person.removePlan(person.getSelectedPlan());
                Plan plan = newPlans.get(person.getId().toString());
                person.addPlan(plan);
                person.setSelectedPlan(plan);
            }
            if (numSlaves > 1) loadBalance();
            waitForSlaveThreads();
            startSlavesInMode(CommunicationsMode.CONTINUE);
        }
    }


    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        //wait for previous transmissions to complete, if necessary
        waitForSlaveThreads();
        //start receiving plans from slaves as the QSim runs
        startSlavesInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        //wating for slaves to receive plans from notifyIterationStarts()
        waitForSlaveThreads();
        mergePlansFromSlaves();
        isLoadBalanceIteration = numSlaves > 1 && event.getIteration() % loadBalanceInterval == 0;
        //do load balancing, if necessary
        if (isLoadBalanceIteration)
            loadBalance();
        isLoadBalanceIteration = false;
        waitForSlaveThreads();
        startSlavesInMode(CommunicationsMode.TRANSMIT_TRAVEL_TIMES);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        //start receiving plans from slaves as the QSim runs
        startSlavesInMode(CommunicationsMode.DIE);
    }

    private void loadBalance() {
        waitForSlaveThreads();
        startSlavesInMode(CommunicationsMode.TRANSMIT_PERFORMANCE);
        waitForSlaveThreads();
        personPool = new ArrayList<>();
        masterLogger.warn("About to start load balancing.");
        setSlaveTargetPopulationSizes();
        startSlavesInMode(CommunicationsMode.POOL_PERSONS);
        waitForSlaveThreads();
        mergePersonsFromSlaves();
        masterLogger.warn("Distributing persons between  slaves");
        startSlavesInMode(CommunicationsMode.DISTRIBUTE_PERSONS);
    }

    private void mergePlansFromSlaves() {
        newPlans.clear();
        for (Slave slave : slaves) {
            newPlans.putAll(slave.plans);
        }

    }

    private synchronized List<PersonSerializable> getPersonsFromPool(int diff) throws IndexOutOfBoundsException {
        List<PersonSerializable> outList = new ArrayList<>();
        if (diff < 0) {
            for (int i = 0; i > diff; i--) {
                outList.add(personPool.get(0));
                personPool.remove(0);
            }
        }
        return outList;
    }

    private void mergePersonsFromSlaves() {
        personPool.clear();
        for (Slave loadBalanceThread : slaves) {
            personPool.addAll(loadBalanceThread.getPersons());
        }
    }

    private void setSlaveTargetPopulationSizes() {
        List<Double> timesPerPlan = new ArrayList<>();
        List<Integer> personsPerSlave = new ArrayList<>();
        List<Integer> optimalNumberPerSlave = new ArrayList<>();
        double sumOfReciprocals = 0.0;
        int totalAllocated = 0;
        int largestAllocation = 0;
        int largestAllocationIndex = 0;

        for (Slave slave : slaves) {
            timesPerPlan.add(slave.totalIterationTime / slave.currentPopulationSize);
            personsPerSlave.add(slave.currentPopulationSize);
            sumOfReciprocals += 1 / (slave.totalIterationTime / slave.currentPopulationSize);
        }
//        find number of plans that should be allocated to each slave
        for (int i = 0; i < timesPerPlan.size(); i++) {
            optimalNumberPerSlave.add((int) (matsimControler.getPopulation().getPersons().size() / timesPerPlan.get(i) / sumOfReciprocals));
            totalAllocated += optimalNumberPerSlave.get(i);
            if (optimalNumberPerSlave.get(i) > largestAllocation) {
                largestAllocation = optimalNumberPerSlave.get(i);
                largestAllocationIndex = i;
            }

        }
        int remainder = matsimControler.getPopulation().getPersons().size() - totalAllocated;
        int newval = optimalNumberPerSlave.get(largestAllocationIndex) + remainder;
        optimalNumberPerSlave.set(largestAllocationIndex, newval);
        List<Integer> differences = new ArrayList<>();
        for (int i = 0; i < numSlaves; i++) {
            differences.add(personsPerSlave.get(i) - optimalNumberPerSlave.get(i));
            slaves[i].targetPopulationSize = optimalNumberPerSlave.get(i);
        }
        StringBuffer sb = new StringBuffer();
        String[] lines = {"slave", "time per plan", "pax per slave", "optimum", "diff"};
        for (int j = 0; j < 5; j++) {

            sb.append("\t\t\t" + lines[j] + "\t");
            for (int i = 0; i < optimalNumberPerSlave.size(); i++) {
                switch (j) {
                    case 0:
                        sb.append("slave_" + i + "\t");
                        break;
                    case 1:
                        sb.append(timesPerPlan.get(i) + "\t");
                        break;
                    case 2:
                        sb.append(personsPerSlave.get(i) + "\t");
                        break;
                    case 3:
                        sb.append(optimalNumberPerSlave.get(i) + "\t");
                        break;
                    case 4:
                        sb.append(differences.get(i) + "\t");
                        break;

                }

            }
            sb.append("\n");
        }
        masterLogger.warn(sb.toString());
    }

    private class Slave implements Runnable {
        final Logger slaveLogger = Logger.getLogger(this.getClass());
        final Map<String, Plan> plans = new HashMap<>();
        ObjectInputStream reader;
        ObjectOutputStream writer;
        double totalIterationTime;
        List<PersonSerializable> slavePersonPool;
        int targetPopulationSize = 0;
        CommunicationsMode communicationsMode = CommunicationsMode.TRANSMIT_TRAVEL_TIMES;
        Collection<String> idStrings;
        private int myNumber;
        private int currentPopulationSize;

        public Slave(Socket socket, int i) throws IOException {
            super();
            myNumber = i;
            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.reader = new ObjectInputStream(socket.getInputStream());
        }

        public void transmitPlans() throws IOException, ClassNotFoundException {
            plans.clear();
            slaveLogger.warn("Waiting to receive plans from slave number " + myNumber);
            Map<String, PlanSerializable> serialPlans = (Map<String, PlanSerializable>) reader.readObject();
            slaveLogger.warn("RECEIVED plans from slave number " + myNumber);
            for (Entry<String, PlanSerializable> entry : serialPlans.entrySet()) {
                plans.put(entry.getKey(), entry.getValue().getPlan(matsimControler.getPopulation()));
            }
        }

        public void transmitPerformance() throws IOException {
            totalIterationTime = this.reader.readDouble();
            currentPopulationSize = this.reader.readInt();
        }

        public void transmitTravelTimes() throws IOException {
            slaveLogger.warn("About to send travel times to slave number " + myNumber);
            writer.writeObject(linkTravelTimes);
            if (config.scenario().isUseTransit()) {
                writer.writeObject(stopStopTimeCalculator.getStopStopTimes());
                writer.writeObject(waitTimeCalculator.getWaitTimes());
            }
            writer.flush();
            slaveLogger.warn("SENT travel times to slave number " + myNumber);
        }

        public void poolPersons() throws IOException, ClassNotFoundException {
            slaveLogger.warn("Trying to receive persons from slave " + myNumber);
            slaveLogger.warn("Currently has " + currentPopulationSize + " persons, target is " + targetPopulationSize);
            slavePersonPool = new ArrayList<>();
            writer.writeInt(currentPopulationSize - targetPopulationSize);
            writer.flush();
            slavePersonPool = (List<PersonSerializable>) reader.readObject();
        }

        public void distributePersons() throws IOException, InterruptedException {
            slaveLogger.warn("Distributing persons to slave" + myNumber);
            slavePersonPool = new ArrayList<>();
            writer.writeObject(getPersonsFromPool(currentPopulationSize - targetPopulationSize));
            writer.flush();
        }

        @Override
        public void run() {
            try {
                writer.writeObject(communicationsMode);
                writer.flush();
                switch (communicationsMode) {
                    case TRANSMIT_TRAVEL_TIMES:
                        transmitTravelTimes();
                        reader.readBoolean();
                        communicationsMode = CommunicationsMode.CONTINUE;
                        writer.writeObject(communicationsMode);
                        writer.flush();
                        break;
                    case POOL_PERSONS:
                        poolPersons();
                        break;
                    case DISTRIBUTE_PERSONS:
                        distributePersons();
                        break;
                    case TRANSMIT_PLANS_TO_MASTER:
                        writer.reset();
                        transmitPlans();
                        break;
                    case TRANSMIT_PERFORMANCE:
                        transmitPerformance();
                        break;
                }
                reader.readBoolean();
            } catch (IOException | InterruptedException | IndexOutOfBoundsException | ClassNotFoundException e) {
                e.printStackTrace();
                somethingWentWrong = true;
                numThreads.decrementAndGet();
            }
            //end of a successful Thread.run()
            numThreads.decrementAndGet();
        }


        public void sendNumber(int i) throws IOException {
            writer.writeInt(i);
            writer.flush();
        }


        public Collection<? extends PersonSerializable> getPersons() {
            return slavePersonPool;
        }

        public void sendBoolean(boolean initialRouing) throws IOException {
            writer.writeBoolean(initialRouing);
            writer.flush();
        }
    }


}
