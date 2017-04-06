package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

import util.file.FileUtil;

public class LoadMain extends JFrame implements ActionListener,TableModelListener,Runnable{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open,bt_load,bt_del,bt_excel;
	JTable table;
	JScrollPane scroll;
	JFileChooser chooser;
	FileReader reader=null;
	BufferedReader buffr = null;
	Vector<Vector> list;
	Vector columnName;
	Thread thread; //������Ͻ� ���� ������
	//��? �����ͷ��� �ʹ� �������, ��Ʈ��ũ ���°� ���� ������� insert�� while���� �����󰣴�
	//���� �������� ���� �ð������� ����Ų�� �׸��� insert �ҰŴ�
	MyModel model;
	//������ â�� ������ �̹� ������ Ȯ���س���
	Connection con;
	DBManager manager=DBManager.getInstance();
	StringBuffer insertSql;  //�������Ͽ� ���� ������ �������� �����尡 ����� �� �ִ� ���·� �����س���.
	String seq;
	public LoadMain() {
		p_north = new JPanel();
		bt_open =  new JButton("CSV ���Ͽ���");
		bt_load =  new JButton("�ε� �ϱ�");
		bt_excel =  new JButton("���� �ε�");
		bt_del =  new JButton("���� �ϱ�");
		t_path = new JTextField(30);
		table = new JTable();
		scroll= new JScrollPane(table);
		chooser = new JFileChooser("C:/animal");
		insertSql =  new StringBuffer();
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_excel.addActionListener(this);
		bt_del.addActionListener(this);
		table.addMouseListener(new MouseAdapter() {
			
			public void mouseClicked(MouseEvent e) {
			
				JTable t= (JTable) e.getSource();
				seq =(String)t.getValueAt(t.getSelectedRow(), 0);
			}
		});
		
		add(p_north,BorderLayout.NORTH);
		add(scroll);
		
		
		//���̺�𵨰� �����ʿ��� ����
		setVisible(true);
		setSize(800,600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//�����ͺ��̽� �ڿ� ����
				manager.disconnect();
				//���μ��� ����
				System.exit(0);
			}
		});
		init();
	}

	//���� Ž���� ����
	public void open(){
		int result =  chooser.showOpenDialog(this);
		
		//���⸦ ������...�������Ͽ� ��Ʈ���� ��������
		if(result==JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
		
			String ext=FileUtil.getExt(file.getName());

			if(ext.equals("csv")){
				t_path.setText(file.getAbsolutePath());
				try {
					reader=new FileReader(file);
					buffr = new BufferedReader(reader);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} 
			}else{
				JOptionPane.showMessageDialog(this, "CSV���ϸ� �����ϼ���");
			}
		}
	}
	//CSV-->Oracle�� ������ ����(migration)�ϱ�
	public void load(){
		//���۽�Ʈ���� �̿��Ͽ� csv�� �����͸� 1�پ� �о�鿩
		//insert ��Ű��
		//���ڵ尡 ����������
		//while������ ������ �ʹ� �����Ƿ� ��Ʈ��ũ�� �����Ҽ��� ����
		//�׷��� �Ϻη� ������Ű�鼭 ������
		String data;
		StringBuffer sb= new StringBuffer();
		PreparedStatement pstmt =null;
		try {
			//seq ���� �����ϰ� insert �ϰڴ�
			while(true){
				data = buffr.readLine();
				if(data==null)break;
				String[] value= data.split(",");
				
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq,name,addr,regdate,status,dimension,type)");
					sb.append("values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					
					pstmt =con.prepareStatement(sb.toString());
					
					int result = pstmt.executeUpdate();//��������
					//������ ������ Stringbuffer�� �����͸� ��� �����
					sb.delete(0, sb.length());	
				}
			}
			JOptionPane.showMessageDialog(this, "���̱׷��̼� �Ϸ�");
			
			//Jtable ������ ó��
			getList();
			model = new MyModel(list, columnName);
			table.setModel(model);
			table.getModel().addTableModelListener(this);
			table.updateUI();
			
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "������ �������ּ���");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	//���� ���� �о db�� ���̱׷��̼� �ϱ�
	//javaSE���� �������� ���̺귯���� ����.
	//open source ��������Ʈ����
	//copyright <--> copyleft(����ġ ��ü)
	//POI ���̺귯��
	/*
	 * HSSFWorkbook : ��������
	 * HSSFSheet : sheet 
	 * HSSFRow :  row
	 * HSSFCell :  cell
	 * */
	public void loadExcel(){
		int result = chooser.showOpenDialog(this);
		
		if(result==JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			FileInputStream fis= null;
			
			StringBuffer data =  new StringBuffer();
			StringBuffer cols =  new StringBuffer();			
			try {
				
				fis = new FileInputStream(file);
				HSSFWorkbook book;
				book  = new HSSFWorkbook(fis);
				HSSFSheet sheet = null;
				sheet = book.getSheet("��������");
				int total = sheet.getLastRowNum();
				DataFormatter df = new DataFormatter();
			
				/*
				 * ù��° row�� �����Ͱ� �ƴ� �÷� �����̹Ƿ�
				 * �� �������� �����Ͽ� insert into table���ʿ��ٰ� ����
				 * */
				HSSFRow firstRow = sheet.getRow(sheet.getFirstRowNum());
				//������ ����ȣ;
				for(int i=0;i<firstRow.getLastCellNum();i++){
					if(i==firstRow.getLastCellNum()-1){
						cols.append(firstRow.getCell(i).getStringCellValue());
					}else{
						cols.append(firstRow.getCell(i).getStringCellValue()+",");
					}
				}
				System.out.println(total);
				//row�� �������, �÷��� �м�����
				for(int i=1;i<=total;i++){
					HSSFRow row = sheet.getRow(i);
					int col=row.getLastCellNum();
					String[] val = new String[col];
					data.delete(0, data.length());
					for(int j=0;j<col;j++){
						HSSFCell cell =  row.getCell(j);
						//�ڷ����� ���ѵ��� �ʰ� ��� stringó���� �Ҽ��� �ִ�.
						String str = df.formatCellValue(cell);
						if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							str="'"+str+"'";
						}
						if(j<col-1){
							data.append(str+",");
						}else{
							data.append(str);
						}
					}
					insertSql.append("insert into hospital("+cols.toString()+")values("+data+");");
					//pstmt =con.prepareStatement(insertSql.toString());
					//pstmt.executeUpdate();//��������
					
				}
				//���� �������� ����ϰ� �����忡�� �Ͻ�Ű��
				//runnable �������̽��� �μ��� ������
				//�Ʒ�thread�� run�� �����ϴ� ���� �ƴ϶� runnable�������̽��� ����������
				//run�� �����Ѵ�. ���� �츮����
				thread = new Thread(this);
				thread.start();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	//��� ���ڵ� ��������
	public void getList(){
		String sql = "select * from hospital order by seq asc";
		PreparedStatement pstmt =null;
		ResultSet rs = null;
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			ResultSetMetaData meta =   rs.getMetaData();
			int count = meta.getColumnCount();
			columnName = new Vector();
			for(int i =0;i<count;i++){
				columnName.add(meta.getColumnLabel(i+1));
			}
			list = new Vector<Vector>(); //����������
			while(rs.next()){
				Vector vec= new Vector();  //���ڵ� �Ѱ� �����ž�
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				list.add(vec);
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	//������ �޼��� ����
	public void delete(){
		PreparedStatement pstmt=null;
		int result =JOptionPane.showConfirmDialog(LoadMain.this, seq+" �����ҷ���?");
		if(result==JOptionPane.OK_OPTION){
			String sql = "delete from hospital where seq ="+seq;
			try {
				pstmt= con.prepareStatement(sql);
				int q_result = pstmt.executeUpdate();
				if(q_result==1){
					JOptionPane.showMessageDialog(LoadMain.this, "�����Ϸ�");
				}
				
			} catch (SQLException e) {
				e.printStackTrace();
			}finally{
				if(pstmt!=null){
					try {
						pstmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				
			}
		}
	}
	public void actionPerformed(ActionEvent e) {
		Object obj= e.getSource();
		if(obj==bt_open){
			open();
		}else if(obj==bt_load){
			load();
		}else if(obj==bt_del){
			delete();
		}else if(obj==bt_excel){
			loadExcel();
		}
	}
	public void init(){
		//connection ���� ����
		con  =manager.getConnection();
	}

	public void run() {
		PreparedStatement pstmt =null;
		String[] str  = insertSql.toString().split(";");
		System.out.println(str.length);
		for(int i =0;i<str.length;i++){
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				pstmt=con.prepareStatement(str[i]);
				pstmt.executeUpdate();
				System.out.println(str[i]);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		//������ ����ߴ� StringBuffer ����
		insertSql.delete(0, insertSql.length());
		if(pstmt!=null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		table.updateUI();
	}
	//���̺���� �����Ͱ��� ������ �߻��ϸ� �� ������ �����ϴ� ������! 
	public void tableChanged(TableModelEvent e) {
		PreparedStatement pstmt=null;
		int row = e.getFirstRow();
		int col =e.getColumn();
		//System.out.println(row+","+col);
		String data =(String) model.getValueAt(row, col);
		String seq = (String) model.getValueAt(row, 0);
		String column=(String) model.getColumnName(col);
		String sql ="update  hospital set "+column+" ='"+data+"' where seq="+seq;
		System.out.println(sql);
		
		try {
			pstmt = con.prepareStatement(sql);
			int result = pstmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	public static void main(String[] args) {
		new LoadMain();
	}
}
