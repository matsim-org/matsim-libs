package playground.dhosse.prt.data;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import playground.michalm.taxi.data.*;

public class PrtData extends TaxiRankDataImpl {
	
	private Collection<TaxiRank> vehicleRanks;
	private static QuadTree<TaxiRank> quadTreeRanks;
	
	@SuppressWarnings("unchecked")
    public PrtData(Network network, TaxiRankDataImpl data){
		this.vehicleRanks = (Collection<TaxiRank>)data.getTaxiRanks().values();
		double[] bb = NetworkUtils.getBoundingBox(network.getNodes().values());
		this.initRankQuadTree(bb[0], bb[1], bb[2], bb[3]);
	}

    public void addVehicleRank(TaxiRank rank){
    	this.vehicleRanks.add(rank);
    }
    
    public void initRankQuadTree(double[] bounds){
    	this.initRankQuadTree(bounds[0], bounds[1], bounds[2], bounds[3]);
    }
    
    private void initRankQuadTree(double minX, double minY, double maxX, double maxY){
    	
    	quadTreeRanks = new QuadTree<TaxiRank>(minX, minY, maxX, maxY);
    	
    	for(TaxiRank rank : this.vehicleRanks){
    		double x = rank.getCoord().getX();
    		double y = rank.getCoord().getY();
    		quadTreeRanks.put(x, y, rank);
    	}
    }
    
    public TaxiRank getNearestRank(Coord coord){
    	return quadTreeRanks.getClosest(coord.getX(), coord.getY());
    }
    
    public TaxiRank getNearestRank(Link link){
    	return quadTreeRanks.getClosest(link.getCoord().getX(), link.getCoord().getY());
    }
}
