package playground.sergioo.mixedtraffic2016.gui;

import java.util.Observable;

public class Animation extends Thread {

	private boolean pause;
	private double time;
	private double rate;
	private Road road;
	private Window window;
	
	public Animation(Road road) {
		super();
		this.road = road;
	}

	public void setWindow(Window window) {
		this.window = window;
	}

	public void run() {
		this.pause = true;
		this.time = road.startTime;
		this.rate = 10;
		while(true) {
			if(!pause) {
				road.setCurrentTime(time);
				((Observable)road).notifyObservers();
				time+=(1/rate);
				window.moveTime((int)time);
				if(time>road.endTime)
					time = road.startTime;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void pause() {
		rate = 0;
		pause = true;
	}

	public void play() {
		rate = 10;
		pause = false;
	}

	public void changeFPS(int value) {
		if(value==0)
			pause=true;
		else {
			pause = false;
			rate = 100/value;
		}
	}

	public void changeTime(int value) {
		time = value;
		road.setCurrentTime(time);
		((Observable)road).notifyObservers();
	}
}
