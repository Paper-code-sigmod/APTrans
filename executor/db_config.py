import argparse
import time
from enum import Enum


class DBConfig:
    def __init__(self):
        self.parser = argparse.ArgumentParser(description='Database configuration')
        self.parser.add_argument('--host', type=str, default='127.0.0.1', help='Database host')
        self.parser.add_argument('--user', type=str, default='root', help='Database user')
        self.parser.add_argument('--database', type=str, default='test', help='Database name')
        self.parser.add_argument('--database_type', type=str, default='mysql', help='Database type')
        self.parser.add_argument('--password', type=str, default='123456', help='Database password')
        self.parser.add_argument('--cases_path', type=str, default='./cases/MySQL', help='Test cases path')
        self.parser.add_argument('--isolation', type=str, default='SERIALIZABLE', help='Database isolation level: SERIALIZABLE, REPEATABLE_READ, READ_COMMITTED')
        self.parser.add_argument('--port', type=int, default=0, help='Database port')
        self.parser.add_argument("--reproduce", type=str, default="false", help="Reproduce the bug")
        self.parser.add_argument("--unique_rate", type=float, default=0.8, help="Unique ratio")
        args_tmp = self.parser.parse_args()
        self.time_out_set = "SET lock_timeout = 1000;"
        self.DBType = "mysql"
        
        if args_tmp.port == 0:
            if args_tmp.database_type == 'mysql':
                args_tmp.port = 13306
            elif args_tmp.database_type == 'mariadb':
                args_tmp.port = 13307
            elif args_tmp.database_type == 'oceanbase':
                args_tmp.port = 12881
                args_tmp.password = ''
            elif args_tmp.database_type == 'postgres':
                args_tmp.port = 15432
                self.DBType = "postgres"
            elif args_tmp.database_type == 'tdsql':
                args_tmp.port = 11345
                args_tmp.host = "43.139.156.4"
                self.DBType = "postgres"
            elif args_tmp.database_type == 'opengauss':
                args_tmp.port = 18888
                self.DBType = "postgres"
                self.time_out_set = "SET lockwait_timeout = 1000; SET update_lockwait_timeout = 1000;"

        self.args = args_tmp    

        self.config = {
            'host': self.args.host,
            'user': self.args.user,
            'database': self.args.database,
            'password': self.args.password,
            'port': self.args.port
        }

        self.date = time.strftime("%m-%d", time.localtime())
        
        self.isolation_level = 'SERIALIZABLE'
        
        if self.args.isolation.upper() == 'SERIALIZABLE':
            self.isolation_level = 'SERIALIZABLE'
        elif self.args.isolation.upper() == 'REPEATABLE_READ':
            self.isolation_level = 'REPEATABLE READ'
        elif self.args.isolation.upper() == 'READ_COMMITTED':
            self.isolation_level = 'READ COMMITTED'
        else:
            assert False, 'isolation_level not support'
        
    def get_session_query(self):
        if self.DBType == 'mysql':
            return 'SELECT CONNECTION_ID();'
        elif self.DBType == 'postgres':
            return 'SELECT pg_backend_pid();'
        else:
            assert False, 'DBType not support'


