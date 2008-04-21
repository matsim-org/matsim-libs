package playground.kai.teach;

import java.io.IOException;
import java.util.*;

import org.matsim.interfaces.networks.basicNet.*;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.NetVis;

import teach.multiagent07.net.CANetStateWritableI;
import teach.multiagent07.net.CANetStateWriter;

class Veh implements DrawableAgentI {
	
	private double pos ;
	public void setPosition ( double tmp ) {
		pos = tmp ;
	}

	public int getLane() {
		return 1;
	}

	public double getPosInLink_m() {
		// TODO Auto-generated method stub
		return 0;
	}

}

class CALink implements CANetStateWritableI {	
	
	final double CELLSIZE=7.5 ;
	
	List cells = new ArrayList() ;
	
	public CALink( BasicLink ll ) {
		// ...
	}

	public List<DrawableAgentI> getDisplayAgents() {
		List<DrawableAgentI> list = new ArrayList() ;
		for ( int ii=0 ; ii<cells.size() ; ii++ ) {
			Veh veh = (Veh) cells.get(ii) ;
			if ( veh!= null ) {
				veh.setPosition(ii*CELLSIZE ) ;
				list.add( veh ) ;
			}
		}
		return list ;
	}

	public double getDisplayValue() {	
		// count occupied cells
		// return occ.cells/cells
		return 0.5 ;
	}
}

class CASim {
	
	CANetStateWriter netVis ;
	
	Map caLinks = new HashMap() ;
	Map caNodes = new HashMap() ;

	public CASim( BasicNet net, CANetStateWriter netVis ) {
		Map links = net.getLinks() ;
		for ( Iterator it = links.values().iterator(); it.hasNext() ; ) {
			BasicLink ll = (BasicLink) it.next();
			CALink caLink = new CALink( ll ) ;	
//			caLink.randomFill() ;
//			caLink.tty() ;
		}
		// dto for nodes

		// connect??
		
		try {
			netVis.dump(0) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
}

public class MyControler {
	

	public static void main(String[] args) {
		String netFileName = "net.xml" ;
		String visFileName = "output/ABC" ;
	
		BasicNet net = new NetworkLayer() ;
		MatsimNetworkReader reader = new MatsimNetworkReader((NetworkLayer)net);
		reader.readFile(netFileName) ;
		// from here on, net can do everything that is guaranteed in BasicNetI
		
		Map nodes = net.getNodes() ;
		
		nodes.get("123") ;
		
		for ( Iterator it = nodes.values().iterator(); it.hasNext() ; ) {
			BasicNode nn = (BasicNode) it.next();
			System.out.println ( nn.getId() ) ;
		}
		
		CANetStateWriter netVis = CANetStateWriter.createWriter( net, netFileName, visFileName );		
		
		CASim sim = new CASim( net , netVis ) ;
		
		NetVis.start(visFileName) ;
		
	}

}
