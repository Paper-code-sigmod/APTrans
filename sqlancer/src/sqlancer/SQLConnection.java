package sqlancer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLConnection {

    private final Connection connection;

    public SQLConnection(Connection connection) {
        this.connection = connection;
    }

    public Statement prepareStatement(String arg) throws SQLException {
        return connection.prepareStatement(arg);
    }

    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }
}
