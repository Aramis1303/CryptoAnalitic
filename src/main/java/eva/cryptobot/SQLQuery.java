/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author username
 */
public class SQLQuery {
    // JDBC URL, username and password of MySQL server
    private static String url;
    
    private static String user;
    private static String password;
    
    public SQLQuery(String db) {
        
        url = "jdbc:mariadb://79.120.44.138:33306/" + db;
        //url = "jdbc:mariadb://192.168.0.9:3306/" + db;
        user = "root";
        password = "[htydfv1303";
        
        try {
            // MariaDB
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println(SQLQuery.class.getName() + " -> " + ex);
        }
    }
    
    // Записать результат прогнозирования
    public synchronized boolean writePrediction (String predictionName, String tbl, String period, double prediction, double probability) {
        
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
            createTable("`" + tbl + "_" + predictionName + "_" + period + "`", "`time` BIGINT NOT NULL UNIQUE, `prediction` DOUBLE, `probability` DOUBLE, `id_key` INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id_key`)");
            stmt.execute("INSERT INTO `" + tbl + "_" + predictionName + "_" + period + "` (`time`, `prediction`, `probability`) VALUES (" + new Date().getTime() + ", " + prediction + ", " + probability +");");
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " writeJapaneseCandles -> " + ex);
            return false;
        }
        return true;
    }
    
    // Взять время последней свечи
    public synchronized long getTimeOfLastPrediction(String predictionName, String tbl, String period) {
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()){
            
            ResultSet rs = stmt.executeQuery("SELECT `time` FROM `" + tbl + "_" + predictionName + "_" + period + "` ORDER BY `time` DESC LIMIT 1;");
            
            if(rs.next()) {
                stmt.close();
                return rs.getLong("time");
            }
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " getTimeOfLastPrediction -> " + ex);
            return 0;
        }
        return 0;
    }
    
    // Записать заказ
    public synchronized boolean writeOrder (String market, String order, String uuid) {
        
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
        
            createTable("`orders`", "`time` BIGINT NOT NULL UNIQUE, `market` TEXT, `order` TEXT, `uuid` TEXT, `result` TEXT, `id_key` INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id_key`)");
            
            stmt.execute("INSERT INTO `orders` (`time`, `market`, `order`, `uuid`, `result`) VALUES (" 
                    + new Date().getTime() + ", "
                    + "\"" + market + "\", "
                    + "\"" + order + "\", "
                    + "\"" + uuid + "\", "
                    + "\"opened\");");
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " writeOrder -> " + ex);
            return false;
        }
        return true;
    }
    
    // Записать заказ
    public synchronized boolean updateOrder (String uuid, String status) {
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
            stmt.execute("UPDATE `orders` SET result = \"" + status + "\" WHERE uuid = \"" + uuid + "\";");
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " updateOrder -> " + ex);
            return false;
        }
        return true;
    }
    
    // Получить открытые заказы
    public synchronized Map <String, String> readOpenedOrder(){
        Map <String, String> orders = new HashMap <>();
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT `market`, `uuid` FROM `orders` WHERE `result` = \"opened\";");
            
            while (rs.next()) {
                orders.put(rs.getString("market"), rs.getString("uuid"));
            }
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " readOpenedOrder -> " + ex);
            return orders;
        }
        return orders;
    }
    
    // Взять время последней свечи
    public synchronized long getTimeOfLastJapaneseCandle(String tbl, String period) {
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()){
            
            ResultSet rs = stmt.executeQuery("SELECT `time` FROM `" + tbl + "_candles_" + period + "` ORDER BY `time` DESC LIMIT 1;");
            
            if(rs.next()) {
                stmt.close();
                return rs.getLong("time");
            }
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " getTimeOfLastJapaneseCandle -> " + ex);
        }
        return 0;
    }
    
    // Получить Candles !!! Внимание Коллекция вовращается в обратной порядке
    public synchronized List <Candle> readCandles(String tbl, String period, int count) {
        
        List <Candle> candles = new ArrayList <>();
        
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tbl + "_candles_" + period +  "` ORDER BY `time` DESC LIMIT " + count + ";");
            
            while (rs.next()) {
                Candle c = new Candle(
                    rs.getLong("time"),
                    rs.getDouble("hight"),
                    rs.getDouble("low"),
                    rs.getDouble("in"),
                    rs.getDouble("out"),
                    rs.getDouble("volume")
                );
                candles.add(c);
            }
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " readCandles -> " + ex);
            return null;
        }
        return candles;
    }
    
    //
    public synchronized Map <String, Status> readMarkets(){
        Map <String, Status> markets = new HashMap <>();
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT `name`, `status` FROM `markets` WHERE `status` != \"" + Status.DELETED + "\";");
            while (rs.next()) {
                markets.put(rs.getString("name"), Status.valueOf(rs.getString("status")));
            }
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " readMarkets -> " + ex);
            return markets;
        }
        return markets;
    }
    
    // Создаем таблицу если она не существует
    public void createTable (String data_table, String param){
        try (Statement stmt = DriverManager.getConnection(url + "?useSSL=false", user, password).createStatement()) {
            // executing SELECT query
            stmt.execute("CREATE TABLE IF NOT EXISTS " + data_table + " (" + param +  ");");
        } catch (SQLException ex) {
            System.out.println(SQLQuery.class.getName() + " createTable -> " + ex);
            return;
        }
    }
    
    
}