class Trigger:
    def __init__(self, TableNames, DBType):
        self.TableNames = TableNames if isinstance(TableNames, list) else [TableNames]
        self.DBType = DBType
        
        self.trigger_init = []
        
        for TableName in self.TableNames:
            if DBType == "mysql":
                trigger_update = f"""
                    CREATE TRIGGER tri_update_{TableName}
                    AFTER UPDATE ON {TableName}
                    FOR EACH ROW
                    BEGIN
                        INSERT INTO {TableName}_log (operation_type, session_id, id, val, c0, c1, c2, time)
                        VALUES ('UPDATE', CONNECTION_ID(), NEW.ID, NEW.VAL, OLD.c0, OLD.c1, OLD.c2, SYSDATE(6));
                    END;
                """
                trigger_delete = f"""
                    CREATE TRIGGER tri_delete_{TableName}
                    AFTER DELETE ON {TableName}
                    FOR EACH ROW
                    BEGIN
                        INSERT INTO {TableName}_log (operation_type, session_id, id, val, c0, c1, c2, time)
                        VALUES ('DELETE', CONNECTION_ID(), OLD.ID, -1, OLD.c0, OLD.c1, OLD.c2, SYSDATE(6));
                    END;
                """
                trigger_insert = f"""
                    CREATE TRIGGER tri_insert_{TableName}
                    AFTER INSERT ON {TableName}
                    FOR EACH ROW
                    BEGIN
                        INSERT INTO {TableName}_log (operation_type, session_id, id, val, c0, c1, c2, time)
                        VALUES ('INSERT', CONNECTION_ID(), NEW.ID, NEW.VAL, NEW.c0, NEW.c1, NEW.c2, SYSDATE(6));
                    END;
                """
                self.trigger_init.extend([trigger_update, trigger_delete, trigger_insert])
            elif DBType == "postgres":
                trigger_update = f"""
                    CREATE OR REPLACE FUNCTION log_update_{TableName}() 
                    RETURNS TRIGGER AS $$
                    BEGIN
                        INSERT INTO {TableName}_log (operation_type, session_id, id, val, c0, c1, c2, time)
                        VALUES ('UPDATE', pg_backend_pid(), NEW.ID, NEW.VAL, OLD.c0, OLD.c1, OLD.c2, clock_timestamp());
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql;

                    CREATE TRIGGER tri_update_{TableName}
                    AFTER UPDATE ON {TableName}
                    FOR EACH ROW EXECUTE PROCEDURE log_update_{TableName}();
                """
                trigger_delete = f"""
                    CREATE OR REPLACE FUNCTION log_delete_{TableName}() 
                    RETURNS TRIGGER AS $$
                    BEGIN
                        INSERT INTO {TableName}_log (operation_type, session_id, id, val, c0, c1, c2, time)
                        VALUES ('DELETE', pg_backend_pid(), OLD.ID, -1, OLD.c0, OLD.c1, OLD.c2, clock_timestamp());
                        RETURN OLD;
                    END;
                    $$ LANGUAGE plpgsql;

                    CREATE TRIGGER tri_delete_{TableName}
                    AFTER DELETE ON {TableName}
                    FOR EACH ROW EXECUTE PROCEDURE log_delete_{TableName}();
                """
                trigger_insert = f"""
                    CREATE OR REPLACE FUNCTION log_insert_{TableName}() 
                    RETURNS TRIGGER AS $$
                    BEGIN
                        INSERT INTO {TableName}_log (operation_type, session_id, id, val, c0, c1, c2, time)
                        VALUES ('INSERT', pg_backend_pid(), NEW.ID, NEW.VAL, NEW.c0, NEW.c1, NEW.c2, clock_timestamp());
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql;

                    CREATE TRIGGER tri_insert_{TableName}
                    AFTER INSERT ON {TableName}
                    FOR EACH ROW EXECUTE PROCEDURE log_insert_{TableName}();
                """
                self.trigger_init.extend([trigger_update, trigger_delete, trigger_insert])
            else:
                raise ValueError("Unsupported DBType. Supported types are 'MySQL' and 'PostgreSQL'.")

    def get_trigger_init(self):
        return self.trigger_init
    
    @staticmethod
    def create_trigger_table(create_table_sql, table_name):
        # 删除旧的触发器日志表
        trigger_drop = f"""DROP TABLE IF EXISTS {table_name}_log;"""
        
        # 替换原始表的表名为触发器表名
        table_sql = create_table_sql.replace(table_name, f"{table_name}_log")
        
        # 利用 ALTER 语句为触发器表添加新的字段
        alter_sql = f"""
            ALTER TABLE {table_name}_log
            ADD COLUMN operation_type VARCHAR(6),
            ADD COLUMN session_id VARCHAR(50),
            ADD COLUMN time TIMESTAMP(6);
        """
        
        return [trigger_drop, table_sql, alter_sql]
        
        

