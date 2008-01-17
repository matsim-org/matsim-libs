package playground.david.vis.gui;

import java.util.ArrayList;

public class PoolFactory<ObjectType> {
	private ArrayList<ObjectType> array;
	private int initialSize = 10000;

	private Class<ObjectType> classObject;
	private int usage = 0;
	
	PoolFactory(Class<ObjectType> c, int initialSize){
		this.array = new ArrayList<ObjectType> (initialSize);
		this.classObject = c;
	}

	public void reset() {
		usage= 0;
	}
	public Class<ObjectType> getClientClass() {
		return classObject;
	}
	
	public ObjectType getOne(){
		array.ensureCapacity(usage);
		ObjectType result = array.get(usage++);
		if (result == null)
			try {
				result = classObject.newInstance();
				array.add(result);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		usage++;
		return result;
	}
	
}
