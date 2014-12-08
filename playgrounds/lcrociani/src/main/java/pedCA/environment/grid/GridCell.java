package pedCA.environment.grid;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class GridCell <T> {
	private ArrayList <T> objects;

	public GridCell (){
		objects = new ArrayList<T>();
	}
	
	public void add(T object){
		objects.add(object);
	}
	
	
	/**
	 * Set the input object to the index. If forced,
	 * it creates null elements to fill objects list, while level > objects.size.
	 * */
	public void set(int level, T object, boolean force){
		try{
			set(level, object);
		}catch(IndexOutOfBoundsException e){
			if (force==true){
				int oldSize = objects.size();
				for(int i=0;i<level-oldSize;i++)
					objects.add(null);
				objects.add(object);
			}else throw e;
		}
	}
	
	public void set(int level, T object){
		objects.set(level, object);
	}
	
	/**
	 * Returns the object at input level.
	 * If the level is out of bound, it returns null.
	 * */
	public T get(int level){
		try{
			return objects.get(level);
		}catch(IndexOutOfBoundsException e){
			return null;
		}
	}
	
	public void remove(T object) throws NoSuchElementException{
		if(!objects.contains(object))
			throw new NoSuchElementException();
		objects.remove(object);
	}
	
	public boolean contains(T object){
		return objects.contains(object);
	}
	
	public int size(){
		return objects.size();
	}
	
	public ArrayList<T> getObjects() {
		return objects;
	}
	
	public String toString(){
		String res = "";
		for(T el : objects){
			res+=el.toString();
		}
		return res;
	}
	
}