class Pattern(Enum):
    # 单变量
    WWW = "W1xW2xW1xC1cC2c"
    WWCW = "W1xW2xC2cW1xC1c"
    WWR = "W1xW2xR1xC1cC2c"
    WWCR = "W1xW2xC2cR1xC1c"
    WRW = "W1xR2xW1xC1cC2c"
    WRCW = "W1xR2xC2cW1xC1c"
    RWW = "R1xW2xW1xC1cC2c"
    RWCW = "R1xW2xC2cW1xC1c"
    RWR = "R1xW2xR1xC1cC2c"
    RWCR = "R1xW2xC2cR1xC1c"
    
    # 双变量
    # ww-
    WWWW1 = "W1xW2xW2yW1yC1cC2c"
    WWWW2 = "W1xW2yW2xW1yC1cC2c"
    WWWW3 = "W1xW2yW1yW2xC1cC2c"
    WWWCW1 = "W1xW2xW2yC2cW1yC1c"
    WWWCW2 = "W1xW2yW2xC2cW1yC1c"
    WWWCW3 = "W1xW2yW1yC1cW2xC2c"
    WWWR1 = "W1xW2xW2yR1yC1cC2c"
    WWWR2 = "W1xW2yW2xR1yC1cC2c"
    WWWR3 = "W1xW2yR1yW2xC1cC2c"
    WWWCR1 = "W1xW2xW2yC2cR1yC1c"
    WWWCR2 = "W1xW2yW2xC2cR1yC1c"
    WWWCR3 = "W1xW2yR1yC1cW2xC2c"
    WWRW1 = "W1xW2xR2yW1yC1cC2c"
    WWRW2 = "W1xR2yW2xW1yC1cC2c"
    WWRW3 = "W1xR2yW1yW2xC1cC2c"
    WWRCW1 = "W1xW2xR2yC2cW1yC1c"
    WWRCW2 = "W1xR2yW2xC2cW1yC1c"
    WWRCW3 = "W1xR2yW1yC1cW2xC2c"
    
    # wr
    WRWW1 = "W1xR2xW2yW1yC1cC2c"
    WRWW2 = "W1xW2yR2xW1yC1cC2c"
    WRWW3 = "W1xW2yW1yR2xC1cC2c"
    WRWCW1 = "W1xR2xW2yC2cW1yC1c"
    WRWCW2 = "W1xW2yR2xC2cW1yC1c"
    WRWCW3 = "W1xW2yW1yC1cR2xC2c"
    WRWR1 = "W1xR2xW2yR1yC1cC2c"
    WRWR2 = "W1xW2yR2xR1yC1cC2c"
    WRWR3 = "W1xW2yR1yR2xC1cC2c"
    WRWCR1 = "W1xR2xW2yC2cR1yC1c"
    WRWCR2 = "W1xW2yR2xC2cR1yC1c"
    WRWCR3 = "W1xW2yR1yC1cR2xC2c"
    WRRW1 = "W1xR2xR2yW1yC1cC2c"
    WRRW2 = "W1xR2yR2xW1yC1cC2c"
    WRRW3 = "W1xR2yW1yR2xC1cC2c"
    WRRCW1 = "W1xR2xR2yC2cW1yC1c"
    WRRCW2 = "W1xR2yR2xC2cW1yC1c"
    WRRCW3 = "W1xR2yW1yC1cR2xC2c"
    
    # rc
    RWWW1 = "R1xW2xW2yW1yC1cC2c"
    RWWW2 = "R1xW2yW2xW1yC1cC2c"
    RWWW3 = "R1xW2yW1yW2xC1cC2c"
    RWWCW1 = "R1xW2xW2yC2cW1yC1c"
    RWWCW2 = "R1xW2yW2xC2cW1yC1c"
    RWWCW3 = "R1xW2yW1yC1cW2xC2c"
    RWWR1 = "R1xW2xW2yR1yC1cC2c"
    RWWR2 = "R1xW2yW2xR1yC1cC2c"
    RWWR3 = "R1xW2yR1yW2xC1cC2c"
    RWWCR1 = "R1xW2xW2yC2cR1yC1c"
    RWWCR2 = "R1xW2yW2xC2cR1yC1c"
    RWWCR3 = "R1xW2yR1yC1cW2xC2c"
    RWRW1 = "R1xW2xR2yW1yC1cC2c"
    RWRW2 = "R1xR2yW2xW1yC1cC2c"
    RWRW3 = "R1xR2yW1yW2xC1cC2c"
    RWRCW1 = "R1xW2xR2yC2cW1yC1c"
    RWRCW2 = "R1xR2yW2xC2cW1yC1c"
    RWRCW3 = "R1xR2yW1yC1cW2xC2c"
    
    # jim gray exceptions
    DW = "W1xW2xC1cC2c"
    DR = "W1xR2xC1cC2c"
    DRC = "R2xW1xR2xC1cC2c"
    PR = "R1XW2XC2cR1XC1c"
    
    def __init__(self, pattern_description):
        self._pattern_description = pattern_description

    def get_pattern_description(self):
        return self._pattern_description

    def length(self):
        return len(self._pattern_description)

    def char_at(self, index):
        return self._pattern_description[index]

    def __str__(self):
        return self._pattern_description

    # 获取 RC 类型的模式
    @staticmethod
    def get_RC():
        return [
            Pattern.DW, Pattern.DR, Pattern.DRC, Pattern.WWW, Pattern.WWCW, Pattern.WWR, Pattern.WWCR, Pattern.RWW,
            Pattern.WRW, Pattern.WRCW, Pattern.RWR, Pattern.WWWW1, Pattern.WWWW2, Pattern.WWWW3, Pattern.WWWCW1,
            Pattern.WWWCW2, Pattern.WWWCW3, Pattern.WWWR1, Pattern.WWWR2, Pattern.WWWR3, Pattern.WWWCR1, Pattern.WWWCR2,
            Pattern.WWWCR3, Pattern.WWRW1, Pattern.WWRW2, Pattern.WWRW3, Pattern.WWRCW1, Pattern.WWRCW2, Pattern.WRWW1,
            Pattern.WRWW2, Pattern.WRWW3, Pattern.WRWCW1, Pattern.WRWCW2, Pattern.WRWCW3, Pattern.WRWR1, Pattern.WRWR2,
            Pattern.WRWR3, Pattern.WRWCR1, Pattern.WRWCR2, Pattern.WRWCR3, Pattern.WRRW1, Pattern.WRRW2, Pattern.WRRW3,
            Pattern.WRRCW1, Pattern.WRRCW2, Pattern.RWWW1, Pattern.RWWR2, Pattern.RWWR3, Pattern.RWWCW3, Pattern.RWWR1,
            Pattern.RWWR2, Pattern.RWWR3, Pattern.RWWCR3
        ]

    # 获取 RR 类型的模式
    @staticmethod
    def get_RR():
        RC = Pattern.get_RC()
        return RC + [
            Pattern.RWCW, Pattern.RWCR, Pattern.WWRCW3, Pattern.WRRCW3, Pattern.RWWCW1, Pattern.RWWCW2, Pattern.RWWCR1,
            Pattern.RWWCR2
        ]

    # 获取 SR 类型的模式
    @staticmethod
    def get_SR():
        RR = Pattern.get_RR()
        return RR + [
            Pattern.RWRW1, Pattern.RWRW2, Pattern.RWRW3, Pattern.RWRCW1, Pattern.RWRCW2, Pattern.RWRCW3
        ]
        
    @staticmethod
    def find(pattern):
        for p in Pattern:
            if p.get_pattern_description() == pattern:
                return p
        return None