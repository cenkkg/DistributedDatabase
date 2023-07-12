package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector {
    String url = "jdbc:postgresql://hostname:port/database";
    String username = "your-username";
    String password = "your-password";

    public Connection getDBConnection() {
        try{
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e){
            return null;
        }
    }

    public Statement getStatement(Connection connection) {
        try{
            return connection.createStatement();
        } catch (Exception e){
            return null;
        }
    }
}
