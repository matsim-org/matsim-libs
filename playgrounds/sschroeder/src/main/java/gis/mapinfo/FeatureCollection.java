/**
 * 
 */
package gis.mapinfo;

import java.util.ArrayList;
import java.util.List;



/**
 * @author stefan
 *
 */
public class FeatureCollection {
	
	private MapInfoMetaData metaData = new MapInfoMetaData();
	
	private List<Feature> features = new ArrayList<Feature>();

	public void addColumn(String columnName, String valueType, String defaultValue){
		columnName = columnName.toUpperCase();
		checkColumnName(columnName);
		metaData.getColumnNames().add(columnName);
		metaData.getColumnValueTypes().add(valueType);
		for(Feature feature : features){
			feature.getFeatureData().getAttributes().put(columnName, defaultValue);
		}
	}

	public MapInfoMetaData getMetaData() {
		return metaData;
	}

	public List<Feature> getFeatures() {
		return features;
	}

	private void checkColumnName(String newColumn) {
		for(String column : metaData.getColumnNames()){
			if(column.equals(column.equals(newColumn))){
				throw new RuntimeException("column " + newColumn + " already exists");
			}
		}
		return;
	}
	
}
