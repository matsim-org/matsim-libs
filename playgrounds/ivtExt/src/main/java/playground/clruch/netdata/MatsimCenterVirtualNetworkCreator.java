/**
 * 
 */
package playground.clruch.netdata;

import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.core.networks.CenterVirtualNetworkCreator;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import playground.clruch.dispatcher.utils.PlaneLocation;

/** @author Claudio Ruch */
public class MatsimCenterVirtualNetworkCreator {

    Random random = new Random();
    DatabaseConnection dbc;
    Database db;
    SquaredEuclideanDistanceFunction dist = SquaredEuclideanDistanceFunction.STATIC;
    RandomlyGeneratedInitialMeans init = new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);
    KMeansLloyd<NumberVector> km;
    Clustering<KMeansModel> c;
    Relation<NumberVector> rel;

    public VirtualNetwork<Link> creatVirtualNetwork(Network network) {
        // TODO magic consts.
        double centerRadius = 1500;
        Collection<Link> elements = (Collection<Link>) network.getLinks().values();
        CenterVirtualNetworkCreator<Link> cvn = new CenterVirtualNetworkCreator<>(centerRadius, elements, PlaneLocation::of, NetworkCreatorUtils::linkToID);
        return cvn.getVirtualNetwork();
    }

}
