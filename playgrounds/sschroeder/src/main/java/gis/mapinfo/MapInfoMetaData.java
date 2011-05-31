package gis.mapinfo;

import java.util.ArrayList;
import java.util.List;

public class MapInfoMetaData {
	private List<String> columnNames = new ArrayList<String>();

	private List<String> columnValueTypes = new ArrayList<String>();
	
	private String head;
	
	private String projection;
	
	public List<String> getColumnNames() {
		return columnNames;
	}
	
	public List<String> getColumnValueTypes() {
		return columnValueTypes;
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}
}