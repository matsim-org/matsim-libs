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

/**
 * @author nagel
 *
 */
public class Generics {

	class Item {
		@Override
		public String toString() {
			return "Item" ;
		}
	}

	class Box<T> {
		private T item = null ;
		void addItem( T iii ) {
			this.item = iii ;
		}
		T getItem() {
			return this.item ;
		}

	}

	class MyItem extends Item {
		@Override
		public String toString() {
			return "MyItem" ;
		}
	}

	@SuppressWarnings("unused")
	void run() {
		// if the box contains <Item>, I can add MyItem:
		Box<Item> box = new Box<Item>() ;
		box.addItem( new MyItem() ) ;
		Item item = box.getItem();

		// what happens of the box contains <? extends Item>:
		Box<? extends Item> box2 = new Box<Item>() ;

		// I canNOT add MyItem any more:
		//			box.addItem( new MyItem() ) ;
		box.addItem( new Item() ) ;
		Item item2 = box2.getItem();

		// what was Marcels example?
		box2 = box ;

		// I would now be able to add MyItem to box and thus have it in box2.
		// (but I just did that, didn't i???)

		System.out.println( box.getItem() ) ;
		System.out.println( box2.getItem() ) ;
		
		// the wildcards problem comes when we have the box as an argument:
		doSomething( box ) ; // ok
//		doSomething( new Box<MyItem>() ) ; // not ok, since Box<MyItem> is not an extension of Box<Item>
		
		// instead tell the method that you accept such things:
		doSomethingElse( box ) ; // ok
		doSomethingElse( new Box<MyItem>() ) ;

	}

	private void doSomethingElse(Box<? extends Item> box) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}



	private void doSomething(Box<Item> box) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}



	public static void main(String[] args) {
		new Generics().run() ;
	}

}
