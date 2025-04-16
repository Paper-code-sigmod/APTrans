package sqlancer.postgres.ast;

import java.math.BigDecimal;

import sqlancer.IgnoreMeException;
import sqlancer.postgres.PostgresSchema.PostgresDataType;

public abstract class PostgresConstant implements PostgresExpression {

    public abstract String getTextRepresentation();

    public abstract String getUnquotedTextRepresentation();

    public static class BooleanConstant extends PostgresConstant {

        public final boolean val;

        public BooleanConstant(boolean val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return val ? "TRUE" : "FALSE";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.BOOLEAN;
        }

        @Override
        public boolean asBoolean() {
            return val;
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return PostgresConstant.createBooleanConstant(val == rightVal.asBoolean());
            } else if (rightVal.isString()) {
                return PostgresConstant
                        .createBooleanConstant(val == rightVal.cast(PostgresDataType.BOOLEAN).asBoolean());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isString()) {
                return isLessThan(rightVal.cast(PostgresDataType.BOOLEAN));
            } else {
                assert rightVal.isBoolean();
                return PostgresConstant.createBooleanConstant((val ? 1 : 0) < (rightVal.asBoolean() ? 1 : 0));
            }
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            switch (type) {
            case BOOLEAN:
                return this;
            case INT:
                return PostgresConstant.createIntConstant(val ? 1 : 0);
            case TEXT:
                return PostgresConstant.createTextConstant(val ? "true" : "false");
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class PostgresNullConstant extends PostgresConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            return PostgresConstant.createNullConstant();
        }

