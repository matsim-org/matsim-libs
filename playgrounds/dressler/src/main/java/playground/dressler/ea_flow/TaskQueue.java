package playground.dressler.ea_flow;

import java.util.Collection;

public interface TaskQueue extends Iterable<BFTask>{

	boolean addAll(Collection<? extends BFTask> c);
	boolean addAll(TaskQueue tasks);
	boolean add(BFTask task);
	BFTask poll();

}
