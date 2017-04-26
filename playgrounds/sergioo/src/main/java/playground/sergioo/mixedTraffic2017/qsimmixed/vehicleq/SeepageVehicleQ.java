package playground.sergioo.mixedTraffic2017.qsimmixed.vehicleq;

import org.matsim.core.config.groups.QSimConfigGroup;
import playground.sergioo.mixedTraffic2017.qsimmixed.QVehicle;

import java.util.*;

public final class SeepageVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {

	private final Collection<String> seepModes;
	private int maxSeepModeAllowed = 0;
	private int noOfSeepModeBringFwd = 0;

	public SeepageVehicleQ(Collection<String> seepModes, int maxSeepModeAllowed) {
		this.seepModes = seepModes;
		this.maxSeepModeAllowed = maxSeepModeAllowed;
	} // to find calls
	
	private final Queue<QVehicle> delegate = new PriorityQueue<>(11, new Comparator<QVehicle>() {

		@Override
		public int compare(QVehicle arg0, QVehicle arg1) {
			return Double.compare(arg0.getEarliestLinkExitTime(), arg1.getEarliestLinkExitTime());
		}

	});

	private final Queue<QVehicle> delegateS = new PriorityQueue<>(11, new Comparator<QVehicle>() {

		@Override
		public int compare(QVehicle arg0, QVehicle arg1) {
			return Double.compare(arg0.getEarliestLinkExitTime(), arg1.getEarliestLinkExitTime());
		}

	});

	@Override
	public boolean offer(QVehicle e) {
		if(seepModes.contains(e.getDriver().getMode()))
			return delegateS.offer(e);
		else
			return delegate.offer(e);
	}

	@Override
	public QVehicle peek() {
		if(maxSeepModeAllowed>0 && noOfSeepModeBringFwd == maxSeepModeAllowed && delegate.size()>0) {
			noOfSeepModeBringFwd = 0;
			return delegate.peek();
		}
		else
			return delegateS.peek();
	}

	@Override
	public QVehicle poll() {
		QVehicle e = this.peek();
		this.remove(e);
		return e;
	}

	@Override
	public boolean remove(Object e) {
		if(seepModes.contains(((QVehicle)e).getDriver().getMode())) {
			if(maxSeepModeAllowed>0)
				noOfSeepModeBringFwd++;
			return delegateS.remove(e);
		}
		else
			return delegate.remove(e);
	}

	@Override
	public void addFirst(QVehicle qveh) {
		qveh.setEarliestLinkExitTime(Double.NEGATIVE_INFINITY);
		this.add(qveh) ; // uses the AbstractQueue.add, which in turn uses the PassingVehicleQ.offer.
	}

	@Override
	public Iterator<QVehicle> iterator() {
		List<QVehicle> itDelegate = new ArrayList<>();
		Queue<QVehicle> delegate = new PriorityQueue<>(this.delegate);
		Queue<QVehicle> delegateS = new PriorityQueue<>(this.delegateS);
		while(!(delegate.isEmpty() && delegateS.isEmpty())) {
			for(int i=0; !delegateS.isEmpty() && i<(maxSeepModeAllowed==0?this.size():maxSeepModeAllowed); i++)
				itDelegate.add(delegateS.poll());
			itDelegate.add(delegate.poll());
		}
		return itDelegate.iterator();
	}

	@Override
	public int size() {
		return delegate.size()+delegateS.size();
	}

}