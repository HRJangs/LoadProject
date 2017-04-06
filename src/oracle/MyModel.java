package oracle;

import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

/*
 * JTable이 수시로 정보를 얻어가는 컨트롤러
 * */
public class MyModel extends AbstractTableModel{
	Vector columnName;  //컬럼의 제목을 담을 벡터
	Vector<Vector> list; //레코드를 담을 이차원 벡터
	
	public MyModel(Vector list, Vector columnName) {
		this.list =list;
		this.columnName = columnName;
		
	}
	@Override
	public String getColumnName(int column) {
		return (String) columnName.get(column);
	}
	public int getRowCount() {
		
		return list.size();
	}
	public int getColumnCount() {
	
		return columnName.size();
	}
	//row,col에 위치한 셀을 편집가능하게 한다.
	public boolean isCellEditable(int row, int col) {
		if(col!=0){
			return true;
		}
		return false;
	}
	@Override
	public int findColumn(String columnName) {
		// TODO Auto-generated method stub
		return super.findColumn(columnName);
	}
	
	//각셀의 값을 반영하는 메서드
	public void setValueAt(Object Value, int row, int col) {
		Vector vec =list.get(row);
		vec.set(col, Value);
		this.fireTableCellUpdated(row, col);
	}

	
	public Object getValueAt(int rowIndex, int columnIndex) {
		Vector vec = list.get(rowIndex);
		return vec.elementAt(columnIndex);
	}
}
