package playground.wrashid.DES.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class FastQueue<T> {

	// max length limited by 2^8*2^22
	private int maxDimentions=22;
	private T[][] array= (T[][]) new Object[maxDimentions][0];
	private int headDimention, headIndex;
	private int tailDimention, tailIndex;
	private int size=0;
	
	public FastQueue(){
		headDimention=0;
		headIndex=0;
		tailIndex=0;
		tailDimention=0;
		array[headDimention]=(T[]) new Object[256];
	}
	
	public FastQueue(int initialCapacity){
		headDimention=0;
		headIndex=0;
		tailIndex=0;
		tailDimention=0;
		array[headDimention]=(T[]) new Object[initialCapacity];
	}
	
	// don't insert null values!!!
	public void add(T t){
		if (array[tailDimention].length==tailIndex){
			// need to create new array perhaps
			tailDimention++;
			if (array[tailDimention].length==0){
				array[tailDimention]=(T[]) new Object[array[tailDimention-1].length*2];
			}
			tailIndex=0;
		}
		array[tailDimention][tailIndex]=t;
		tailIndex++;
		size++;
	}
	
	// if it is null, then the value of the queue is zero
	public T poll(){
		T t=null;
		
		if (size!=0){
			t=array[headDimention][headIndex];
			
			headIndex++;
			if (headIndex==array[headDimention].length){
				// release unused array
				array[headDimention]=null;
				headDimention++;
				headIndex=0;
			}
			size--;
		}
		
		return t;
	}
	
	// be prepared for out of boundry exceptions if you try to
	// access non existing elements
	public T get(int index){
		// if index in same dimention as headDimention
		if (array[headDimention].length-headIndex-index>0){
			return array[headDimention][headIndex+index];
		}
		
		int i=index;
		i=index-(array[headDimention].length-headIndex);
		for (int j=headDimention+1;j<maxDimentions-headDimention;j++){
			if (i>=array[j].length){
				i-=array[j].length;
			} else {
				return array[j][i];
			}
		}
		
		return null;
	}
	
	public void addAll(Collection<T> coll){
		Iterator<T> iterator=coll.iterator();
		
		while (iterator.hasNext()){
			add(iterator.next());
		}
	}
	
	public void addAll(FastQueue<T> coll){
		for (int i=0;i<coll.size();i++){
			add(coll.get(i));
		}
	}
	
	public int size(){
		return size;
	}
	
	
	public static void main(String[] args) {
		FastQueue<Integer> fq=new FastQueue<Integer>();
	}
	
	
}
