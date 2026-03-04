"""
电表数据插入器模块
提供插入电压和电流数据到MySQL的功能
"""

import pymysql
from datetime import datetime
from typing import Optional, Tuple, Dict, Any
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class ElectricityDataInserter:
    def __init__(self, 
                 host: str = "localhost",
                 port: int = 3306,
                 database: str = "power_manage",
                 user: str = "root",
                 password: str = "123456",
                 charset: str = "utf8"):
        """
        初始化数据库连接参数
        
        Args:
            host: MySQL主机地址
            port: MySQL端口
            database: 数据库名
            user: 用户名
            password: 密码
            charset: 字符集
        """
        self.db_config = {
            'host': host,
            'port': port,
            'database': database,
            'user': user,
            'password': password,
            'charset': charset,
            'cursorclass': pymysql.cursors.DictCursor
        }
        logger.info(f"初始化数据库连接器: {host}:{port}/{database}")
        
    def test_connection(self) -> bool:
        """
        测试数据库连接
        
        Returns:
            bool: 连接是否成功
        """
        try:
            connection = pymysql.connect(**self.db_config)
            connection.close()
            logger.info("数据库连接测试成功")
            return True
        except Exception as e:
            logger.error(f"数据库连接测试失败: {e}")
            return False
    
    def _calculate_power_and_consumption(self, voltage: float, current: float, 
                                        interval_minutes: int = 15) -> Tuple[float, float]:
        """
        根据电压和电流计算功率和电量
        
        Args:
            voltage: 电压（V）
            current: 电流（A）
            interval_minutes: 时间间隔（分钟）
            
        Returns:
            Tuple[功率(kW), 电量(kWh)]
        """
        # 计算有功功率：P = U * I（假设功率因数为1，单位：W）
        power_watt = voltage * current  # 功率（W）
        power_kw = power_watt / 1000.0  # 转换为kW
        
        # 计算电量：E = P * t（t为小时）
        time_hours = interval_minutes / 60.0
        electricity_consumption = power_kw * time_hours  # 电量（kWh）
        
        return round(power_kw, 4), round(electricity_consumption, 4)
    
    def insert_electricity_data(self, 
                              user_id: int,
                              voltage: float,
                              current: float,
                              data_source: str = "sensor",
                              data_type: str = "实时",
                              interval_minutes: int = 15,
                              record_time: Optional[datetime] = None) -> Dict[str, Any]:
        """
        插入电表数据到数据库
        
        Args:
            user_id: 用户ID
            voltage: 电压（V）
            current: 电流（A）
            data_source: 数据来源，默认为'sensor'
            data_type: 数据类型，'历史'或'实时'，默认为'实时'
            interval_minutes: 数据间隔（分钟），默认为15
            record_time: 记录时间，默认为当前时间
            
        Returns:
            包含操作结果和插入ID的字典
        """
        
        # 验证输入参数
        if not self._validate_input(voltage, current):
            return {"success": False, "error": "输入参数无效"}
        
        # 计算功率和电量
        try:
            power_kw, electricity_consumption = self._calculate_power_and_consumption(
                voltage, current, interval_minutes
            )
        except Exception as e:
            logger.error(f"计算功率和电量时出错: {e}")
            return {"success": False, "error": f"计算错误: {str(e)}"}
        
        # 设置记录时间
        if record_time is None:
            record_time = datetime.now()
        
        # 准备SQL语句
        sql = """
        INSERT INTO t_electricity_data (
            user_id, record_time, electricity_consumption, power,
            data_source, data_type, interval_minutes, is_valid
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """
        
        # 准备参数
        params = (
            user_id,
            record_time,
            electricity_consumption,
            power_kw,
            data_source,
            data_type,
            interval_minutes,
            True  # is_valid默认为True
        )
        
        # 连接数据库并执行插入
        connection = None
        try:
            connection = pymysql.connect(**self.db_config)
            with connection.cursor() as cursor:
                cursor.execute(sql, params)
                inserted_id = cursor.lastrowid
                connection.commit()
                
                logger.info(f"数据插入成功，ID: {inserted_id}")
                logger.info(f"电压: {voltage}V, 电流: {current}A")
                logger.info(f"计算得到的功率: {power_kw:.3f}kW, 电量: {electricity_consumption:.3f}kWh")
                
                return {
                    "success": True,
                    "inserted_id": inserted_id,
                    "voltage": voltage,
                    "current": current,
                    "power": power_kw,
                    "electricity_consumption": electricity_consumption,
                    "record_time": record_time
                }
                
        except pymysql.MySQLError as e:
            logger.error(f"数据库操作出错: {e}")
            if connection:
                connection.rollback()
            return {"success": False, "error": f"数据库错误: {str(e)}"}
            
        except Exception as e:
            logger.error(f"未知错误: {e}")
            if connection:
                connection.rollback()
            return {"success": False, "error": f"未知错误: {str(e)}"}
            
        finally:
            if connection:
                connection.close()
    
    def _validate_input(self, voltage: float, current: float) -> bool:
        """
        验证输入参数是否合理
        
        Args:
            voltage: 电压
            current: 电流
            
        Returns:
            bool: 是否有效
        """
        if voltage <= 0:
            logger.error("电压必须大于0")
            return False
        if current < 0:
            logger.error("电流不能为负数")
            return False
        if voltage > 1000:  # 假设电压不超过1000V
            logger.warning("电压值异常，请检查")
        if current > 1000:  # 假设电流不超过1000A
            logger.warning("电流值异常，请检查")
        return True
    
    