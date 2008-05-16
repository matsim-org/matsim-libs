package org.matsim.utils.vis.otfivs.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PoolFactory<ObjectType> {
	private static Map<Class, PoolFactory> allFactories = new HashMap<Class, PoolFactory>();
	
	private final ArrayList<ObjectType> array;
	private static final int initialSize = 10000;

	private final Class<ObjectType> classObject;
	private int usage = 0;
	
	private PoolFactory(Class c, int initialSize){
		this.array = new ArrayList<ObjectType> (initialSize);
		this.classObject = c;
	}

	public void reset() {
		usage= 0;
	}
	
	public void remove() {
		allFactories.remove(this.classObject);
	}
	
	public Class<ObjectType> getClientClass() {
		return classObject;
	}
	
	public ObjectType getOne(){
		ObjectType result = null;
		array.ensureCapacity(usage);
			try {
				if (usage >= array.size()) {
					result = classObject.newInstance();
					array.add(result);
					usage++;
				} else 	{
					result = array.get(usage);
					usage++;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Class object of pool factory could not be created!");
				System.exit(1);
			}
		return result;
	}
	
	public static PoolFactory get(Class c) {
		PoolFactory fac = allFactories.get(c);
		if (fac == null) {
			fac = new PoolFactory(c, initialSize);
			allFactories.put(c, fac);
		}
		return fac;
	}
	
	public static void resetAll() {
		for (PoolFactory fac : allFactories.values()) fac.reset();
	}
	
}
