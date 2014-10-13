package playground.pieter.ipf;

import java.util.HashSet;

class Categories {
	private final HashSet<Category> categories;

	public Categories() {
		this.categories = new HashSet<>();
	}
	
	public void addCategory(Category category){
		//don't need to check for names, done by comparator
		this.categories.add(category);
	}
}
