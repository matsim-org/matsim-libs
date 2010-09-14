package playground.anhorni.crossborder;

import java.util.ArrayList;

import org.matsim.core.network.NetworkImpl;

abstract class Parser {
	
	
	protected ArrayList<Relation> relations;
	protected String file;
	protected NetworkImpl network;

	public Parser() {}
	
	public Parser(NetworkImpl network, String file) {
		this.relations=new ArrayList<Relation>();
		this.file=file;
		this.network=network;
	}
	
	public ArrayList<Relation> getRelations() {
		return relations;
	}

	public abstract int parse(String type, int startTime, int actPersonNumber);
}
