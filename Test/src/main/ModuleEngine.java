package main;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import ru.umeta.harvesting.base.IHarvester;
import ru.umeta.harvesting.base.model.Query;

public class ModuleEngine {

	private final static Map<String, URLClassLoader> hashMap = new HashMap<>();

	public static int executeClassMethod(String path, String name, Query query) {
        try {
            if (!hashMap.containsKey(path)) {
                File file = new File(path);
                URL jarUrl = new URL("jar", "", "file:" + file.getAbsolutePath() + "!/");
                hashMap.put(path,
                        new URLClassLoader(new URL[]{jarUrl}, ModuleEngine.class.getClassLoader()));
            }
            Class<?> harvesterClass = Class.forName(name, true, hashMap.get(path));
            IHarvester harvesterInstance = (IHarvester) harvesterClass.newInstance();
            return harvesterInstance.harvest(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
	/*EndURL - ����� ������� � ������� url:port/database, ��������, opac.ruslan.ru:210/lounb
	 *StartURL - ����� ��� ���������� ���������� ������ (�� ��������� � ����� results ����� � jar ������)
	 *Reg - ���������� ������� � ����� ����������� ����� (�� ��������� - 1000)
	 *Active - ���� ����� "save", �� ��������� �����, ����� �� ��������� (��� ��������� ������ ���������� � ���, ������� ������ ��������)
	 *Struct_loc - ���� ��� ������ ������, ��������, rus ��� eng (�� ��������� ��� �����)
	 *Time - ���������, ���� ��������, ����� CharsetDetector ����� ������������ */
    public static void main(String[] args){
		Query q = new Query("","","opac.ruslan.ru:210/lounb","C:/Users/cuda/Desktop/4/results","","UTF-8","1000","","ru","","save");
		System.out.println(executeClassMethod("C:/Users/cuda/Desktop/3/ZHarvester.jar", "main.ZHarvester", q));	
    }
}