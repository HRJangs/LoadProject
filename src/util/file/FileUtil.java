package util.file;

import java.io.File;

/*���ϰ� ���õ� �۾��� �����ִ� ���뼺�� �ִ� Ŭ���� ����*/
public class FileUtil {
	/*�Ѱܹ��� ��ο��� Ȯ���� ���ϱ�*/
	public static String getExt(String path){
		int st_index = path.lastIndexOf(".");
		return path.substring(st_index+1, path.length());
	}
}
