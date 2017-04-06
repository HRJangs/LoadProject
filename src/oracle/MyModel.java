package oracle;

import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

/*
 * JTable�� ���÷� ������ ���� ��Ʈ�ѷ�
 * */
public class MyModel extends AbstractTableModel{
	Vector columnName;  //�÷��� ������ ���� ����
	Vector<Vector> list; //���ڵ带 ���� ������ ����
	
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
	//row,col�� ��ġ�� ���� ���������ϰ� �Ѵ�.
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
	
	//������ ���� �ݿ��ϴ� �޼���
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
