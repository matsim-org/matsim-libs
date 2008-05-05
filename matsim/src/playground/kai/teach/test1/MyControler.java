package playground.kai.teach.test1;

import java.io.IOException;
import java.util.*;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.networks.basicNet.*;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.NetVis;


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

class CANet implements BasicNet {	
	private Map caLinks = new HashMap() ;
	private Map caNodes = new HashMap() ;
	public CANet( BasicNet net ) {

		// copy links
		Map basicLinks = net.getLinks() ;
		for ( Iterator it = basicLinks.values().iterator(); it.hasNext() ; ) {
			BasicLink basicLink = (BasicLink) it.next();
			CALink caLink = new CALink( basicLink ) ;	
			caLinks.put( basicLink.getId(), caLink ) ;
//			caLink.randomFill() ;
//			caLink.tty() ;
		}

		// copy nodes
		// todo

		// connect??

	}
	public Map<Id, ? extends BasicLink> getLinks() {
		return caLinks ;
	}
	public Map<Id, ? extends BasicNode> getNodes() {
		return caNodes ;
	}
	public void connect() {
		// TODO Auto-generated method stub
	}
}

class CASim {
	
	CANetStateWriter netVis ;
	CANet caNet ;
	
	public CASim( BasicNet net ) {
		caNet = new CANet(net) ;
		

		CANetStateWriter netVis = CANetStateWriter.createWriter( caNet, MyControler.netFileName, MyControler.visFileName );		
		try {
			netVis.dump(0) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
}

public class MyControler {
	static String netFileName = "net.xml" ;
	static String visFileName = "output/ABC" ;
	

	public static void main(String[] args) {

		BasicNet net = new NetworkLayer() ;
		MatsimNetworkReader reader = new MatsimNetworkReader((NetworkLayer)net);
		reader.readFile(netFileName) ;
		// from here on, net can do everything that is guaranteed in BasicNetI
		
		Map links = net.getLinks() ;
		Id id = new IdImpl("12") ;
		BasicLink link = (BasicLink) links.get(id) ;
		System.out.println ( "length: " + link.getLength() ) ;
		
		
		
//		CASim sim = new CASim( net ) ;
//		
//		NetVis.start(visFileName) ;
		
	}

}
