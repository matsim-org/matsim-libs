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
    private LoadBalancer loadBalancer;

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
        loadBalancer = new LoadBalancer();
        ServerSocket server = new ServerSocket(socketNumber);
        for (int i = 0; i < numSlaves; i++) {
            Socket s = server.accept();
            masterLogger.warn("Slave " + i + " out of " + numSlaves + " accepted.");
            slaves[i] = new Slave(s, i);

            //order is important
            slaves[i].sendNumber(i);
            slaves[i].sendNumber(numberOfPSimIterations);
            loadBalancer.addSlave(slaves[i]);
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
        if (commandLine.hasOption("r")) {
            //initialize link travel times if you want to do remote routing
            masterLogger.warn("ROUTING initial plans on slaves.");
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

    public static void main(String[] args) {
        RepeatableMasterControler master = null;
        try {
            master = new RepeatableMasterControler(args);
        } catch (IOException | ParseException e) {
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
        masterLogger.warn("Plans from al slaves merged together. About to start load balancing.");
        List<Integer> loadBalanceNumbers = loadBalanceNumbers();
        masterLogger.warn("loadbalanceNumbers() returns: \n" + loadBalanceNumbers.toString());
        loadBalancer.balance(loadBalanceNumbers);

    }

    private void mergePlansFromSlaves() {
        newPlans.clear();
        for (Slave slave : slaves) {
            newPlans.putAll(slave.plans);
        }

    }

    private List<Integer> loadBalanceNumbers() {
        List<Double> timesPerPlan = new ArrayList<>();
        List<Integer> personsPerSlave = new ArrayList<>();
        List<Integer> optimalNumberPerSlave = new ArrayList<>();
        double sumOfReciprocals = 0.0;
        int totalAllocated = 0;
        int largestAllocation = 0;
        int largestAllocationIndex = 0;

        for (Slave slave : slaves) {
            timesPerPlan.add(slave.averageIterationTime / slave.plans.size());
            personsPerSlave.add(slave.plans.size());
            sumOfReciprocals += slave.averageIterationTime / slave.plans.size();
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
        for (int i = 0; i < optimalNumberPerSlave.size(); i++) {
            differences.add(personsPerSlave.get(i) - optimalNumberPerSlave.get(i));
        }
        return differences;
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
        double averageIterationTime;

        public Slave(Socket socket, int i) throws IOException {
            super();
            myNumber = i;
            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.reader = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                writer.writeBoolean(true);
                averageIterationTime = this.reader.readDouble();
                slaveLogger.warn("About to send travel times to slave number " + myNumber);
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

    private class LoadBalancer {

        private List<PersonSerializable> personPool = new LinkedList<>();
        private List<LoadBalancingThread> loadBalanceThreads = new ArrayList<>();
        private AtomicInteger loadBalanceNumThreads;
        Logger loadBalanceLogger = Logger.getLogger(this.getClass());

        public LoadBalancer() {
            loadBalanceThreads = new ArrayList<>();
        }

        public void addSlave(Slave slave) {
            loadBalanceThreads.add(new LoadBalancingThread(slave));
        }

        public void balance(List<Integer> loadBalanceNumbers) {
            loadBalanceLogger.warn("Starting load balancing...");
            loadBalanceNumThreads = new AtomicInteger(loadBalanceThreads.size());
            for (int i = 0; i < loadBalanceThreads.size(); i++) {
                loadBalanceThreads.get(i).sendTarget=true;
                loadBalanceThreads.get(i).setPool(null);
                loadBalanceThreads.get(i).target = loadBalanceNumbers.get(i);
                new Thread(loadBalanceThreads.get(i)).start();
            }
            while (numThreads.get() > 0)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            mergePersonsFromSlaves();
            //run again to distribute to slaves
            for (int i = 0; i < loadBalanceThreads.size(); i++) {
                loadBalanceThreads.get(i).sendTarget=false;
                loadBalanceThreads.get(i).setPool(this.getFromPool(loadBalanceNumbers.get(i)));
                new Thread(loadBalanceThreads.get(i)).start();
            }
            while (numThreads.get() > 0)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

        }

        private List<PersonSerializable> getFromPool(int diff) {
            List<PersonSerializable> outList = new ArrayList<>();
            if (diff < 0) {
                for (int i = 0; i > diff; i--) {
                    outList.add(personPool.get(0));
                    personPool.remove(0);
                }
                return outList;
            } else {
                return null;
            }

        }

        private void mergePersonsFromSlaves() {
            personPool.clear();
            for (LoadBalancingThread loadBalanceThread : loadBalanceThreads) {
                personPool.addAll(loadBalanceThread.getPersons());
            }


        }
    }


    class LoadBalancingThread implements Runnable {
        Slave mySlave;
        Integer target = 0;
        List<PersonSerializable> personPool = null;
        public boolean sendTarget =false;
        Logger lbtLogger = Logger.getLogger(this.getClass());
        public LoadBalancingThread(Slave slave) {
            mySlave = slave;
        }

        @Override
        public void run() {
            lbtLogger.warn("Starting load balancing on thread " + mySlave.myNumber);
                    lbtLogger.warn("Target is " + target);
            try {
                if(sendTarget)
                mySlave.writer.writeInt(target);
            if (target > 0) {
               //getr persons
                personPool = (List<PersonSerializable>) mySlave.reader.readObject();
            }
            if(target <0 && !sendTarget){
                mySlave.writer.writeObject(personPool);
            }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public List<PersonSerializable> getPersons() {
            return personPool;
        }

        public void setPool(List<PersonSerializable> personPool) {
            this.personPool=personPool;
        }
    }

}
