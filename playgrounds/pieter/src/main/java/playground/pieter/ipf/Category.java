package playground.pieter.ipf;

import java.util.TreeSet;

class Category {
	private final String name;
	private final TreeSet<String> levels;

	public Category(String name) {
		this.name = name;
		this.levels = new TreeSet<>();
	}

	String getName() {
		return name;
	}

	public TreeSet<String> getLevels() {
		return levels;
	}

	public void addLevel(String s) {
		this.levels.add(s);
	}

	public boolean equals(Category o) {
        return this.name.equals(o.getName());
	}
}
