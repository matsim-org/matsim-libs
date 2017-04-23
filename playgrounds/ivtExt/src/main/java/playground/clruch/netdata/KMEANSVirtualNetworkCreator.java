package playground.clruch.netdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import playground.clruch.utils.GlobalAssert;

public class KMEANSVirtualNetworkCreator extends AbstractVirtualNetworkCreator {

    Random random = new Random();
    DatabaseConnection dbc;
    Database db;
    SquaredEuclideanDistanceFunction dist = SquaredEuclideanDistanceFunction.STATIC;
    RandomlyGeneratedInitialMeans init = new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);
    KMeansLloyd<NumberVector> km;
    Clustering<KMeansModel> c;
    Relation<NumberVector> rel;

    public KMEANSVirtualNetworkCreator() {

    }

    public VirtualNetwork createVirtualNetwork(Population population, Network network, int numVNodes) {
        // initialize new virtual network
        VirtualNetwork virtualNetwork = new VirtualNetwork();

        // FOR ALL activities find positions, record in list and store in array
        List<double[]> dataList = new ArrayList<>();

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElem : plan.getPlanElements()) {
                    if (planElem instanceof Activity) {
                        double x = ((Activity) planElem).getCoord().getX();
                        double y = ((Activity) planElem).getCoord().getY();
                        dataList.add(new double[] { x, y });
                    }
                }
            }
        }

        final double data[][] = new double[dataList.size()][2];
        for (int i = 0; i < dataList.size(); ++i) {
            data[i][0] = dataList.get(i)[0];
            data[i][1] = dataList.get(i)[1];
        }

        // COMPUTE CLUSTERING with k-means method
        // adapter to load data from an existing array.
        dbc = new ArrayAdapterDatabaseConnection(data);
        // Create a database (which may contain multiple relations!)
        db = new StaticArrayDatabase(dbc, null);
        // Load the data into the database (do NOT forget to initialize...)
        db.initialize();

        // Setup textbook k-means clustering:
        km = new KMeansLloyd<>(dist, numVNodes, 1000, init);

        // Run the algorithm:
        c = km.run(db);

        // Relation containing the number vectors:
        rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
        // We know that the ids must be a continuous range:
        DBIDRange ids = (DBIDRange) rel.getDBIDs();

        // CREATE MAP with all VirtualNodes
        Map<VirtualNode, Set<Link>> vNMap = new HashMap<>();

        {
            int index = 0;
            int neighCount = numVNodes - 1;
            for (Cluster<KMeansModel> clu : c.getAllClusters()) {
                Coord coord = new Coord(clu.getModel().getPrototype().get(0), clu.getModel().getPrototype().get(1));
                String indexStr = "vNode_" + Integer.toString(index + 1);
                vNMap.put(new VirtualNode(index, indexStr, neighCount, coord), new LinkedHashSet<Link>());
                index++;
            }

        }

        // ASSIGN network links to closest nodes with a quadtree structure
        QuadTree<VirtualNode> vnQuadtree;
        final double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        vnQuadtree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        for (VirtualNode virtualNode : vNMap.keySet()) {
            boolean successAdd = vnQuadtree.put(virtualNode.getCoord().getX(), virtualNode.getCoord().getY(), virtualNode);
            GlobalAssert.that(successAdd);
        }

        for (Link link : network.getLinks().values()) {
            VirtualNode closestVNode = vnQuadtree.getClosest(link.getCoord().getX(), link.getCoord().getY());
            vNMap.get(closestVNode).add(link);
        }

        for (VirtualNode virtualNode : vNMap.keySet()) {
            virtualNode.setLinks(vNMap.get(virtualNode));
            virtualNetwork.addVirtualNode(virtualNode);
        }

        // CREATE VirtualLinks
        int index = 0;
        for (VirtualNode vNfrom : virtualNetwork.getVirtualNodes()) {
            for (VirtualNode vNto : virtualNetwork.getVirtualNodes()) {
                if (!vNfrom.equals(vNto)) {
                    String indexStr = "vLink_" + Integer.toString(index + 1);
                    virtualNetwork.addVirtualLink(indexStr, vNfrom, vNto, coordDist(vNfrom.getCoord(), vNto.getCoord()));
                    index++;
                }
            }
        }

        return virtualNetwork;

    }

    double coordDist(Coord c1, Coord c2) {
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.hypot(dx, dy);
    }

}
