package sqlancer.mysql.gen;

import java.util.ArrayList;
import java.util.List;
// import java.util.Random;

// import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.gen.UntypedExpressionGenerator;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLRowValue;
import sqlancer.mysql.ast.MySQLBetweenOperation;
import sqlancer.mysql.ast.MySQLBinaryComparisonOperation;
import sqlancer.mysql.ast.MySQLBinaryComparisonOperation.BinaryComparisonOperator;
import sqlancer.mysql.ast.MySQLBinaryLogicalOperation;
import sqlancer.mysql.ast.MySQLBinaryLogicalOperation.MySQLBinaryLogicalOperator;
import sqlancer.mysql.ast.MySQLBinaryOperation;
import sqlancer.mysql.ast.MySQLBinaryOperation.MySQLBinaryOperator;
import sqlancer.mysql.ast.MySQLCastOperation;
import sqlancer.mysql.ast.MySQLCastOperation.CastType;
import sqlancer.mysql.ast.MySQLColumnReference;
import sqlancer.mysql.ast.MySQLComputableFunction;
import sqlancer.mysql.ast.MySQLComputableFunction.MySQLFunction;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLConstant.MySQLDoubleConstant;
import sqlancer.mysql.ast.MySQLExists;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.ast.MySQLInOperation;
import sqlancer.mysql.ast.MySQLStringExpression;
import sqlancer.mysql.ast.MySQLUnaryPostfixOperation;
import sqlancer.mysql.ast.MySQLUnaryPrefixOperation;
import sqlancer.mysql.ast.MySQLUnaryPrefixOperation.MySQLUnaryPrefixOperator;

public class MySQLExpressionGenerator extends UntypedExpressionGenerator<MySQLExpression, MySQLColumn> {

    private final MySQLGlobalState state;
    private MySQLRowValue rowVal;

    public MySQLExpressionGenerator(MySQLGlobalState state) {
        this.state = state;
    }

    public MySQLExpressionGenerator setRowVal(MySQLRowValue rowVal) {
        this.rowVal = rowVal;
        return this;
    }

    private enum Actions {
        COLUMN, LITERAL, UNARY_PREFIX_OPERATION, UNARY_POSTFIX, COMPUTABLE_FUNCTION, BINARY_LOGICAL_OPERATOR,
        BINARY_COMPARISON_OPERATION, CAST, IN_OPERATION, BINARY_OPERATION, EXISTS, BETWEEN_OPERATOR;
    }

    public enum ExpressionType {
        INT, DOUBLE, STRING, BOOLEAN;
        public static ExpressionType[] valuesETypes() {
            return new ExpressionType[] { INT, DOUBLE, STRING, BOOLEAN };
        }
    }

