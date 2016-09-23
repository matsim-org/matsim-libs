package playground.sebhoerl.av.model.dispatcher;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;

import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.events.EventListener;
import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.agent.AVFleet;
import playground.sebhoerl.av.logic.service.Service;
import playground.sebhoerl.av.router.AVRouterFactory;
import playground.sebhoerl.av.utils.Grid;

public class HeuristicDispatcher implements Dispatcher {
    enum Mode {
        OVERSUPPLY, UNDERSUPPLY
    }
    
    final private Network network;
    final private EventsManager events;
    final private Config config;
    
    final private Grid<AVAgent> agentGrid;
    final private Grid<Service> serviceGrid;
    
    final private Queue<AVAgent> agentQueue;
    final private Queue<Service> serviceQueue;
    
    private Mode mode = Mode.OVERSUPPLY;
    
    final private ExecutorService executor;
    
    private LinkedList<HeuristicRoutingWorker> routingWorkers = new LinkedList<HeuristicRoutingWorker>();
    private LinkedList<Future<Collection<Service>>> routingTasks = new LinkedList<Future<Collection<Service>>>();
    
    public HeuristicDispatcher(Config config, AVFleet fleet, AVRouterFactory routerFactory, Network network, EventsManager events) {
        this.network = network;
        this.events = events;
        this.config = config;
        
        this.executor = Executors.newFixedThreadPool(config.getNumberOfRouters());
        
        for (int i = 0; i < config.getNumberOfRouters(); i++) {
            routingWorkers.add(new HeuristicRoutingWorker(routerFactory.createRouter()));
        }
        
        double bounds[] = NetworkUtils.getBoundingBox(network.getNodes().values());

        agentGrid = new Grid<AVAgent>(bounds[0], bounds[1], bounds[2], bounds[3], config.getGridX(), config.getGridY());
        serviceGrid = new Grid<Service>(bounds[0], bounds[1], bounds[2], bounds[3], config.getGridX(), config.getGridY());
        
        agentQueue = new LinkedList<>();
        serviceQueue = new LinkedList<>();
        
        for (AVAgent agent : fleet.getAgents().values()) {
            updateAgentGrid(agent, agent.getCurrentLinkId());
            agentQueue.add(agent);
        }
    }
    
    private HeuristicRoutingWorker getNextWorker() {
        HeuristicRoutingWorker next = routingWorkers.poll();
        routingWorkers.offer(next);
        return next;
    }
    
    private void updateAgentGrid(AVAgent agent, Id<Link> linkId) {
        Coord coord = network.getLinks().get(linkId).getCoord();
        agentGrid.update(agent, coord.getX(), coord.getY());
    }
    
    private void updateServiceGrid(Service service, Id<Link> linkId) {
        Coord coord = network.getLinks().get(linkId).getCoord();
        serviceGrid.update(service, coord.getX(), coord.getY());
    }
    
    private AVAgent getClosestAgent(Service service) {
        Coord coord = network.getLinks().get(service.getRequest().getPickupLinkId()).getCoord();
        Collection<AVAgent> agents = agentGrid.getClosest(coord.getX(), coord.getY(), 1);
        return agents.size() == 0 ? null : agents.iterator().next();
        
    }
    
    private Service getClosestService(AVAgent agent) {
        Coord coord = network.getLinks().get(agent.getCurrentLinkId()).getCoord();
        Collection<Service> services = serviceGrid.getClosest(coord.getX(), coord.getY(), 1);
        return services.size() == 0 ? null : services.iterator().next();
    }
    
    @Override
    public void handle(Service service) {
        updateServiceGrid(service, service.getRequest().getPickupLinkId());
        serviceQueue.add(service);
    }
    
    private void finishService(AVAgent agent) {
        updateAgentGrid(agent, agent.getCurrentLinkId());
        agentQueue.add(agent);
    }
    

    private void processAssignment(final Service service, final AVAgent agent, double now) {
        // Cleanup structures
        serviceGrid.remove(service);
        agentGrid.remove(agent);
        
        serviceQueue.remove(service);
        agentQueue.remove(agent);
        
        // Prepare
        service.setStartLinkId(agent.getCurrentLinkId());
        service.setDriverAgent(agent);
        service.setDispatchmentTime(now);
        
        // Routing
        getNextWorker().addService(service);
    }
    
