/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nagel
 *
 */
public class Generics2 {
	
	class Item {}
	
	class MyItem extends Item {}
	
	interface ContainerI {
		List<Item> getList() ;
	}
	
	// the following is not possible:
//	class MyContainer implements ContainerI {
//		private List<MyItem> list = new ArrayList<MyItem>() ;
//		@Override public List<MyItem> getList() { return list ; } 
//	}
	
	// to allow something like the above, the <? extends ...> syntax was introduced:
	interface Container2I {
		List<? extends Item> getList() ;
	}
	class MyContainer2 implements Container2I {
		private List<MyItem> list = new ArrayList<MyItem>() ;
		@Override public List<MyItem> getList() { return list ; } 
	}
	
	// However, we cannot implement <? extends Item>:
	class MyContainer3 implements Container2I {
//		private List<? extends Item> list = new ArrayList<? extends Item>() ;
		private List<? extends Item> list = new ArrayList<Item>() ;
		@Override public List<? extends Item> getList() { return list ; } 
	}
	
	// So the implementation of Container2I has to pick some actual Object type.  But since the interface does not know 
	// which type that is, it cannot add any object just from the interface.

	void run() {
		// as we know, we can put extended objects into a list (here: MyItem):
		List<Item> firstList = new ArrayList<Item>() ;
		firstList.add( new Item() ) ;
		firstList.add( new MyItem() ) ;
		
		// however, we cannot specialize the list:
//		List<Item> secondList = new ArrayList<MyItem>() ;
		// (this would not be an issue here, but  
		
	}

}