    @Override
    public MySQLExpression generateExpression(int depth) {
//        if (depth >= state.getOptions().getMaxExpressionDepth()) {
//            return generateLeafNode();
//        }
        switch (Randomly.fromOptions(Actions.values())) {
        case COLUMN:
            return generateColumn();
        case LITERAL:
            return generateConstant();
        case UNARY_PREFIX_OPERATION:
            MySQLExpression subExpr = generateExpression(depth + 1);
            MySQLUnaryPrefixOperator random = MySQLUnaryPrefixOperator.getRandom();
            return new MySQLUnaryPrefixOperation(subExpr, random);
        case UNARY_POSTFIX:
            return new MySQLUnaryPostfixOperation(generateExpression(depth + 1),
                    Randomly.fromOptions(MySQLUnaryPostfixOperation.UnaryPostfixOperator.values()),
                    Randomly.getBoolean());
        case COMPUTABLE_FUNCTION:
            return getComputableFunction(depth + 1);
        case BINARY_LOGICAL_OPERATOR:
            return new MySQLBinaryLogicalOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    MySQLBinaryLogicalOperator.getRandom());
        case BINARY_COMPARISON_OPERATION:
            return new MySQLBinaryComparisonOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    BinaryComparisonOperator.getRandom());
        case CAST:
            return new MySQLCastOperation(generateExpression(depth + 1), MySQLCastOperation.CastType.getRandom());
        case IN_OPERATION:
            MySQLExpression expr = generateExpression(depth + 1);
            List<MySQLExpression> rightList = new ArrayList<>();
            for (int i = 0; i < 1 + Randomly.smallNumber(); i++) {
                rightList.add(generateExpression(depth + 1));
            }
            return new MySQLInOperation(expr, rightList, Randomly.getBoolean());
        case BINARY_OPERATION:
            return new MySQLBinaryOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    MySQLBinaryOperator.getRandom());
        case EXISTS:
            return getExists();
        case BETWEEN_OPERATOR:
            return new MySQLBetweenOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    generateExpression(depth + 1));
        default:
            throw new AssertionError();
        }
    }

    public MySQLExpression generateExpression(int depth, MySQLDataType dataType){
        switch (dataType) {
            case INT:
                return generateExpression(depth, ExpressionType.INT);
            case VARCHAR:
                return generateExpression(depth, ExpressionType.STRING);
            case FLOAT:
            case DOUBLE:
            case DECIMAL:
                return generateExpression(depth, ExpressionType.DOUBLE);
            case BOOLEAN:
                return generateExpression(depth, ExpressionType.BOOLEAN);
            default:
                throw new AssertionError();
        }
    }

    public List<MySQLExpression> generateProjectionColumns(int num){
        List<MySQLExpression> result = new ArrayList<>();
        for(int i = 0; i < num; i++){
            result.add(generateColumn());
        }
        return result;
    }

    public MySQLExpression generateExpression(int depth, ExpressionType type) {
//        if (depth >= state.getOptions().getMaxExpressionDepth()){
//            return generateConstant(type);
//        }

        if (Randomly.getBooleanWithSmallProbability()) {
            MySQLColumn c = FindColumn(type);
            if (c != null){
                MySQLConstant val = null;
                return MySQLColumnReference.create(c, val);
            }
        }

        Actions action;
        switch (type) {
            case INT:
            case DOUBLE:
                action = Randomly.fromOptions(Actions.LITERAL, Actions.COMPUTABLE_FUNCTION, Actions.CAST, Actions.BINARY_OPERATION);
                break;
            case STRING:
                action = Randomly.fromOptions(Actions.LITERAL, Actions.CAST);
                break;
            case BOOLEAN:
                action = Randomly.fromOptions(Actions.LITERAL, Actions.UNARY_PREFIX_OPERATION, Actions.UNARY_POSTFIX, Actions.BINARY_LOGICAL_OPERATOR,
                        Actions.BINARY_COMPARISON_OPERATION, Actions.IN_OPERATION, Actions.BINARY_OPERATION, Actions.BETWEEN_OPERATOR);
                break;
            default:
                throw new AssertionError();
        }

        return getExpression(depth, action, type);
    }

    public MySQLExpression getExpression(int depth, Actions action, ExpressionType type){
        ExpressionType tp;
        switch (action) {
            case LITERAL:
                return generateConstant(type);
            case UNARY_PREFIX_OPERATION:
                MySQLExpression subExpr = generateExpression(depth + 1, type);
                MySQLUnaryPrefixOperator op;
                if (type == ExpressionType.INT || type == ExpressionType.DOUBLE) {
                    if(Randomly.getBoolean()){
                        op = MySQLUnaryPrefixOperator.PLUS;
                    }
                    else {
                        op = MySQLUnaryPrefixOperator.MINUS;
                    }
                }
                else {
                    op = MySQLUnaryPrefixOperator.NOT;
                }
                return new MySQLUnaryPrefixOperation(subExpr, op);
            case UNARY_POSTFIX:
                return new MySQLUnaryPostfixOperation(generateExpression(depth + 1, type),
                    Randomly.fromOptions(MySQLUnaryPostfixOperation.UnaryPostfixOperator.values()),
                    Randomly.getBoolean());
            case COMPUTABLE_FUNCTION:
                return getComputableFunction(depth + 1, type);
            case BINARY_LOGICAL_OPERATOR:
                return new MySQLBinaryLogicalOperation(generateExpression(depth + 1, ExpressionType.BOOLEAN), generateExpression(depth + 1, ExpressionType.BOOLEAN),
                    MySQLBinaryLogicalOperator.getRandom());
            case BINARY_COMPARISON_OPERATION:
                tp = Randomly.fromOptions(ExpressionType.valuesETypes());
                BinaryComparisonOperator BCop = BinaryComparisonOperator.getRandom();
                if(BCop == BinaryComparisonOperator.LIKE){
                    return new MySQLBinaryComparisonOperation(generateExpression(depth + 1, ExpressionType.STRING), generateExpression(depth + 1, ExpressionType.STRING),
                            BCop);
                }
                return new MySQLBinaryComparisonOperation(generateExpression(depth + 1, tp), generateExpression(depth + 1, tp), BCop);
            case CAST:
                return getCastOperation(depth + 1, type);
            case IN_OPERATION:
                tp = Randomly.fromOptions(ExpressionType.valuesETypes());
                MySQLExpression expr = generateExpression(depth + 1, tp);
                List<MySQLExpression> rightList = new ArrayList<>();
                for (int i = 0; i < 1 + Randomly.smallNumber(); i++) {
                    rightList.add(generateExpression(depth + 1, tp));
                }
                return new MySQLInOperation(expr, rightList, Randomly.getBoolean());
            case BINARY_OPERATION:
                return getBinaryOperation(depth + 1, type);
            case BETWEEN_OPERATOR:
                tp = Randomly.fromOptions(ExpressionType.INT, ExpressionType.DOUBLE, ExpressionType.STRING);
                return new MySQLBetweenOperation(generateExpression(depth + 1, tp), generateExpression(depth + 1, tp),
                    generateExpression(depth + 1, tp));
            default:
                throw new AssertionError();
        }
    }

    private MySQLExpression getExists() {
        if (Randomly.getBoolean()) {
            return new MySQLExists(new MySQLStringExpression("SELECT 1", MySQLConstant.createTrue()));
        } else {
            return new MySQLExists(new MySQLStringExpression("SELECT 1 WHERE FALSE", MySQLConstant.createFalse()));
        }
    }

    private MySQLExpression getComputableFunction(int depth) {
        MySQLFunction func = MySQLFunction.getRandomFunction();
        int nrArgs = func.getNrArgs();
        if (func.isVariadic()) {
            nrArgs += Randomly.smallNumber();
        }
        MySQLExpression[] args = new MySQLExpression[nrArgs];
        for (int i = 0; i < args.length; i++) {
            args[i] = generateExpression(depth + 1);
        }
        return new MySQLComputableFunction(func, args);
    }

    private MySQLExpression getComputableFunction(int depth, ExpressionType type) {
        MySQLFunction func = MySQLFunction.getRandomFunction(type);
        int nrArgs = func.getNrArgs();
        if (func.isVariadic()) {
            nrArgs += Randomly.smallNumber();
        }
        MySQLExpression[] args = new MySQLExpression[nrArgs];
        if(func == MySQLFunction.IF) {
            args[0] = generateExpression(depth + 1, ExpressionType.BOOLEAN);
            args[1] = generateExpression(depth + 1, type);
            args[2] = generateExpression(depth + 1, type);
        }
        else if(func == MySQLFunction.IFNULL) {
            args[0] = generateExpression(depth + 1, Randomly.fromOptions(ExpressionType.valuesETypes()));
            args[1] = generateExpression(depth + 1, type);
        }
        else{
            for (int i = 0; i < args.length; i++) {
                args[i] = generateExpression(depth + 1, type);
            }
        } 
        return new MySQLComputableFunction(func, args);
    }

    private MySQLExpression getBinaryOperation(int depth, ExpressionType type) {
        MySQLBinaryOperator op = MySQLBinaryOperator.getRandom();
        if (type == ExpressionType.INT){
            return new MySQLBinaryOperation(generateExpression(depth + 1, ExpressionType.INT), generateExpression(depth + 1, ExpressionType.INT), op);
        }
        else{
            return new MySQLBinaryOperation(generateExpression(depth + 1, ExpressionType.BOOLEAN), generateExpression(depth + 1, ExpressionType.BOOLEAN), op);
        }
    }

    private MySQLExpression getCastOperation(int depth, ExpressionType type) {
        CastType castType = CastType.getRandom(type);
        switch (castType) {
            case SIGNED:
            case UNSIGNED:
            case DOUBLE:
                if (Randomly.getBoolean()){
                    return new MySQLCastOperation(generateExpression(depth + 1, ExpressionType.INT), castType);
                }
                else {
                    return new MySQLCastOperation(generateExpression(depth + 1, ExpressionType.DOUBLE), castType);
                }   
            case DATE:
                return new MySQLCastOperation(generateExpression(depth + 1, type), castType);
            case CHAR:
                ExpressionType target_type = Randomly.fromOptions(ExpressionType.INT, ExpressionType.DOUBLE, ExpressionType.STRING, ExpressionType.BOOLEAN);
                return new MySQLCastOperation(generateExpression(depth + 1, target_type), castType);
            default:
                throw new AssertionError(castType);
        }
    }

    private enum ConstantType {
        INT, NULL, STRING, DOUBLE;
    }

    @Override
    public MySQLExpression generateConstant() {
        ConstantType[] values;
        values = ConstantType.values();
        switch (Randomly.fromOptions(values)) {
        case INT:
            return MySQLConstant.createIntConstant((int) state.getRandomly().getInteger());
        case NULL:
            return MySQLConstant.createNullConstant();
        case STRING:
            /* Replace characters that still trigger open bugs in MySQL */
            String string = state.getRandomly().getString().replace("[","").replace("]", "").replace("\\", "").replace("\n", "");
            return MySQLConstant.createStringConstant(string);
        case DOUBLE:
            double val = state.getRandomly().getDouble();
            return new MySQLDoubleConstant(val);
        default:
            throw new AssertionError();
        }
    }

    public MySQLExpression generateConstant(MySQLDataType type) {
        if (Randomly.getBooleanWithSmallProbability()) {
            return generateConstant();
        }
        return type.getRandomValue(state, false);
    }

    public MySQLExpression generateConstant(ExpressionType type) {
        if (Randomly.getBooleanWithSmallProbability()) {
            return generateConstant();
        }
        switch (type) {
            case INT:
                return MySQLDataType.INT.getRandomValue(state, false);
            case DOUBLE:
                return MySQLDataType.DOUBLE.getRandomValue(state, false);
            case STRING:
                return MySQLDataType.VARCHAR.getRandomValue(state, false);
            case BOOLEAN:
                return MySQLDataType.BOOLEAN.getRandomValue(state, false);
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected MySQLExpression generateColumn() {
        MySQLColumn c = Randomly.fromList(columns);
        MySQLConstant val;
        if (rowVal == null) {
            val = null;
        } else {
            val = rowVal.getValues().get(c);
        }
        return MySQLColumnReference.create(c, val);
    }

    @Override
    public MySQLExpression negatePredicate(MySQLExpression predicate) {
        return new MySQLUnaryPrefixOperation(predicate, MySQLUnaryPrefixOperator.NOT);
    }

    @Override
    public MySQLExpression isNull(MySQLExpression expr) {
        return new MySQLUnaryPostfixOperation(expr, MySQLUnaryPostfixOperation.UnaryPostfixOperator.IS_NULL, false);
    }

    @Override
    public List<MySQLExpression> generateOrderBys() {
        List<MySQLExpression> newOrderBys = new ArrayList<>();
        for(int i = 0; i < columns.size(); i++) {
            MySQLExpression orderBy = generateColumn();
            newOrderBys.add(orderBy);
            if(Randomly.getBoolean()){
                break;
            }
        }
        return newOrderBys;
    }

    public MySQLExpression generateLeafNode(MySQLDataType type) {
        if (Randomly.getBoolean() && !columns.isEmpty()) {
            return generateColumn();
        } else {
            return generateConstant(type);
        }
    }

    public MySQLColumn FindColumn(ExpressionType type) {
        for (int i = 0; i < columns.size(); i++) {
            MySQLDataType dataType = columns.get(i).getType();
            switch (type) {
                case BOOLEAN:
                    if (dataType == MySQLDataType.BOOLEAN) {
                        return columns.get(i);
                    }
                    break;
                case INT:
                case DOUBLE:
                    if (dataType.isNumeric()){
                        return columns.get(i);
                    }
                    break;
                case STRING:
                    if (dataType.isString()){
                        return columns.get(i);
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }
}