    void processAvailableServices(double now) {
        AVAgent agent = null;
        Service service = null;
        
        while (agentQueue.size() > 0 && serviceQueue.size() > 0) {
            service = null;
            agent = null;
            
            switch (mode) {
                case OVERSUPPLY:
                    service = serviceQueue.poll();
                    agent = getClosestAgent(service);
                    break;
                case UNDERSUPPLY:
                    agent = agentQueue.poll();
                    service = getClosestService(agent);
                    break;
            }
            
            if (service != null && agent != null) {
                processAssignment(service, agent, now);
            } else {
                break;
            }
        }
    }

    @Override
    public Collection<Service> processServices(double now) {
        LinkedList<Service> dispatched = new LinkedList<>(); 
        
        for (Future<Collection<Service>> task : routingTasks) {
            try {
                dispatched.addAll(task.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException("Error while prcessing AV services");
            }
        }
        
        for (final Service service : dispatched) {
            service.getFinishTaskEvent().addListener(new EventListener() {
                @Override
                public void notifyEvent(Event event) {
                    finishService(service.getDriverAgent());
                }
            });
            
            service.getDriverAgent().handleService(service);
        }
        
        processAvailableServices(now);
        
        Mode updatedMode = agentQueue.size() > 0 ? Mode.OVERSUPPLY : Mode.UNDERSUPPLY;
        
        if (config.getForceMode() != null) {
            updatedMode = config.getForceMode();
        }
        
        if (!updatedMode.equals(mode)) {
            events.processEvent(new ModeChangeEvent(now, updatedMode));
            mode = updatedMode;
        }
        
        routingTasks.clear();
        
        for (HeuristicRoutingWorker worker : routingWorkers) {
            routingTasks.add(executor.submit(worker));
        }
        
        return dispatched;
    }
    
    @Override
    public void shutdown() {
        executor.shutdown();
    }
    
    static public class Config {
        private Mode forceMode = null;
        private int gridX = 100;
        private int gridY = 100;
        private int numberOfRouters = 1;
        
        public Mode getForceMode() {
            return forceMode;
        }

        public void setForceMode(Mode forceMode) {
            this.forceMode = forceMode;
        }

        public int getGridX() {
            return gridX;
        }

        public void setGridX(int gridX) {
            this.gridX = gridX;
        }

        public int getGridY() {
            return gridY;
        }

        public void setGridY(int gridY) {
            this.gridY = gridY;
        }
        
        public int getNumberOfRouters() {
            return numberOfRouters;
        }
        
        public void setNumberOfRouters(int numberOfRouters) {
            this.numberOfRouters = numberOfRouters;
        }
    }
    
    static public void loadConfig(Config config, ConfigGroup cfg) {
        Map<String, String> params = cfg.getParams();
        
        if (params.containsKey("mode")) {
            switch (params.get("mode")) {
            case "oversupply":
                config.setForceMode(Mode.OVERSUPPLY);
                break;
            case "undersupply":
                config.setForceMode(Mode.UNDERSUPPLY);
                break;
            case "adaptive":
                config.setForceMode(null);
                break;
            default:
                throw new IllegalArgumentException("Invalid dispatch mode for HeuristicDispatcher");
            }
        }
        
        if (params.containsKey("numberOfRouters")) {
            config.setNumberOfRouters(Integer.parseInt(params.get("numberOfRouters")));
        }
        
        Integer gridX = null;
        Integer gridY = null;
        
        if (params.containsKey("gridX")) {
            gridX = Integer.decode(params.get("gridX"));
        }
        
        if (params.containsKey("gridY")) {
            gridY = Integer.decode(params.get("gridY"));
        }
        
        if (gridX != null && gridY != null) {
            config.setGridX(gridX);
            config.setGridY(gridY);
        } else if ((gridX != null) ^ (gridY != null)) {
            throw new IllegalArgumentException("Parameters grid_x and grid_y must both be defined!");
        }
    }

    static class ModeChangeEvent extends org.matsim.api.core.v01.events.Event {
        private Mode mode;
        
        public ModeChangeEvent(double time, Mode mode) {
            super(time);
            this.mode = mode;
        }

        @Override
        public String getEventType() {
            return "AVDispatchModeChange";
        }
        
        @Override
        public Map<String, String> getAttributes() {
            Map<String, String> attr = super.getAttributes();
            attr.put("mode", mode.toString());
            return attr;
        }
    }
}
