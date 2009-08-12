package playground.christoph.knowledge.container;

public interface DBStorage {

	public void writeToDB();
	
	public void readFromDB();
	
	public void clearLocalKnowledge();

	public void createTable();
	
	public void clearTable();
}
