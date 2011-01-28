package playground.dressler.Interval;

import playground.dressler.Interval.Interval;

class SkipNodeIntervals<T extends Interval>
{
    public final T value;
    public final SkipNodeIntervals<T>[] next;

    @SuppressWarnings("unchecked")
	public SkipNodeIntervals(int level, T value) 
    {
        this.value = value;
        next = new SkipNodeIntervals[level + 1];
    }

}