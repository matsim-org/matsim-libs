// code by clruch
package playground.clruch.dispatcher.utils.virtualnodedestselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;

public class KMeansVirtualNodeDest extends AbstractVirtualNodeDest {
    Random random = new Random();
    DatabaseConnection dbc;
    Database db;
    SquaredEuclideanDistanceFunction dist = SquaredEuclideanDistanceFunction.STATIC;
    RandomlyGeneratedInitialMeans init = new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);
    KMeansLloyd<NumberVector> km;
    Clustering<KMeansModel> c;
    Relation<NumberVector> rel;

    @Override
    public List<Link> selectLinkSet(VirtualNode<Link> virtualNode, int size) {

        // if no vehicles to be send to node, return empty list
        if (size < 1)
            return Collections.emptyList();

        // 1) extract link center positions
        final double data[][] = new double[virtualNode.getLinks().size()][2];
        int i = -1;
        for (Link link : virtualNode.getLinks()) {
            ++i;
            data[i][0] = link.getCoord().getX();
            data[i][1] = link.getCoord().getY();
        }

        // 2) compute clustering using a k-means method
        // adapter to load data from an existing array.
        dbc = new ArrayAdapterDatabaseConnection(data);
        // Create a database (which may contain multiple relations!)
        db = new StaticArrayDatabase(dbc, null);
        // Load the data into the database (do NOT forget to initialize...)
        db.initialize();

        // Setup textbook k-means clustering:
        km = new KMeansLloyd<>(dist, size, 1000, init);

        // Run the algorithm:
        c = km.run(db);

        // Relation containing the number vectors:
        rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
        // We know that the ids must be a continuous range:
        // DBIDRange ids = (DBIDRange) rel.getDBIDs(); // <- not used

        // 3) for every cluster center, find the link closest to the center and return
        List<Link> ret = new ArrayList<>();
        List<Link> links = virtualNode.getLinks().stream().collect(Collectors.toList());
        i = 0;

        for (Cluster<KMeansModel> clu : c.getAllClusters()) {
            // get the center coordinates
            // Vector vector = clu.getModel().getPrototype(); // <- not used
            // find the closest link
            double dist = Double.MAX_VALUE;
            Link closestlink = links.get(0);
            for (Link link : links) {
                double dist_iter = Math.hypot(link.getCoord().getX() - clu.getModel().getPrototype().get(0), link.getCoord().getY() - clu.getModel().getPrototype().get(1));
                if (dist_iter < dist) {
                    dist = dist_iter;
                    closestlink = link;
                }
            }
            ret.add(closestlink);
            ++i;
        }

        // 4) if not enough clusters were generated, add links twice // TODO see if this can be removed and placed inside the run() statement of ELKI
        if (ret.size() != size) {
            int j = 0;
            while (ret.size() != size) {
                ret.add(ret.get(j));
                ++j;
            }
        }
        GlobalAssert.that(ret.size() == size);
        return ret;
    }
}
