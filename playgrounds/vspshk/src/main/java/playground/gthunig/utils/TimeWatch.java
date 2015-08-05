package playground.gthunig.utils;

import java.util.concurrent.TimeUnit;

public class TimeWatch {    
    
	long starts;

    public static TimeWatch start() {
        return new TimeWatch();
    }

    private TimeWatch() {
        reset();
    }

    public TimeWatch reset() {
        starts = System.currentTimeMillis();
        return this;
    }

    public long time() {
        long ends = System.currentTimeMillis();
        return ends - starts;
    }

    public long time(TimeUnit unit) {
        return unit.convert(time(), TimeUnit.MILLISECONDS);
    }
    
    public long timeInSec() {
    	return time(TimeUnit.SECONDS);
    }
    
    public long timeInMin() {
    	return time(TimeUnit.MINUTES);
    }
    
    public String getElapsedTime() {
    	long time = time(TimeUnit.SECONDS);
    	int hours = (int) time / 3600;
    	int minutes = (int) (time - hours * 3600) / 60;
    	int seconds = (int) (time - hours * 3600 - minutes * 60);
    	return hours + ":" + minutes + ":" + seconds;
    }
}