package playgrounds.ssix;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.VehicleQ;

//A simple copy of mzilske's PassingVehicleQ (couldn't make import work).

public class MZilskePassingVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {


	private final Queue<QVehicle> delegate = new PriorityQueue<QVehicle>(11, new Comparator<QVehicle>() {

		@Override
		public int compare(QVehicle arg0, QVehicle arg1) {
			return Double.compare(arg0.getEarliestLinkExitTime(), arg1.getEarliestLinkExitTime());
		}

	});

	@Override
	public boolean offer(QVehicle e) {
		return delegate.offer(e);
	}

	@Override
	public QVehicle peek() {
		return delegate.peek();
	}

	@Override
	public QVehicle poll() {
		return delegate.poll();
	}

	@Override
	public void addFirst(QVehicle previous) {
		throw new RuntimeException();
	}

	@Override
	public Iterator<QVehicle> iterator() {
		return delegate.iterator();
	}

	@Override
	public int size() {
		return delegate.size();
	}

}