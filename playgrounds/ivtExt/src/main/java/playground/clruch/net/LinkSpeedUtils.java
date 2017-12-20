// code by andya
package playground.clruch.net;

import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import playground.clruch.io.fleet.TaxiTrail;

/** @author Andreas Aumiller */
public class LinkSpeedUtils {

    private TaxiTrail taxiTrail;
    private final QuadTree<Link> qt;
    private final MatsimStaticDatabase db;

    public LinkSpeedUtils(TaxiTrail taxiTrail, QuadTree<Link> qt, MatsimStaticDatabase db) {
        this.taxiTrail = taxiTrail;
        this.qt = qt;
        this.db = db;
    }

    public LinkSpeedUtils(TaxiTrail taxiTrail, Network network, MatsimStaticDatabase db) {
        final double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        final QuadTree<Link> qt = new QuadTree<>( //
                networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        for (Link link : db.getLinkInteger().keySet())
            qt.put(link.getCoord().getX(), link.getCoord().getY(), link);
        this.taxiTrail = taxiTrail;
        this.qt = qt;
        this.db = db;
    }
    
    public void setTaxiTrail(TaxiTrail taxiTrail) {
        this.taxiTrail = taxiTrail;
    }
    
    public double getLinkSpeed(int now) {
        if (Objects.nonNull(taxiTrail.getLastEntry(now))) {
            Coord nowXY = taxiTrail.interp(now).getValue().gps;
            Coord lastXY = taxiTrail.getLastEntry(now).getValue().gps;
            int nowLink = getLinkfromCoord(nowXY);
            int lastLink = getLinkfromCoord(lastXY);

            // if taxi is still on the same link estimate speed by dividing distance over time
            if (nowLink == lastLink) {
                int dTime = now - taxiTrail.getLastEntry(now).getKey();
                return getGPSDistance(nowXY.getY(), nowXY.getX(), lastXY.getY(), lastXY.getX()) / dTime;
            }
        }
        return 0;
    }

    public int getLinkfromCoord(Coord gps) {
        // System.out.println("getting Link from Coord x: " + gps.getX() + " / y: " + gps.getY());
        // System.out.println(this.taxiTrail.toString());
        // System.out.println(this.qt.toString());
        // System.out.println(this.db.toString());
        int linkIndex;
        Coord xy = db.referenceFrame.coords_fromWGS84.transform(gps);
        linkIndex = db.getLinkIndex(qt.getClosest(xy.getX(), xy.getY()));
        return linkIndex;
    }

    // Reference for haversine formula not covering elevation (accurate enough for short distances like just in CH)
    // https://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
    public static double getGPSDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double R = 6371000; // in meters
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
