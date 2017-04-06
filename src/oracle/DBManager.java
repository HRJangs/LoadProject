package oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
	private static DBManager instance;
	private String driver="oracle.jdbc.driver.OracleDriver";
	private String url="jdbc:oracle:thin:@localhost:1521:XE";
	private String user="batman";
	private String password="1234";
	
	
	Connection con;
	
	//new 막았다 프라이빗으로...싱글톤이다
	private DBManager(){
		try {
			Class.forName(driver);
			con= DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static DBManager getInstance(){
		if(instance == null){
			instance= new DBManager();
		}
		return instance;
	}
	//접속 객체 반환
	public Connection getConnection(){
		return con;
	} 
	//접속해제
	public void disconnect(){
		if(con!=null){
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
