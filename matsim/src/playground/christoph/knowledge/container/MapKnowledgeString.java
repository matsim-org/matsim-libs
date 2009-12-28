package playground.christoph.knowledge.container;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

/*
 * Reads and Writes a Person's known Nodes to a String.
 */
public class MapKnowledgeString extends MapKnowledge implements DBStorage{
	
	private final static Logger log = Logger.getLogger(MapKnowledgeString.class);
	
	private boolean localKnowledge;

	private String nodesString;
	private String separator = "@";
	
	public MapKnowledgeString()
	{
		super();
		localKnowledge = true;
	}

/*
	public MapKnowledgeString(Map<Id, NodeImpl> nodes)
	{
		super(nodes);
		localKnowledge = true;
	}
*/	
	
	@Override
	public boolean knowsNode(Node node)
	{
		readFromDB();
		
		return super.knowsNode(node);
	}
	
	
	@Override
	public boolean knowsLink(Link link)
	{
		readFromDB();
		
		return super.knowsLink(link);
	}
	
	
	@Override
	public Map<Id, Node> getKnownNodes()
	{
		readFromDB();
		
		return super.getKnownNodes();
	}
	
	@Override
	public void setKnownNodes(Map<Id, Node> nodes)
	{
		super.setKnownNodes(nodes);
		
		this.writeToDB();
	}
	
	public synchronized void readFromDB()
	{	
		// If Knowledge is currently not in Memory, get it from the DataBase
		if (!localKnowledge)
		{
			this.clearLocalKnowledge();
			
			localKnowledge = true;
			
			String[] nodeIds = nodesString.split(this.separator);
			
			for (String id : nodeIds)
			{					
				//NodeImpl node = this.network.getNode(new IdImpl(id));
				Node node = this.network.getNodes().get(new IdImpl(id));
				super.addNode(node);
			}
		}
	}
	
	public synchronized void writeToDB()
	{
		Map<Id, Node> nodes = super.getKnownNodes();
		
		nodesString = createNodesString(nodes);
	}

	
	public void clearLocalKnowledge()
	{
		localKnowledge = false;
		super.getKnownNodes().clear();
	}
	
	
	public void clearTable()
	{
	}
		
	private String createNodesString(Map<Id, Node> nodes)
	{
		// if no Nodes are known -> just return a separator
		if (nodes.values().size() == 0) return this.separator;
		
		String string = "";
		
		for (Node node : nodes.values())
		{
			string = string + node.getId() + this.separator;
		}
			
		return string;
	}
	
	public void createTable() {
		// TODO Auto-generated method stub
		
	}
}