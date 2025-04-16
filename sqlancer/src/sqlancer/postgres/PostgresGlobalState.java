package sqlancer.postgres;
import java.sql.SQLException;

import sqlancer.DBMSSpecificOptions;
import sqlancer.SQLGlobalState;

public class PostgresGlobalState extends SQLGlobalState<DBMSSpecificOptions<?>, PostgresSchema> {

    @Override
    public PostgresSchema readSchema() throws SQLException {
        return PostgresSchema.fromConnection(getConnection(), getDatabaseName());

    }
}
