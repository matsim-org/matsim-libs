package playground.dhosse.prt.data;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.data.TaxiRank;

public class PrtData extends TaxiData {
	
	private List<TaxiRank> vehicleRanks;
	private static QuadTree<TaxiRank> quadTreeRanks;
	
	public PrtData(Network network, TaxiData data){
		this.vehicleRanks = data.getTaxiRanks();
		double[] bb = NetworkUtils.getBoundingBox(network.getNodes().values());
		this.initRankQuadTree(bb[0], bb[1], bb[2], bb[3]);
	}

    public void addVehicleRank(TaxiRank rank){
    	this.vehicleRanks.add(rank);
    }
    
    public void initRankQuadTree(double minX, double minY, double maxX, double maxY){
    	
    	quadTreeRanks = new QuadTree<TaxiRank>(minX, minY, maxX, maxY);
    	
    	for(TaxiRank rank : this.vehicleRanks){
    		double x = rank.getCoord().getX();
    		double y = rank.getCoord().getY();
    		quadTreeRanks.put(x, y, rank);
    	}
    }
    
    public static TaxiRank getNearestRank(Coord coord){
    	return quadTreeRanks.get(coord.getX(), coord.getY());
    }
    
    public static TaxiRank getNearestRank(Link link){
    	return quadTreeRanks.get(link.getCoord().getX(), link.getCoord().getY());
    }
    
    @SuppressWarnings("unchecked")
    private static <S, T> List<T> convertList(List<S> list)
    {
        return (List<T>)list;
    }

}
