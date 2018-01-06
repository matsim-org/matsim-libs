/**
 * 
 */
package playground.lsieber.networkshapecutter.networkcuttershape;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import com.vividsolutions.jts.geom.MultiPolygon;

import ch.ethz.idsc.queuey.core.networks.MultiPolygons;
import playground.lsieber.networkshapecutter.NetworkCutter;

/** @author Claudio Ruch */
public class NetworkCutterShape extends NetworkCutter {

    private Network modifiedNetwork;
    private final MultiPolygons multipolygons;

    public NetworkCutterShape(File shapefile) throws IOException {
        this.multipolygons = new MultiPolygons(shapefile);
    }

    @Override
    public Network process(Network network) throws MalformedURLException, IOException {
        Network modifiedNetwork = filterInternal(network);
        return modifiedNetwork;
    }

    public Network filterInternal(Network originalNetwork) throws IOException {
        modifiedNetwork = NetworkUtils.createNetwork();
        Set<MultiPolygon> polygons = multipolygons.getPolygons();
        AddContainedNodes.of(originalNetwork).in(polygons).to(modifiedNetwork);
        AddContainedLinks.of(originalNetwork).to(modifiedNetwork);
        return modifiedNetwork;
    }

}
