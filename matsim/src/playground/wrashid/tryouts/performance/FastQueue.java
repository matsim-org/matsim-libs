package playground.wrashid.tryouts.performance;

import java.util.Collection;
import java.util.Iterator;

public class FastQueue<T> {

	// max length
	private int maxDimentions=30;
	private static int minQueueSize=256;
	private T[][] array= (T[][]) new Object[maxDimentions][0];
	private int headDimention, headIndex;
	private int tailDimention, tailIndex;
	private int size=0;
	
	public FastQueue(){
		headDimention=0;
		headIndex=0;
		tailIndex=0;
		tailDimention=0;
		array[headDimention]=(T[]) new Object[minQueueSize];
	}
	
	public FastQueue(int initialCapacity){
		headDimention=0;
		headIndex=0;
		tailIndex=0;
		tailDimention=0;
		array[headDimention]=(T[]) new Object[initialCapacity];
	}
	
	public FastQueue(int initialCapacity, int maxDimentions){
		headDimention=0;
		headIndex=0;
		tailIndex=0;
		tailDimention=0;
		this.maxDimentions=maxDimentions;
		array[headDimention]=(T[]) new Object[initialCapacity];
	}
	
	
	// don't insert null values, because they are just inserted as normal objects
	public void add(T t){
		if (array[tailDimention]==null || array[tailDimention].length==tailIndex){
			// need to create new array perhaps
			tailDimention=(tailDimention+1) % maxDimentions;
			if (array[tailDimention]==null || array[tailDimention].length==0){
				// the length of the next dimention is proportional to the current
				// length of the queue. reasoning: we could have a long lived queue
				// with lots of adds and removes. So, the total memory of the queue should
				// not grow too much
				// because, size might be zero, we must use some min size
				array[tailDimention]=(T[]) new Object[Math.max(minQueueSize, size()*2)];
			} else {
				// not enough space
				// this should cause an out of boundry exception
				array[tailDimention][array[tailDimention].length]=null;
			}
			tailIndex=0;
		}
		try{
		array[tailDimention][tailIndex]=t;
		}catch (Exception e){
			System.out.println();
		}
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
				headDimention=(headDimention+1) % maxDimentions ;
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
