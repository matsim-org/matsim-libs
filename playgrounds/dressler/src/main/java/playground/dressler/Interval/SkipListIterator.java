package playground.dressler.Interval;

import java.util.Iterator;

import playground.dressler.Interval.Interval;

public class SkipListIterator<T extends Interval> implements Iterator {
	SkipNodeIntervals<T> node;
	//SkipListIntervals<T> list;

	public SkipListIterator(final IntervalsSkipList<T> list) {
		//this.list = list;
		node = list.header.next[0];
	}
	
	public SkipListIterator(final IntervalsSkipList<T> list, int t) {
		//this.list = list;
		node = list.getNodeAt(t);
	}
	
	/*public void setTo(int t) {	
		node = list.getNodeAt(t);
	}*/
	
	@Override
	public boolean hasNext() {		
		return node != null;
	}

	@Override
	public T next() {
		T tmp = node.value;
		node = node.next[0];
		return tmp;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This Iterator does not implement the remove method");  		
	}
	
	
}