        @Override
        public PostgresConstant isLessThan(PostgresConstant rightVal) {
            return PostgresConstant.createNullConstant();
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            return PostgresConstant.createNullConstant();
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class StringConstant extends PostgresConstant {

        public final String val;

        public StringConstant(String val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("'%s'", val.replace("'", "''"));
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(PostgresDataType.INT).isEquals(rightVal.cast(PostgresDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(PostgresDataType.BOOLEAN).isEquals(rightVal.cast(PostgresDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(val.contentEquals(rightVal.asString()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(PostgresDataType.INT).isLessThan(rightVal.cast(PostgresDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(PostgresDataType.BOOLEAN).isLessThan(rightVal.cast(PostgresDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(val.compareTo(rightVal.asString()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            if (type == PostgresDataType.TEXT) {
                return this;
            }
            String s = val.trim();
            switch (type) {
            case BOOLEAN:
                try {
                    return PostgresConstant.createBooleanConstant(Long.parseLong(s) != 0);
                } catch (NumberFormatException e) {
                }
                switch (s.toUpperCase()) {
                case "T":
                case "TR":
                case "TRU":
                case "TRUE":
                case "1":
                case "YES":
                case "YE":
                case "Y":
                case "ON":
                    return PostgresConstant.createTrue();
                case "F":
                case "FA":
                case "FAL":
                case "FALS":
                case "FALSE":
                case "N":
                case "NO":
                case "OF":
                case "OFF":
                default:
                    return PostgresConstant.createFalse();
                }
            case INT:
                try {
                    return PostgresConstant.createIntConstant(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    return PostgresConstant.createIntConstant(-1);
                }
            case TEXT:
                return this;
            default:
                return null;
            }
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.TEXT;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String asString() {
            return val;
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return val;
        }

    }

    public static class IntConstant extends PostgresConstant {

        public final long val;

        public IntConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.INT;
        }

        @Override
        public long asInt() {
            return val;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return cast(PostgresDataType.BOOLEAN).isEquals(rightVal);
            } else if (rightVal.isInt()) {
                return PostgresConstant.createBooleanConstant(val == rightVal.asInt());
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(val == rightVal.cast(PostgresDataType.INT).asInt());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return PostgresConstant.createBooleanConstant(val < rightVal.asInt());
            } else if (rightVal.isBoolean()) {
                throw new AssertionError(rightVal);
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(val < rightVal.cast(PostgresDataType.INT).asInt());
            } else {
                throw new IgnoreMeException();
            }

        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            switch (type) {
            case BOOLEAN:
                return PostgresConstant.createBooleanConstant(val != 0);
            case INT:
                return this;
            case TEXT:
                return PostgresConstant.createTextConstant(String.valueOf(val));
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static PostgresConstant createNullConstant() {
        return new PostgresNullConstant();
    }

    public String asString() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isString() {
        return false;
    }

    public static PostgresConstant createIntConstant(long val) {
        return new IntConstant(val);
    }

    public static PostgresConstant createBooleanConstant(boolean val) {
        return new BooleanConstant(val);
    }

    @Override
    public PostgresConstant getExpectedValue() {
        return this;
    }

    public boolean isNull() {
        return false;
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException(this.toString());
    }

    public static PostgresConstant createFalse() {
        return createBooleanConstant(false);
    }

    public static PostgresConstant createTrue() {
        return createBooleanConstant(true);
    }

    public long asInt() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isBoolean() {
        return false;
    }

    public abstract PostgresConstant isEquals(PostgresConstant rightVal);

    public boolean isInt() {
        return false;
    }

    public abstract PostgresConstant isLessThan(PostgresConstant rightVal);

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    public abstract PostgresConstant cast(PostgresDataType type);

    public static PostgresConstant createTextConstant(String string) {
        return new StringConstant(string);
    }

    public abstract static class PostgresConstantBase extends PostgresConstant {

        @Override
        public String getUnquotedTextRepresentation() {
            return null;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            return null;
        }

        @Override
        public PostgresConstant isLessThan(PostgresConstant rightVal) {
            return null;
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            return null;
        }
    }

    public static class DecimalConstant extends PostgresConstantBase {

        public final BigDecimal val;

        public DecimalConstant(BigDecimal val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.DECIMAL;
        }

    }

    public static class InetConstant extends PostgresConstantBase {

        public final String val;

        public InetConstant(String val) {
            this.val = "'" + val + "'";
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.INET;
        }

    }

    public static class FloatConstant extends PostgresConstantBase {

        public final float val;

        public FloatConstant(float val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(val)) {
                return String.valueOf(val);
            } else {
                return "'" + val + "'";
            }
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.FLOAT;
        }

    }

    public static class DoubleConstant extends PostgresConstantBase {

        public final double val;

        public DoubleConstant(double val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("%.4f", val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.FLOAT;
        }

    }

    public static class BitConstant extends PostgresConstantBase {

        public final long val;

        public BitConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("B'%s'", Long.toBinaryString(val));
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.BIT;
        }

    }

    public static class RangeConstant extends PostgresConstantBase {

        public final long left;
        public final boolean leftIsInclusive;
        public final long right;
        public final boolean rightIsInclusive;

        public RangeConstant(long left, boolean leftIsInclusive, long right, boolean rightIsInclusive) {
            this.left = left;
            this.leftIsInclusive = leftIsInclusive;
            this.right = right;
            this.rightIsInclusive = rightIsInclusive;
        }

        @Override
        public String getTextRepresentation() {
            StringBuilder sb = new StringBuilder();
            sb.append("'");
            if (leftIsInclusive) {
                sb.append("[");
            } else {
                sb.append("(");
            }
            sb.append(left);
            sb.append(",");
            sb.append(right);
            if (rightIsInclusive) {
                sb.append("]");
            } else {
                sb.append(")");
            }
            sb.append("'");
            sb.append("::int4range");
            return sb.toString();
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.RANGE;
        }

    }

    public static PostgresConstant createDecimalConstant(BigDecimal bigDecimal) {
        return new DecimalConstant(bigDecimal);
    }

    public static PostgresConstant createFloatConstant(float val) {
        return new FloatConstant(val);
    }

    public static PostgresConstant createDoubleConstant(double val) {
        return new DoubleConstant(val);
    }

    public static PostgresConstant createRange(long left, boolean leftIsInclusive, long right,
            boolean rightIsInclusive) {
        long realLeft;
        long realRight;
        if (left > right) {
            realRight = left;
            realLeft = right;
        } else {
            realLeft = left;
            realRight = right;
        }
        return new RangeConstant(realLeft, leftIsInclusive, realRight, rightIsInclusive);
    }

    public static PostgresExpression createBitConstant(long integer) {
        return new BitConstant(integer);
    }

    public static PostgresExpression createInetConstant(String val) {
        return new InetConstant(val);
    }

}
