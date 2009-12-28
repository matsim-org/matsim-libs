package playground.anhorni.crossborder;

import java.util.ArrayList;

import org.matsim.core.network.NetworkLayer;

abstract class Parser {
	
	
	protected ArrayList<Relation> relations;
	protected String file;
	protected NetworkLayer network;

	public Parser() {}
	
	public Parser(NetworkLayer network, String file) {
		this.relations=new ArrayList<Relation>();
		this.file=file;
		this.network=network;
	}
	
	public ArrayList<Relation> getRelations() {
		return relations;
	}

	public abstract int parse(String type, int startTime, int actPersonNumber);
}
