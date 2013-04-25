package playground.pieter.singapore.utils.postgresql;

public class PostgresqlColumnDefinition {


	public PostgresqlColumnDefinition(String name, PostgresType type,
			String extraParams) {
		super();
		this.name = name;
		this.type = type;
		this.extraParams = extraParams;
	}

	public PostgresqlColumnDefinition(String name, PostgresType type) {
		super();
		this.name = name;
		this.type = type;
		this.extraParams = "";
	}

	String extraParams;
	String name;
	PostgresType type;
}
