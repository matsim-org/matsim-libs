package playground.dressler.ea_flow;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import playground.dressler.Interval.VertexIntervalWithCost;

public class TwinTaskQueue implements TaskQueue {
	private LinkedList<BFTask> _first;
	private LinkedList<BFTask> _second;
	
	public TwinTaskQueue(){
		_first= new LinkedList<BFTask>();
		_second= new LinkedList<BFTask>();
	}
	
	class TwinIterator implements Iterator<BFTask>{
		Iterator<BFTask> _firstiter;
		Iterator<BFTask> _seconditer;
		public TwinIterator(){
			_firstiter = _first.iterator();
			_seconditer = _second.iterator();
		}
		@Override
		public boolean hasNext() {
			if(_firstiter.hasNext())return true;
			return _seconditer.hasNext();
		}

		@Override
		public BFTask next() {
			
			BFTask result = null;
			if(_firstiter.hasNext()){
				result=_firstiter.next();
			}else{
				if(_seconditer.hasNext()){
					result=_seconditer.next();
				}
			}
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	@Override
	public boolean add(BFTask task) {
		boolean result =false;
		if(task.ival instanceof playground.dressler.Interval.VertexIntervalWithCost){
			VertexIntervalWithCost costival = (VertexIntervalWithCost)task.ival;
			if(costival.costIsRelative && costival.getCost()==0){
				result =_first.add(task);
			}else{
				result =_second.add(task);
			}
		}else{
			result =_first.add(task);
		}
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends BFTask> tasks) {
		boolean result = false;
		boolean test=false;
		for(BFTask task: tasks){
			test= this.add(task);
			if(test)result=test;
		}
		return result;
	}

	@Override
	public boolean addAll(TaskQueue tasks) {
		boolean result = false;
		boolean test=false;
		for(BFTask task: tasks){
			test= this.add(task);
			if(test)result=test;
		}
		return result;
	}

	@Override
	public BFTask poll() {
		BFTask result = _first.poll();
		if(result==null){
			result=_second.poll();
		}
		return result;
	}

	@Override
	public Iterator<BFTask> iterator() {
		return new TwinIterator();
	}

}
