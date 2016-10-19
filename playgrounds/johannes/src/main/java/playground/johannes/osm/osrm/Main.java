package playground.johannes.osm.osrm; /**
 * Created by NicoKuehnel on 03.08.2016.
 */

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


public class Main {

    private double minY = Double.POSITIVE_INFINITY;
    private double minX = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;

    private HttpClient httpclient = HttpClients.createDefault();
    private URIBuilder uriBuilder = new URIBuilder()
            .setScheme("http")
            .setHost("127.0.0.1:5000")
            .setParameter("annotations", "true");

    private Network network;
    private Map<Node, Node> fromToNodes;
    private LeastCostPathCalculator dijkstraCalculator;
    private LeastCostPathCalculator aStarCalculator;
    private Person person;
    private Vehicle vehicle;
    private QuadTree<Node> quad;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.setup(args[0]);
        main.run();
    }

    private void setup(String networkPath) {

        vehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId(0), VehicleUtils.getDefaultVehicleType());
        person = PopulationUtils.getFactory().createPerson(Id.createPersonId(0));

        fromToNodes = new HashMap<>();

        network = NetworkUtils.createNetwork();
        MatsimNetworkReader networkReader = new MatsimNetworkReader(this.network);
        networkReader.readFile(networkPath);

        TravelDisutility disutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(new PlanCalcScoreConfigGroup());

        DijkstraFactory dijkstraFactory = new DijkstraFactory();
        AStarEuclideanFactory aStarFactory = new AStarEuclideanFactory(network, disutility);
        dijkstraCalculator = new Dijkstra(network, disutility, new FreeSpeedTravelTime());
        dijkstraFactory.createPathCalculator(network, disutility, new FreeSpeedTravelTime());
        aStarCalculator = aStarFactory.createPathCalculator(network, disutility, new FreeSpeedTravelTime());

        setBounds();

        initializeQuadTree();

        mapFromToNodes();
    }

    /**
     * Sets the bounds to be used in the quadtree. Generated from spatial spread of all stops.
     */
    private void setBounds() {
        for (Node node : this.network.getNodes().values()) {
            if (node.getCoord().getX() > maxX) {
                maxX = node.getCoord().getX();
            }
            if (node.getCoord().getX() < minX) {
                minX = node.getCoord().getX();
            }
            if (node.getCoord().getY() > maxY) {
                maxY = node.getCoord().getY();
            }
            if (node.getCoord().getY() < minY) {
                minY = node.getCoord().getY();
            }
        }
    }

    private void initializeQuadTree() {
        quad = new QuadTree<>(minX, minY, maxX, maxY);
        for (Node node : this.network.getNodes().values()) {
            quad.put(node.getCoord().getX(), node.getCoord().getY(), node);
        }
    }

    private void mapFromToNodes() {

        Random rnd = new Random();
        rnd.setSeed(42);

        for (int i = 0; i < 100; i++) {
            List<Node> nodes = new ArrayList<>(this.network.getNodes().values());
            Node nodeFrom = nodes.get(rnd.nextInt(this.network.getNodes().size()));
            //get nodes in given distance for random paths in a set vicinity
//            List<Node> nearestNodes = new ArrayList<Node>(quad.getRing(nodeFrom.getCoord().getX(), nodeFrom.getCoord().getY(), 100000,1000000));
//            Node nodeTo = nearestNodes.get(rnd.nextInt(nearestNodes.size()));
            Node nodeTo = nodes.get((int) (rnd.nextDouble() * this.network.getNodes().size()));
            this.fromToNodes.put(nodeFrom, nodeTo);
        }
    }


    private void run() {

        try {
            //example route
//            getTestRoute();

            //measure runtime difference MATSim Dijkstra - OSRM
//            diffOsrmDijkstra();

            //measure runtime difference MATSim Dijkstra - A*
            diffDijkstraAStar();

            //measure runtime of osrm query single threaded
//            double singleRuntime = measureSingleRuntime();

            //measure runtime of osrm query multithreaded
//            double threadRuntime = measureThreadRuntime(2);
//            System.out.println("Threading runtime: " + threadRuntime + " | Single: " + singleRuntime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getTestRoute() throws Exception {

        uriBuilder.setPath("/route/v1/driving/13.3878,51.7262;13.4218,51.7221");
        URI uri = uriBuilder.build();
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            parseJSONObject(EntityUtils.toString(entity));
        }
    }

    private void diffOsrmDijkstra() {
        Map<Integer, Measurement> measurements = new HashMap<>();
        int i = 0;
        for (Map.Entry<Node, Node> entry : this.fromToNodes.entrySet()) {
            double osrmTime = 0;
            double matsimTime;
            try {
                osrmTime = getHTTPRoute(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
            matsimTime = getMatsimRouteDikstra(entry.getKey(), entry.getValue());
            System.out.println("Route " + i + " OSRM time: " + osrmTime + " | MATSim time: " + matsimTime);
            measurements.put(i, new Measurement(matsimTime, osrmTime));
            i++;
        }

        double averageMatsim = 0;
        double averageOsrm = 0;
        for (Map.Entry<Integer, Measurement> entry : measurements.entrySet()) {
            System.out.println(entry.getKey() + "| MATSim: " + entry.getValue().matsim + " | OSRM: " + entry.getValue().osrm);
            averageMatsim += entry.getValue().matsim;
            averageOsrm += entry.getValue().osrm;
        }
        System.out.println("Average MATSim: " + averageMatsim / measurements.size() + " | Average OSRM: " + averageOsrm / measurements.size());

    }

    private void diffDijkstraAStar() {
        Map<Integer, Measurement> measurements = new HashMap<>();
        int i = 0;
        for (Map.Entry<Node, Node> entry : this.fromToNodes.entrySet()) {
            double dijkstraTime = 0;
            double aStarTime;
            dijkstraTime = getMatsimRouteDikstra(entry.getKey(), entry.getValue());
            aStarTime = getMatsimRouteAStar(entry.getKey(), entry.getValue());
            System.out.println("Route " + i + " Dijkstra time: " + dijkstraTime + " | A* time: " + aStarTime);
            measurements.put(i, new Measurement(dijkstraTime, aStarTime));
            i++;
        }

        double averageDijkstra = 0;
        double averageAStar = 0;
        for (Map.Entry<Integer, Measurement> entry : measurements.entrySet()) {
            System.out.println(entry.getKey() + "| Dijkstra: " + entry.getValue().matsim + " | A*: " + entry.getValue().osrm);
            averageDijkstra += entry.getValue().matsim;
            averageAStar += entry.getValue().osrm;
        }
        System.out.println("Average Dijkstra: " + averageDijkstra / measurements.size() + " | Average A*: " + averageAStar / measurements.size());

    }

    private double measureThreadRuntime(int nThreads) throws Exception {

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);

        List<Runnable> querys = new ArrayList<>();
        for (final Map.Entry<Node, Node> entry : this.fromToNodes.entrySet()) {
            Runnable runnable = () -> {
                try {
                    getHTTPRoute(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            querys.add(runnable);
        }

        List<Future<?>> futures = new ArrayList<>(querys.size());
        querys.forEach(r -> futures.add(service.submit(r)));

        double time = System.currentTimeMillis();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return (System.currentTimeMillis() - time);

    }

    private double measureSingleRuntime() throws Exception {

        double timeSum = 0;
        for (final Map.Entry<Node, Node> entry : this.fromToNodes.entrySet()) {
            double time = System.currentTimeMillis();
            try {
                getHTTPRoute(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
            timeSum += (System.currentTimeMillis() - time);
        }
        return (timeSum);
    }

    // HTTP GET request
    private double getHTTPRoute(Node nodeFrom, Node nodeTo) {
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:31467", TransformationFactory.WGS84);
        HttpClient httpclient = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder()
                .setScheme("http")
                .setHost("127.0.0.1:5000")
                .setParameter("annotations", "true");


        Coord fromCoord = transformation.transform(nodeFrom.getCoord());
        Coord toCoord = transformation.transform(nodeTo.getCoord());
        uriBuilder.setPath("/route/v1/driving/" + fromCoord.getX() + "," + fromCoord.getY() + ";" + toCoord.getX() + "," + toCoord.getY());
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        double time = System.currentTimeMillis();
        HttpGet httpget = new HttpGet(uri);
        time = System.currentTimeMillis() - time;
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                parseJSONObject(EntityUtils.toString(entity));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return time;
    }

    private void parseJSONObject(String jsonString) throws Exception {

        StringBuilder stB = new StringBuilder();
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(jsonString);
        JSONArray routes = (JSONArray) root.get("routes");
        for (Object routeX : routes) {
            JSONObject route = (JSONObject) routeX;
            JSONArray legs = (JSONArray) route.get("legs");
            for (Object legX : legs) {
                JSONObject leg = (JSONObject) legX;
                JSONObject annotation = (JSONObject) leg.get("annotation");
                JSONArray nodes = (JSONArray) annotation.get("nodes");
                for (Object node : nodes) {
                    stB.append(node).append(" ");
                }
            }
        }
        System.out.println(stB.toString());
    }

    private double getMatsimRouteDikstra(Node from, Node to) {
        double time = System.currentTimeMillis();
        LeastCostPathCalculator.Path path = dijkstraCalculator.calcLeastCostPath(from, to, 0, person, vehicle);
        time = System.currentTimeMillis() - time;
        System.out.println(path.nodes);
        return time;
    }

    private double getMatsimRouteAStar(Node from, Node to) {
        double time = System.currentTimeMillis();
        LeastCostPathCalculator.Path path = aStarCalculator.calcLeastCostPath(from, to, 0, person, vehicle);
        time = System.currentTimeMillis() - time;
        System.out.println(path.nodes);
        return time;
    }

    private class Measurement {

        double matsim;
        double osrm;

        Measurement(double matsim, double osrm) {
            this.matsim = matsim;
            this.osrm = osrm;
        }
    }
}