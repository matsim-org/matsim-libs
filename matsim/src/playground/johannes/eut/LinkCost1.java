package playground.johannes.eut;

import org.apache.log4j.Logger;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.network.Link;
import org.matsim.router.util.TravelCostI;

/**
 * 
 * @author gunnar
 * @author illenberger
 * 
 */
public class LinkCost1 extends TimevariantCostMapBased implements TravelCostI {
	
	private static Logger logger = Logger.getLogger(LinkCost1.class);

    // -------------------- CONSTRUCTION --------------------

    public LinkCost1(int startTime_s, int endTime_s, int binSize_s) {
        super(startTime_s, endTime_s, binSize_s);
    }

    // -------------------- SETTERS --------------------

    public void setBinCost(BasicLinkI link, double cost, int bin) {
        super.setBinCost(cost, bin, link);
    }

    // TODO new
    public void addBinCost(BasicLinkI link, double addend, int bin) {
        super.addBinCost(addend, bin, link);
    }

    // TODO new
    public void multBinCost(BasicLinkI link, double factor, int bin) {
        super.multBinCost(factor, bin, link);
    }

    public void setCost(BasicLinkI link, double cost, int time_s) {
        setBinCost(link, cost, getBin(time_s));
    }

    // -------------------- GETTERS --------------------

    public double getBinCost(BasicLinkI link, int bin) {
        return super.getBinCost(bin, link);
    }

    // TODO new
    // public double getBinAvgCost(BasicLinkI link, int bin) {
    // return super.getBinAvgCost(bin, link);
    // }

    public double getCost(BasicLinkI link, int time_s) {
        return getBinCost(link, getBin(time_s));
    }

    // --------------- IMPLEMENTATION OF LinkCostI ---------------

	public double getLinkTravelCost(Link link, double time_s) {
        return getCost(link, (int) time_s);
    }

    // -------------------- IMPLEMENTATION OF super.FileIO --------------------

    public static class FileIO extends TimevariantCostMapBased.FileIO {

        private final BasicNetI net;

        // CONSTRUCTION

        public FileIO(BasicNetI net) {
            this.net = net;
        }

        // INTERFACE IMPLEMENTATION

        @Override
        protected TimevariantCostMapBased newInstance(int startTime_s,
                int endTime_s, int binSize_s) {
            return new LinkCost1(startTime_s, endTime_s, binSize_s);
        }

        @Override
        protected String keys2string(Object... keys) {

            // CHECK

            if (keys == null || keys.length != 1) {
                final String msg = "Expecting 1 key!";
                logger.warn(msg);
                throw new IllegalArgumentException(msg);
            }

            // CONTINUE

            return ((BasicLinkI) keys[0]).getId().toString();
        }

        @Override
        protected Object[] string2keys(String string) {
            return new Object[] { net.getLinks().get(string.trim()) };
        }

        // FOR CONVENIENCE

        @Override
        public LinkCost1 read(String file) {
            return (LinkCost1) super.read(file);
        }

    }
}
