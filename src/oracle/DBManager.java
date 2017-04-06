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
	
	//new ���Ҵ� �����̺�����...�̱����̴�
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
	//���� ��ü ��ȯ
	public Connection getConnection(){
		return con;
	} 
	//��������
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
