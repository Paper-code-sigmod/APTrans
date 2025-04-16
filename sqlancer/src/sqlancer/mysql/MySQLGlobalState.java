
package sqlancer.mysql;

import java.sql.SQLException;

import sqlancer.DBMSSpecificOptions;
import sqlancer.SQLGlobalState;

public class MySQLGlobalState extends SQLGlobalState<DBMSSpecificOptions<?>, MySQLSchema> {

    @Override
    protected MySQLSchema readSchema() throws SQLException {
        return MySQLSchema.fromConnection(getConnection(), getDatabaseName());
    }


}
