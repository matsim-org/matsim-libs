package playground.gthunig.utils;

import java.util.concurrent.TimeUnit;

public class StopWatch {
    
	private long start;
	private long end = -1;

    public StopWatch() {
        reset();
    }

	public void reset() {
        start = System.currentTimeMillis();
    }

    public long time() {
        long ends = System.currentTimeMillis();
        return ends - start;
    }

	public void stop() {
		end = System.currentTimeMillis();
	}

	public String getStoppedTime() {
		if (end == -1) {
			end = System.currentTimeMillis();
		}
		long time = TimeUnit.SECONDS.convert(end - start, TimeUnit.MILLISECONDS);
		int hours = (int) time / 3600;
		int minutes = (int) (time - hours * 3600) / 60;
		int seconds = (int) (time - hours * 3600 - minutes * 60);
		return hours + ":" + minutes + ":" + seconds;
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