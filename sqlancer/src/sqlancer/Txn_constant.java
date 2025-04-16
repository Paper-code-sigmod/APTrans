package sqlancer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import sqlancer.MainOptions;
// Txn_constant.java
public class Txn_constant {
    static MainOptions options = new MainOptions();
    public static int VAL = 0;
    public static int ID = 0;
    
    public enum IsolationOption {
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }

    public static IsolationOption Isolation = IsolationOption.SERIALIZABLE;
    public static String isolation_level = "SERIALIZABLE";

    public enum Pattern {
        // 单变量
        WWW("W1xW2xW1xC1cC2c"),
        WWCW("W1xW2xC2cW1xC1c"),
        WWR("W1xW2xR1xC1cC2c"),
        WWCR("W1xW2xC2cR1xC1c"),
        WRW("W1xR2xW1xC1cC2c"),
        WRCW("W1xR2xC2cW1xC1c"),
        RWW("R1xW2xW1xC1cC2c"),
        RWCW("R1xW2xC2cW1xC1c"),
        RWR("R1xW2xR1xC1cC2c"),
        RWCR("R1xW2xC2cR1xC1c"),
        // 双变量
        // ww-
        WWWW1("W1xW2xW2yW1yC1cC2c"),
        WWWW2("W1xW2yW2xW1yC1cC2c"),
        WWWW3("W1xW2yW1yW2xC1cC2c"),
        WWWCW1("W1xW2xW2yC2cW1yC1c"),
        WWWCW2("W1xW2yW2xC2cW1yC1c"),
        WWWCW3("W1xW2yW1yC1cW2xC2c"),
        WWWR1("W1xW2xW2yR1yC1cC2c"),
        WWWR2("W1xW2yW2xR1yC1cC2c"),
        WWWR3("W1xW2yR1yW2xC1cC2c"),
        WWWCR1("W1xW2xW2yC2cR1yC1c"),
        WWWCR2("W1xW2yW2xC2cR1yC1c"),
        WWWCR3("W1xW2yR1yC1cW2xC2c"),
        WWRW1("W1xW2xR2yW1yC1cC2c"),
        WWRW2("W1xR2yW2xW1yC1cC2c"),
        WWRW3("W1xR2yW1yW2xC1cC2c"),
        WWRCW1("W1xW2xR2yC2cW1yC1c"),
        WWRCW2("W1xR2yW2xC2cW1yC1c"),
        WWRCW3("W1xR2yW1yC1cW2xC2c"),
        // wr
        WRWW1("W1xR2xW2yW1yC1cC2c"),
        WRWW2("W1xW2yR2xW1yC1cC2c"),
        WRWW3("W1xW2yW1yR2xC1cC2c"),
        WRWCW1("W1xR2xW2yC2cW1yC1c"),
        WRWCW2("W1xW2yR2xC2cW1yC1c"),
        WRWCW3("W1xW2yW1yC1cR2xC2c"),
        WRWR1("W1xR2xW2yR1yC1cC2c"),
        WRWR2("W1xW2yR2xR1yC1cC2c"),
        WRWR3("W1xW2yR1yR2xC1cC2c"),
        WRWCR1("W1xR2xW2yC2cR1yC1c"),
        WRWCR2("W1xW2yR2xC2cR1yC1c"),
        WRWCR3("W1xW2yR1yC1cR2xC2c"),
        WRRW1("W1xR2xR2yW1yC1cC2c"),
        WRRW2("W1xR2yR2xW1yC1cC2c"),
        WRRW3("W1xR2yW1yR2xC1cC2c"),
        WRRCW1("W1xR2xR2yC2cW1yC1c"),
        WRRCW2("W1xR2yR2xC2cW1yC1c"),
        WRRCW3("W1xR2yW1yC1cR2xC2c"),
        // rc
        RWWW1("R1xW2xW2yW1yC1cC2c"),
        RWWW2("R1xW2yW2xW1yC1cC2c"),
        RWWW3("R1xW2yW1yW2xC1cC2c"),
        RWWCW1("R1xW2xW2yC2cW1yC1c"),
        RWWCW2("R1xW2yW2xC2cW1yC1c"),
        RWWCW3("R1xW2yW1yC1cW2xC2c"),
        RWWR1("R1xW2xW2yR1yC1cC2c"),
        RWWR2("R1xW2yW2xR1yC1cC2c"),
        RWWR3("R1xW2yR1yW2xC1cC2c"),
        RWWCR1("R1xW2xW2yC2cR1yC1c"),
        RWWCR2("R1xW2yW2xC2cR1yC1c"),
        RWWCR3("R1xW2yR1yC1cW2xC2c"),
        RWRW1("R1xW2xR2yW1yC1cC2c"),
        RWRW2("R1xR2yW2xW1yC1cC2c"),
        RWRW3("R1xR2yW1yW2xC1cC2c"),
        RWRCW1("R1xW2xR2yC2cW1yC1c"),
        RWRCW2("R1xR2yW2xC2cW1yC1c"),
        RWRCW3("R1xR2yW1yC1cW2xC2c"),
        // jim gray exceptions
        DW("W1xW2xC1cC2c"),
        DR("W1xR2xC1cC2c"),
        DRC("R2xW1xR2xC1cC2c"),
        PR("R1XW2XC2cR1XC1c");
    
