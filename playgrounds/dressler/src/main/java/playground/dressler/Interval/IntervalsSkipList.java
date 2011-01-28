package playground.dressler.Interval;

import java.util.Iterator;

import playground.dressler.Interval.Interval;
import playground.dressler.Interval.IntervalsInterface;
import playground.dressler.util.MyRandom;

/* Copyright (c) 2011 the authors listed at the following URL, and/or
the authors of referenced articles or incorporated external code:
http://en.literateprograms.org/Skip_list_(Java)?action=history&offset=20090115223423

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Retrieved from: http://en.literateprograms.org/Skip_list_(Java)?oldid=15959
*/


public class IntervalsSkipList<T extends Interval > implements IntervalsInterface<T> 
{
	public static int prob  = -Integer.MAX_VALUE / 2; // 25%	
	//public static int prob  = 0; // 50%

	public static final int MAX_LEVEL = 12; // 19 for 1 million entries or so ...

	public static int randomLevel() {
		
		int lvl = 0;
		while (lvl < MAX_LEVEL && MyRandom.nextInt() < prob ) lvl++;
		return lvl;
	} 

    private int _lastT;
    private int _size;

    public final SkipNodeIntervals<T> header = new SkipNodeIntervals<T>(MAX_LEVEL, null);
    public int level = 0;

    public IntervalsSkipList(T all) {
    	_lastT = all._r;
    	insert(all);    	    	
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        SkipNodeIntervals<T> x = header.next[0];
        while (x != null) {
            sb.append(x.value);
            x = x.next[0];
            if (x != null)
                sb.append(",");
        }    
        sb.append("}");
        return sb.toString();
    }  
    
    public T getIntervalAt(int t) {
    	SkipNodeIntervals<T> x = header;
    	for (int i = level; i >= 0; i--) {
    		while (x.next[i] != null && x.next[i].value._l <= t) {
    			x = x.next[i];
    		}
    	}

    	return x.value;
    }
    
    public SkipNodeIntervals<T> getNodeAt(int t) {
    	SkipNodeIntervals<T> x = header;
    	for (int i = level; i >= 0; i--) {
    		while (x.next[i] != null && x.next[i].value._l <= t) {
    			x = x.next[i];
    		}
    	}

    	return x;
    }

    @SuppressWarnings("unchecked")
    public void insert(T value)
    {
    	_size++;
    	SkipNodeIntervals<T> x = header;	
    	SkipNodeIntervals<T>[] update = new SkipNodeIntervals[MAX_LEVEL + 1];

    	for (int i = level; i >= 0; i--) {
    		while (x.next[i] != null && x.next[i].value._l < value._l) {
    			x = x.next[i];
    		}
    		update[i] = x; 
    	}
    	x = x.next[0];


    	if (x == null || !x.value.equals(value)) {        
    		int lvl = randomLevel();

    		if (lvl > level) {
    			for (int i = level + 1; i <= lvl; i++) {
    				update[i] = header;
    			}
    			level = lvl;
    		}

    		x = new SkipNodeIntervals<T>(lvl, value);
    		for (int i = 0; i <= lvl; i++) {
    			x.next[i] = update[i].next[i];
    			update[i].next[i] = x;
    		}

    	}
    }

    @SuppressWarnings("unchecked")
    public void delete(Interval value)
    {
    	_size--;
    	SkipNodeIntervals<T> x = header;	
    	SkipNodeIntervals<T>[] update = new SkipNodeIntervals[MAX_LEVEL + 1];

    	for (int i = level; i >= 0; i--) {
    		while (x.next[i] != null && x.next[i].value.compareTo(value) < 0) {
    			x = x.next[i];
    		}
    		update[i] = x; 
    	}
    	x = x.next[0];


    	if (x.value.equals(value)) {
    		for (int i = 0; i <= level; i++) {
    			if (update[i].next[i] != x)
    				break;
    			update[i].next[i] = x.next[i];
    		}

    		while (level > 0 && header.next[level] == null) {
    			level--;
    		}

    	}
    }

	

	@Override
	public int getSize() {
		return _size;
	}

	@Override
	public int getMeasure() {		
		return _size;
	}

	@Override
	public T getLast() {
		return getIntervalAt(_lastT - 1);
	}

	@Override
	public boolean isLast(Interval o) {
		return _lastT == o._r;
	}

	@Override
	public T getNext(Interval o) {		
		return getIntervalAt(o._r);
	}

	@Override
	public int getLastTime() {
		return _lastT;
	}

	@Override
	public T splitAt(int t) {
		_size++;
    	SkipNodeIntervals<T> x = header;	
    	@SuppressWarnings("unchecked")
		SkipNodeIntervals<T>[] update = new SkipNodeIntervals[MAX_LEVEL + 1];

    	for (int i = level; i >= 0; i--) {
    		while (x.next[i] != null && x.next[i].value._l < t) {
    			x = x.next[i];
    		}
    		update[i] = x; 
    	}    	

    	if (x.value._l == t ) throw new IllegalArgumentException("there is no Interval that can be split at "+t);

    	//if (x == null || x.value._l != t) {        
    	int lvl = randomLevel();

    	if (lvl > level) {
    		for (int i = level + 1; i <= lvl; i++) {
    			update[i] = header;
    		}
    		level = lvl;
    	}
    	
    	T split = (T) x.value.splitAt(t);    	
    	
    	x = new SkipNodeIntervals<T>(lvl, split);
    	for (int i = 0; i <= lvl; i++) {
    		x.next[i] = update[i].next[i];
    		update[i].next[i] = x;
    	}

    	//}
    	
		return split;
	}
	
	public static void main(String[] args) {
		IntervalsSkipList<Interval> SL = new IntervalsSkipList<Interval>(new Interval(0,100));
		System.out.println(SL);
		SL.splitAt(4);
		System.out.println(SL);
		SL.splitAt(20);
		SL.splitAt(150);
		SL.splitAt(8);
		SL.splitAt(7);
		System.out.println(SL);
		
		SkipListIterator<Interval> iter = new SkipListIterator<Interval>(SL);
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}
		
		iter = new SkipListIterator<Interval>(SL, 10);
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}
	}

	@Override
	public Iterator<T> getIterator() {
		return new SkipListIterator<T>(this);		
	}

	@Override
	public Iterator<T> getIteratorAt(int t) {
		return new SkipListIterator<T>(this, t);
	}

}
