package playground.artemc.analysis.postgresql;

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

	final String extraParams;
	final String name;
	final PostgresType type;
}
