package sqlancer.mysql.ast;
import sqlancer.Randomly;
import sqlancer.mysql.gen.MySQLExpressionGenerator.ExpressionType;

public class MySQLCastOperation implements MySQLExpression {

    private final MySQLExpression expr;
    private final CastType type;

    public enum CastType {
        SIGNED, UNSIGNED, CHAR, DATE, DOUBLE;

        public static CastType getRandom() {
            return SIGNED;
            // return Randomly.fromOptions(CastType.values());
        }

        public static CastType getRandom(ExpressionType type) {
            switch (type){
                case INT:
                    return Randomly.fromOptions(SIGNED, UNSIGNED);
                case DOUBLE:
                    return Randomly.fromOptions(SIGNED, UNSIGNED, DOUBLE);
                case STRING:
                    return Randomly.fromOptions(CHAR);
                default:
                    throw new AssertionError();
            }
        }

    }

    public MySQLCastOperation(MySQLExpression expr, CastType type) {
        this.expr = expr;
        this.type = type;
    }

    public MySQLExpression getExpr() {
        return expr;
    }

    public CastType getType() {
        return type;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        return expr.getExpectedValue().castAs(type);
    }

}
