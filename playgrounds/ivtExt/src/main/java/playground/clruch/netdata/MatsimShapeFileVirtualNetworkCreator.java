/**
 * 
 */
package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import ch.ethz.idsc.queuey.core.networks.MultiPolygons;
import ch.ethz.idsc.queuey.core.networks.MultiPolygonsVirtualNetworkCreator;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.utils.NDTreeReducer;
import playground.clruch.dispatcher.utils.PlaneLocation;
import playground.clruch.options.ScenarioOptions;

/** @author Claudio Ruch */
public class MatsimShapeFileVirtualNetworkCreator {

    public VirtualNetwork<Link> creatVirtualNetwork(Network network, ScenarioOptions scenarioOptions) {
        File shapeFile = scenarioOptions.getShapeFile();
        boolean completeGraph = scenarioOptions.isCompleteGraph();
        GlobalAssert.that(shapeFile.exists());
        MultiPolygons multiPolygons = null;
        try {
            multiPolygons = new MultiPolygons(shapeFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Not able to load shapeFile for virtual Network creation. Stopping execution.");
            GlobalAssert.that(false);
        }

        @SuppressWarnings("unchecked")
        Collection<Link> elements = (Collection<Link>) network.getLinks().values();
        Tensor lbounds = NDTreeReducer.lowerBoudnsOf(network);
        Tensor ubounds = NDTreeReducer.upperBoudnsOf(network);

        Map<Node, HashSet<Link>> uElements = new HashMap<>();
        network.getNodes().values().forEach(n -> uElements.put(n, new HashSet<>()));
        network.getLinks().values().forEach(l -> uElements.get(l.getFromNode()).add(l));
        network.getLinks().values().forEach(l -> uElements.get(l.getToNode()).add(l));

        MultiPolygonsVirtualNetworkCreator<Link, Node> mpvnc = new MultiPolygonsVirtualNetworkCreator<>(multiPolygons, //
                elements, PlaneLocation::of, NetworkCreatorUtils::linkToID, //
                uElements, lbounds, ubounds, completeGraph);

        return mpvnc.getVirtualNetwork();

    }

    public static Tensor NoMeaningOf(Link link) {
        return Tensors.vector(0, 0, 0);
    }

}
