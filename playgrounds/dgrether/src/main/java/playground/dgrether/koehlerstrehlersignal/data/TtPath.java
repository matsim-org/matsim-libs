/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.data;

import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author tthunig
 */
public class TtPath {

	private Id<TtPath> id;
	private List<Id<DgStreet>> path;
	private double flow;
	
	
	public TtPath(Id<TtPath> id, List<Id<DgStreet>> path, double flow) {
		super();
		this.id = id;
		this.path = path;
		this.flow = flow;
	}

	public void increaseFlow(double flowValue) {
		this.flow += flowValue;
	}

	
	public Id<TtPath> getId() {
		return this.id;
	}

	public List<Id<DgStreet>> getPath() {
		return this.path;
	}

	public double getFlow() {
		return this.flow;
	}	
	
}
