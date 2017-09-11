/**
 * 
 */
package playground.clruch.netdata;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.KMeansVirtualNetworkCreator;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.tensor.Tensor;
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
import playground.clruch.dispatcher.utils.NDTreeReducer;
import playground.clruch.dispatcher.utils.PlaneLocation;

/** @author Claudio Ruch */
public class MatsimKMEANSVirtualNetworkCreator {

    Random random = new Random();
    DatabaseConnection dbc;
    Database db;
    SquaredEuclideanDistanceFunction dist = SquaredEuclideanDistanceFunction.STATIC;
    RandomlyGeneratedInitialMeans init = new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);
    KMeansLloyd<NumberVector> km;
    Clustering<KMeansModel> c;
    Relation<NumberVector> rel;

    public VirtualNetwork<Link> createVirtualNetwork(Population population, Network network, int numVNodes, boolean completeGraph) {

        double data[][] = NetworkCreatorUtils.fromPopulation(population, network);
        Collection<Link> elements = (Collection<Link>) network.getLinks().values();
        Tensor lbounds = NDTreeReducer.lowerBoudnsOf(network);
        Tensor ubounds = NDTreeReducer.upperBoudnsOf(network);

        Map<Node, HashSet<Link>> uElements = new HashMap<>();

        network.getNodes().values().forEach(n -> uElements.put(n, new HashSet<>()));

        network.getLinks().values().forEach(l -> uElements.get(l.getFromNode()).add(l));
        network.getLinks().values().forEach(l -> uElements.get(l.getToNode()).add(l));

        KMeansVirtualNetworkCreator<Link, Node> vnc = new KMeansVirtualNetworkCreator<>(data, elements, uElements, PlaneLocation::of, //
                NetworkCreatorUtils::linkToID, lbounds, ubounds, numVNodes, completeGraph);

        return vnc.getVirtualNetwork();

    }
}
