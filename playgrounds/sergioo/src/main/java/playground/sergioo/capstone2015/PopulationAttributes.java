package playground.sergioo.capstone2015;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class PopulationAttributes {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		ObjectAttributes personAttributes = new ObjectAttributes();
		DataBaseAdmin dataBase  = new DataBaseAdmin(new File(args[0]));
		ResultSet resultSet = dataBase.executeQuery("SELECT * FROM "+args[1]);
		while(resultSet.next())
			personAttributes.putAttribute(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3));
		resultSet.close();
		new ObjectAttributesXmlWriter(personAttributes).writeFile(args[2]);
	}

}
