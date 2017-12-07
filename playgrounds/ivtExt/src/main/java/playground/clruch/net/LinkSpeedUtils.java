package playground.clruch.net;

import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

import playground.clruch.io.fleet.TaxiTrail;

/** @author Andreas Aumiller */
public class LinkSpeedUtils {

    private TaxiTrail taxiTrail;
    private QuadTree<Link> qt;
    private MatsimStaticDatabase db;

    public LinkSpeedUtils(TaxiTrail taxiTrail, QuadTree<Link> qt, MatsimStaticDatabase db) {
        this.taxiTrail = taxiTrail;
        this.qt = qt;
        this.db = db;
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

    public int getLinkfromCoord(Coord xy) {
        int linkIndex;
        xy = db.referenceFrame.coords_fromWGS84.transform(xy);
        linkIndex = db.getLinkIndex(qt.getClosest(xy.getX(), xy.getY()));
        return linkIndex;
    }

    public double getGPSDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double R = 6371000; // in meters
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
