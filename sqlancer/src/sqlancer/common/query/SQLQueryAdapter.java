package sqlancer.common.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import sqlancer.GlobalState;
import sqlancer.SQLConnection;

public class SQLQueryAdapter extends Query<SQLConnection> {

    private final String query;
    private final ExpectedErrors expectedErrors;
    private final boolean couldAffectSchema;

    public SQLQueryAdapter(String query) {
        this(query, new ExpectedErrors());
    }

    public SQLQueryAdapter(String query, ExpectedErrors expectedErrors) {
        this(query, expectedErrors, guessAffectSchemaFromQuery(query));
    }

    private static boolean guessAffectSchemaFromQuery(String query) {
        return query.contains("CREATE TABLE") && !query.startsWith("EXPLAIN");
    }

    public SQLQueryAdapter(String query, ExpectedErrors expectedErrors, boolean couldAffectSchema) {
        this(query, expectedErrors, couldAffectSchema, true);
    }

    public SQLQueryAdapter(String query, ExpectedErrors expectedErrors, boolean couldAffectSchema,
            boolean canonicalizeString) {
        if (canonicalizeString) {
            this.query = canonicalizeString(query);
        } else {
            this.query = query;
        }
        this.expectedErrors = expectedErrors;
        this.couldAffectSchema = couldAffectSchema;
        checkQueryString();
    }

    private String canonicalizeString(String s) {
        if (s.endsWith(";")) {
            return s;
        } else if (!s.contains("--")) {
            return s + ";";
        } else {
            // query contains a comment
            return s;
        }
    }

    private void checkQueryString() {
        if (!couldAffectSchema && guessAffectSchemaFromQuery(query)) {
            throw new AssertionError("CREATE TABLE statements should set couldAffectSchema to true");
        }
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public <G extends GlobalState<?, ?, SQLConnection>> boolean execute(G globalState, String... fills)
            throws SQLException { // 在数据库中运行SQL语句
        Statement s;
        if (fills.length > 0) {
            s = globalState.getConnection().prepareStatement(fills[0]);
            for (int i = 1; i < fills.length; i++) {
                ((PreparedStatement) s).setString(i, fills[i]);
            }
        } else {
            s = globalState.getConnection().createStatement();
        }
        try {
            if (fills.length > 0) {
                ((PreparedStatement) s).execute();
            } else {
                s.execute(query);
            }
            return true;
        } catch (Exception e) {
            checkException(e);
            return false;
        } finally {
            s.close();
        }
    }

    public void checkException(Exception e) throws AssertionError {
        Throwable ex = e;

        while (ex != null) {
            if (expectedErrors.errorIsExpected(ex.getMessage())) {
                return;
            } else {
                ex = ex.getCause();
            }
        }

        throw new AssertionError(query, e);
    }

    @Override
    public <G extends GlobalState<?, ?, SQLConnection>> SQLancerResultSet executeAndGet(G globalState, String... fills)
            throws SQLException {
        Statement s;
        if (fills.length > 0) {
            s = globalState.getConnection().prepareStatement(fills[0]);
            for (int i = 1; i < fills.length; i++) {
                ((PreparedStatement) s).setString(i, fills[i]);
            }
        } else {
            s = globalState.getConnection().createStatement();
        }
        ResultSet result;
        try {
            if (fills.length > 0) {
                result = ((PreparedStatement) s).executeQuery();
            } else {
                result = s.executeQuery(query);
            }
            if (result == null) {
                return null;
            }
            return new SQLancerResultSet(result);
        } catch (Exception e) {
            s.close();
            checkException(e);
        }
        return null;
    }

    @Override
    public String getLogString() {
        return getQueryString();
    }
}
