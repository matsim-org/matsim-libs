/**
 * 
 */
package playground.clruch.netdata;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.core.networks.CenterVirtualNetworkCreator;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
<<<<<<< HEAD
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
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
=======
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
>>>>>>> 8c3fede0f553a05993fe22f52e1096bbe2350777
import playground.clruch.dispatcher.utils.PlaneLocation;

/** @author Claudio Ruch creates {@link VirtualNetwork} with a center node and a surrounding node. The center node
 *         is located at the mean location of all {@link Network} {@link Link} and has a radius specified by the user, it is
 *         shifted by centerShift, i.e. centerActual = centerComputed + centerShift */
public class MatsimCenterVirtualNetworkCreator {

<<<<<<< HEAD
	Random random = new Random();
	DatabaseConnection dbc;
	Database db;
	SquaredEuclideanDistanceFunction dist = SquaredEuclideanDistanceFunction.STATIC;
	RandomlyGeneratedInitialMeans init = new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);
	KMeansLloyd<NumberVector> km;
	Clustering<KMeansModel> c;
	Relation<NumberVector> rel;

	public VirtualNetwork<Link> creatVirtualNetwork(Network network, double centerRadius, Tensor centerShift) {
		Collection<Link> elements = (Collection<Link>) network.getLinks().values();

		CenterVirtualNetworkCreator<Link> cvn = new CenterVirtualNetworkCreator<>(centerRadius, centerShift, elements,
				PlaneLocation::of, NetworkCreatorUtils::linkToID);  
		return cvn.getVirtualNetwork();

	}
=======
    private final double radius;
    private final Coord centerShift;
    private final Network network;
    private final VirtualNetwork<Link> virtualNetwork;

    public MatsimCenterVirtualNetworkCreator(Coord centerShift, double radius, Network network) {
        this.radius = radius;
        this.centerShift = centerShift;
        this.network = network;
        virtualNetwork = creatVirtualNetwork(network);

    }

    private VirtualNetwork<Link> creatVirtualNetwork(Network network) {
        Collection<Link> elements = (Collection<Link>) network.getLinks().values();
        Tensor centerShift =  Tensors.vectorDouble(this.centerShift.getX(), this.centerShift.getY());
        CenterVirtualNetworkCreator<Link> cvn = new CenterVirtualNetworkCreator<>(radius, centerShift, elements, //
                PlaneLocation::of, NetworkCreatorUtils::linkToID);
        return cvn.getVirtualNetwork();
    }
>>>>>>> 8c3fede0f553a05993fe22f52e1096bbe2350777

    public VirtualNetwork<Link> getVirtualNetwork() {
        GlobalAssert.that(virtualNetwork != null);
        return virtualNetwork;
    }

}