        private String patternDescription;
    
        // 构造函数，接受字符串描述
        Pattern(String patternDescription) {
            this.patternDescription = patternDescription;
        }
    
        // 获取字符串描述
        public String getPatternDescription() {
            return patternDescription;
        }

        public int length() {
            return patternDescription.length();
        }

        public char charAt(int index) {
            return patternDescription.charAt(index);
        }

        public String toString() {
            return patternDescription;
        }
    }
    
    // 静态List用于分类，隔离级别的类别
    public static final List<Pattern> RC = Arrays.asList(Pattern.DW, Pattern.DR, Pattern.DRC, Pattern.WWW, Pattern.WWCW, Pattern.WWR, Pattern.WWCR, Pattern.RWW
                                                        , Pattern.WRW, Pattern.WRCW, Pattern.RWR
                                                        , Pattern.WWWW1, Pattern.WWWW2, Pattern.WWWW3, Pattern.WWWCW1, Pattern.WWWCW2, Pattern.WWWCW3
                                                        , Pattern.WWWR1, Pattern.WWWR2, Pattern.WWWR3, Pattern.WWWCR1, Pattern.WWWCR2, Pattern.WWWCR3
                                                        , Pattern.WWRW1, Pattern.WWRW2, Pattern.WWRW3, Pattern.WWRCW1, Pattern.WWRCW2
                                                        , Pattern.WRWW1, Pattern.WRWW2, Pattern.WRWW3, Pattern.WRWCW1, Pattern.WRWCW2, Pattern.WRWCW3
                                                        , Pattern.WRWR1, Pattern.WRWR2, Pattern.WRWR3, Pattern.WRWCR1, Pattern.WRWCR2, Pattern.WRWCR3
                                                        , Pattern.WRRW1, Pattern.WRRW2, Pattern.WRRW3, Pattern.WRRCW1, Pattern.WRRCW2
                                                        , Pattern.RWWW1, Pattern.RWWR2, Pattern.RWWR3, Pattern.RWWCW3
                                                        , Pattern.RWWR1, Pattern.RWWR2, Pattern.RWWR3, Pattern.RWWCR3);

    public static final List<Pattern> RR = new ArrayList<>(Arrays.asList(Pattern.RWCW, Pattern.RWCR
                                            , Pattern.WWRCW3, Pattern.WRRCW3
                                            , Pattern.RWWCW1, Pattern.RWWCW2
                                            , Pattern.RWWCR1, Pattern.RWWCR2));
    
    public static final List<Pattern> SER = new ArrayList<>(Arrays.asList(Pattern.RWRW1, Pattern.RWRW2, Pattern.RWRW3, Pattern.RWRCW1, Pattern.RWRCW2, Pattern.RWRCW3));

    static {
        RR.addAll(RC);
        SER.addAll(RR);
    }

    // 静态List用于分类，变量数的类别
    public static final List<Pattern> ONE = Arrays.asList(Pattern.DW, Pattern.DR, Pattern.DRC, Pattern.WWW, Pattern.WWCW, Pattern.WWR, Pattern.WWCR, Pattern.RWW, Pattern.WRW, Pattern.WRCW, Pattern.RWR, Pattern.RWCW, Pattern.RWCR);

