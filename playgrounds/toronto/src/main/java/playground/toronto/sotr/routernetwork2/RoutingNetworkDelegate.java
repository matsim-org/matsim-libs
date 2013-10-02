package playground.toronto.sotr.routernetwork2;

import java.util.Iterator;

import org.matsim.core.utils.collections.QuadTree;

public class RoutingNetworkDelegate {
	
	private QuadTree<RoutingNode> quadTree;
	private RoutingLink[] links;
	
	public void reset(){
		for (RoutingLink link : links){
			link.reset();
		}
	}
	
	public int getSize(){
		return links.length;
	}

	public Iterable<RoutingLink> getLinks(){
		return new Iterable<RoutingLink>() {
			
			@Override
			public Iterator<RoutingLink> iterator() {
				return new Iterator<RoutingLink>() {
					
					int i =0;
					
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
					
					@Override
					public RoutingLink next() { return links[i++]; }
					
					@Override
					public boolean hasNext() {return i < links.length;}
				};
			}
		};
	}
}
