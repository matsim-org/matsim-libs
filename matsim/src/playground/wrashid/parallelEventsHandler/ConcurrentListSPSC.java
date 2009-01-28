/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wrashid.parallelEventsHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import playground.wrashid.DES.util.Timer;
// optimized for single producer, single consumer
// it can be used by multiple producers, but it is not optimized for that case
public class ConcurrentListSPSC<T> {
	private LinkedList<T> inputBuffer=new LinkedList<T>();
	private LinkedList<T> outputBuffer=new LinkedList<T>(); 
	public void add(T element){
		synchronized (inputBuffer){
			inputBuffer.add(element);
		}
	}
	
	// the input list will be emptied
	public void add(ArrayList<T> list){
		synchronized (inputBuffer){
			inputBuffer.addAll(list);
		}
	}
	
	// returns null, if empty, else the first element
	public T remove(){
		if (outputBuffer.size()>0){
			return outputBuffer.poll();
		}
		if (inputBuffer.size()>0){
			synchronized (inputBuffer){
				//swap buffers
				LinkedList<T> tempList=null; 
				tempList=inputBuffer;
				inputBuffer=outputBuffer;
				outputBuffer=tempList;
			}
			return outputBuffer.poll();
		}
		return null;
	}
	
	
	
	// this experiment effectivly demonstrates, why reimplement ConcurrentList
	// ------------
	// consumed Items:10000
	// time required for ConcurrentList (consumer): 27031
	// ----------
	// consumed Items:10000
	// time required for ConcurrentLinkedQueue (consumer): 60953
	// ----------
	// This experiment was done with adding 10000000 elements but only 10000 consumed
	// It shows, that ConcurrentList much better decouples the producer from the consumer
	// especially, when the consumer is slower than the producer (which was simulated by the sleep(1)
	

	public static void main(String[] args){
		
		// part 1
		
		ConcurrentListSPSC<Integer> cList=new ConcurrentListSPSC<Integer>();
		Thread t=new Thread(new ARunnable(cList));
		t.start();
		t=new Thread(new BRunnable(cList));
		t.start();
		
		/*
		// part 2
		ConcurrentLinkedQueue<Integer> cList=new ConcurrentLinkedQueue<Integer>();
		Thread t=new Thread(new CRunnable(cList));
		t.start();
		t=new Thread(new DRunnable(cList));
		t.start();
		*/
		
		
		
		
	}

static class ARunnable implements Runnable{
	ConcurrentListSPSC<Integer> cList=new ConcurrentListSPSC<Integer>();
	public void run() {
		Timer timer=new Timer();
		timer.startTimer();
		for (int i=0;i<10000000;i++){
			cList.add(i);
		}
		timer.endTimer();
		timer.printMeasuredTime("time required for ConcurrentList (producer): ");
	}
	public ARunnable(ConcurrentListSPSC<Integer> cList){
		super();
		this.cList=cList;
	}
}

static class BRunnable implements Runnable{
	ConcurrentListSPSC<Integer> cList=new ConcurrentListSPSC<Integer>();

	public void run() {
		Timer timer=new Timer();
		timer.startTimer();
		int count=0;
		while (count<10000){
			if (cList.remove()!=null){
				count++;
			}
			try {
				Thread.currentThread().sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("consumed Items:" + count);
		timer.endTimer();
		timer.printMeasuredTime("time required for ConcurrentList (consumer): ");
	}
	public BRunnable(ConcurrentListSPSC<Integer> cList){
		super();
		this.cList=cList;
	}
}

static class CRunnable implements Runnable{
	ConcurrentLinkedQueue<Integer> cList=new ConcurrentLinkedQueue<Integer>();
	public void run() {
		Timer timer=new Timer();
		timer.startTimer();
		for (int i=0;i<10000000;i++){
			cList.add(i);
		}
		timer.endTimer();
		timer.printMeasuredTime("time required for ConcurrentLinkedQueue (producer): ");
	}
	public CRunnable(ConcurrentLinkedQueue<Integer> cList){
		super();
		this.cList=cList;
	}
}

static class DRunnable implements Runnable{
	ConcurrentLinkedQueue<Integer> cList=new ConcurrentLinkedQueue<Integer>();
	public void run() {
		Timer timer=new Timer();
		timer.startTimer();
		int count=0;
		while (count<10000){
			if (cList.poll()!=null){
				count++;
				try {
					Thread.currentThread().sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		System.out.println("consumed Items:" + count);
		timer.endTimer();
		timer.printMeasuredTime("time required for ConcurrentLinkedQueue (consumer): ");
	}
	public DRunnable(ConcurrentLinkedQueue<Integer> cList){
		super();
		this.cList=cList;
	}
}
	
	
}

