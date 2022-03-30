package lsp;

import lsp.shipment.LSPShipment;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.HashSet;
import java.util.Set;

/**
 * In order to enable a wide range of data to be represented, the abstract class {@link LSPInfo} was created.
 * It contains data about the object to which it is attached.
 *
 * Infos can be attached to {@link LogisticsSolution}s, {@link LSPShipment}s, {@link LogisticsSolutionElement}s and {@link LSPResource}s.
 *
 * Further, they can be valid only during a certain period of time.
 *
 */
public abstract class LSPInfo implements Attributable {

	private final Attributes attributes = new Attributes();

//	protected final Set<LSPInfo> predecessorInfos;
	
	protected LSPInfo() {
//		this.predecessorInfos = new HashSet<>();
	}

//	public void addPredecessorInfo(LSPInfo info) {
//		predecessorInfos.add(info);
//	}
	
//	public void removePredecessorInfo(LSPInfo info) {
//		if(predecessorInfos.contains(info)) {
//			predecessorInfos.remove(info);
//		}
//	}
	
//	public Set<LSPInfo> getPredecessorInfos() {
//		return predecessorInfos;
//	}
	
	public abstract void setName(String name);
	public abstract String getName();
	public abstract double getFromTime();
	public abstract double getToTime();
	public abstract void update();

	@Override
	public Attributes getAttributes(){
		return attributes;
	}


}