    public static final List<Pattern> TWO = Arrays.asList(Pattern.WWWW1, Pattern.WWWW2, Pattern.WWWW3, Pattern.WWWCW1, Pattern.WWWCW2, Pattern.WWWCW3
                                                        , Pattern.WWWR1, Pattern.WWWR2, Pattern.WWWR3, Pattern.WWWCR1, Pattern.WWWCR2, Pattern.WWWCR3
                                                        , Pattern.WWRW1, Pattern.WWRW2, Pattern.WWRW3, Pattern.WWRCW1, Pattern.WWRCW2, Pattern.WWRCW3
                                                        , Pattern.WRWW1, Pattern.WRWW2, Pattern.WRWW3, Pattern.WRWCW1, Pattern.WRWCW2, Pattern.WRWCW3
                                                        , Pattern.WRWR1, Pattern.WRWR2, Pattern.WRWR3, Pattern.WRWCR1, Pattern.WRWCR2, Pattern.WRWCR3
                                                        , Pattern.WRRW1, Pattern.WRRW2, Pattern.WRRW3, Pattern.WRRCW1, Pattern.WRRCW2, Pattern.WRRCW3
                                                        , Pattern.RWWW1, Pattern.RWWR2, Pattern.RWWR3, Pattern.RWWCW1, Pattern.RWWCW2, Pattern.RWWCW3
                                                        , Pattern.RWWR1, Pattern.RWWR2, Pattern.RWWR3, Pattern.RWWCR1, Pattern.RWWCR2, Pattern.RWWCR3
                                                        , Pattern.RWRW1, Pattern.RWRW2, Pattern.RWRW3, Pattern.RWRCW1, Pattern.RWRCW2, Pattern.RWRCW3);
    // 使用put()方法初始化Map，避免双花括号
    public static final Map<IsolationOption, List<Pattern>> ISOLATION_PATTERN = Map.of(
        IsolationOption.READ_COMMITTED, RC,
        IsolationOption.REPEATABLE_READ, RR,
        IsolationOption.SERIALIZABLE, SER
    );

    public static final Map<Integer, List<Pattern>> VAR_PATTERN = Map.of(
        1, ONE,
        2, TWO
    );

    // 禁止实例化这个类
    private Txn_constant() {
        throw new AssertionError("Cannot instantiate this class.");
    }

    // Scheme & table 定义
    public static final String[] TABLES = {
        "CREATE TABLE t0 (k INT PRIMARY KEY, v INT)"
    };

    public static int getID() {
        ID = ID + 1;
        return ID;
    }

    public static int getVAL() {
        VAL = VAL + 1;
        return VAL;
    }

    // 获取匹配的模式，使用求交集，再做选择
    public static Pattern getPattern(int var_num) {
        List<Pattern> varPatterns = VAR_PATTERN.get(var_num);
        List<Pattern> isoPatterns = ISOLATION_PATTERN.get(Isolation);
        
        // 创建两个集合
        List<Pattern> intersection = new ArrayList<>(varPatterns); 
        // 计算交集
        intersection.retainAll(isoPatterns);
        // 如果交集为空，报Assertion
        if (intersection.isEmpty()) {
            throw new AssertionError("No intersection found.");
        }

        // 随机选择一个交集中的元素
        Random random = new Random();
        int randomIndex = random.nextInt(intersection.size());  // 获取一个随机索引
        Pattern randomPattern = intersection.get(randomIndex);

        // 返回选择的
        return randomPattern;
    }

    public static boolean isPredicate(Pattern pattern) {
        if (pattern == Pattern.PR) {
            return true;
        }
        return false; // 返回false
    }

    public static void LOGing (String logstring) {
        System.out.println(logstring);
    }

    // 设置隔离级别的方法
    public static void setIsolationLevel() {
        switch (options.getTest_isolation().toLowerCase()) {
            case "read_committed":
                Isolation = IsolationOption.READ_COMMITTED;
                isolation_level = "READ COMMITTED";
                break;
            case "repeatable_read":
                Isolation = IsolationOption.REPEATABLE_READ;
                isolation_level = "REPEATABLE READ";
                break;
            case "serializable":
                Isolation = IsolationOption.SERIALIZABLE;
                isolation_level = "SERIALIZABLE";
                break;
            default:
                throw new IllegalArgumentException("Invalid isolation level: " + options.getTest_isolation());
        }
    }
}
