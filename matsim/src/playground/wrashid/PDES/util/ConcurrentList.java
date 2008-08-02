package playground.wrashid.PDES.util;

import java.util.LinkedList;

public class ConcurrentList<T> {
	private LinkedList<T> inputBuffer=new LinkedList<T>();
	private LinkedList<T> outputBuffer=new LinkedList<T>(); 
	public void add(T element){
		synchronized (inputBuffer){
			inputBuffer.add(element);
		}
	}
	
	public T remove(){
		if (!outputBuffer.isEmpty()){
			return outputBuffer.poll();
		}
		if (!inputBuffer.isEmpty()){
			synchronized (inputBuffer){
				LinkedList<T> tempList=new LinkedList<T>(); 
				tempList=inputBuffer;
				inputBuffer=outputBuffer;
				outputBuffer=tempList;
			}
			return outputBuffer.poll();
		}
		return null;
	}
	
	public static void main(String[] args){
		ConcurrentList<Integer> cl=new ConcurrentList<Integer>();
		for (int i=0;i<100;i++){
			cl.add(i);
		}
		Integer i=cl.remove();
		while (i!=null){
			System.out.println(i);
			i=cl.remove();
		}
		for (int j=0;j<100;j++){
			cl.add(j);
		}
		i=cl.remove();
		while (i!=null){
			System.out.println(i);
			i=cl.remove();
		}
		
		
	}
	
	
}
