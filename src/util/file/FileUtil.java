package util.file;

import java.io.File;

/*파일과 관련된 작업을 도와주는 재사용성이 있는 클래스 생성*/
public class FileUtil {
	/*넘겨받은 경로에서 확장자 구하기*/
	public static String getExt(String path){
		int st_index = path.lastIndexOf(".");
		return path.substring(st_index+1, path.length());
	}
}
