package playground.dressler.ea_flow;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class SimpleTaskQueue implements TaskQueue {
	private LinkedList<BFTask> _list;
	
	public SimpleTaskQueue(){
		_list= new LinkedList<BFTask>();
	}
	
	@Override
	public boolean addAll(Collection<? extends BFTask> c) {
		return _list.addAll(c);
	}

	@Override
	public Iterator<BFTask> iterator() {
		return _list.iterator();
	}

	@Override
	public boolean add(BFTask e) {
		return _list.add(e);
	}

	@Override
	public boolean addAll(TaskQueue tasks) {		
		boolean result= false;
		for(BFTask task: tasks){
			Boolean test=_list.add(task);
			if(test)result=test;
		}
		return result;
	}

	@Override
	public BFTask poll() {
		return _list.poll();
	}

	

}
