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
	Thread thread; //엑셀등록시 사용될 쓰레드
	//왜? 데이터량이 너무 많을경우, 네트워크 상태가 좋지 않을경우 insert가 while문을 못따라간다
	//따라서 안정성을 위해 시간지연을 일으킨다 그리고 insert 할거다
	MyModel model;
	//윈도우 창이 열리면 이미 접속을 확보해놓자
	Connection con;
	DBManager manager=DBManager.getInstance();
	StringBuffer insertSql;  //엑셀파일에 의해 생성된 쿼리문을 쓰레드가 사용할 수 있는 상태로 저장해놓자.
	String seq;
	public LoadMain() {
		p_north = new JPanel();
		bt_open =  new JButton("CSV 파일열기");
		bt_load =  new JButton("로드 하기");
		bt_excel =  new JButton("엑셀 로드");
		bt_del =  new JButton("삭제 하기");
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
		
		
		//테이블모델과 리스너와의 연결
		setVisible(true);
		setSize(800,600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//데이터베이스 자원 해제
				manager.disconnect();
				//프로세스 종료
				System.exit(0);
			}
		});
		init();
	}

	//파일 탐색기 띄우기
	public void open(){
		int result =  chooser.showOpenDialog(this);
		
		//열기를 누르면...목적파일에 스트림을 생성하자
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
				JOptionPane.showMessageDialog(this, "CSV파일만 선택하세요");
			}
		}
	}
	//CSV-->Oracle로 데이터 이전(migration)하기
	public void load(){
		//버퍼스트림을 이용하여 csv의 데이터를 1줄씩 읽어들여
		//insert 시키자
		//레코드가 없을때까지
		//while문으로 돌리면 너무 빠르므로 네트워크가 감당할수가 없다
		//그래서 일부러 지연시키면서 보내자
		String data;
		StringBuffer sb= new StringBuffer();
		PreparedStatement pstmt =null;
		try {
			//seq 줄을 제외하고 insert 하겠다
			while(true){
				data = buffr.readLine();
				if(data==null)break;
				String[] value= data.split(",");
				
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq,name,addr,regdate,status,dimension,type)");
					sb.append("values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					
					pstmt =con.prepareStatement(sb.toString());
					
					int result = pstmt.executeUpdate();//쿼리수행
					//기존에 누적된 Stringbuffer의 데이터를 모두 지우기
					sb.delete(0, sb.length());	
				}
			}
			JOptionPane.showMessageDialog(this, "마이그레이션 완료");
			
			//Jtable 나오는 처리
			getList();
			model = new MyModel(list, columnName);
			table.setModel(model);
			table.getModel().addTableModelListener(this);
			table.updateUI();
			
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "파일을 선택해주세요");
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
	//엑셀 파일 읽어서 db에 마이그레이션 하기
	//javaSE에는 엑셀제어 라이브러리가 없다.
	//open source 공개소프트웨어
	//copyright <--> copyleft(아파치 단체)
	//POI 라이브러리
	/*
	 * HSSFWorkbook : 엑셀파일
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
				sheet = book.getSheet("동물병원");
				int total = sheet.getLastRowNum();
				DataFormatter df = new DataFormatter();
			
				/*
				 * 첫번째 row는 데이터가 아닌 컬럼 정보이므로
				 * 이 정보들을 추출하여 insert into table안쪽에다가 넣자
				 * */
				HSSFRow firstRow = sheet.getRow(sheet.getFirstRowNum());
				//마지막 셀번호;
				for(int i=0;i<firstRow.getLastCellNum();i++){
					if(i==firstRow.getLastCellNum()-1){
						cols.append(firstRow.getCell(i).getStringCellValue());
					}else{
						cols.append(firstRow.getCell(i).getStringCellValue()+",");
					}
				}
				System.out.println(total);
				//row를 얻었으니, 컬럼을 분석하자
				for(int i=1;i<=total;i++){
					HSSFRow row = sheet.getRow(i);
					int col=row.getLastCellNum();
					String[] val = new String[col];
					data.delete(0, data.length());
					for(int j=0;j<col;j++){
						HSSFCell cell =  row.getCell(j);
						//자료형에 국한되지 않고 모두 string처리를 할수가 있다.
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
					//pstmt.executeUpdate();//쿼리수행
					
				}
				//모든게 끝났으니 편안하게 쓰레드에게 일시키자
				//runnable 인터페이스를 인수로 넣으면
				//아래thread의 run을 수행하는 것이 아니라 runnable인터페이스를 구현한자의
				//run을 수행한다. 따라서 우리꺼다
				thread = new Thread(this);
				thread.start();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	//모든 레코드 가져오기
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
			list = new Vector<Vector>(); //이차원벡터
			while(rs.next()){
				Vector vec= new Vector();  //레코드 한건 담을거야
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
	//선택한 메서드 삭제
	public void delete(){
		PreparedStatement pstmt=null;
		int result =JOptionPane.showConfirmDialog(LoadMain.this, seq+" 삭제할래요?");
		if(result==JOptionPane.OK_OPTION){
			String sql = "delete from hospital where seq ="+seq;
			try {
				pstmt= con.prepareStatement(sql);
				int q_result = pstmt.executeUpdate();
				if(q_result==1){
					JOptionPane.showMessageDialog(LoadMain.this, "삭제완료");
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
		//connection 얻어다 놓기
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
		//기존에 사용했던 StringBuffer 비우기
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
	//테이블모델의 데이터값에 변경이 발생하면 그 찰나를 감지하는 리스너! 
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
