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
	/*EndURL - адрес сервера в формате url:port/database, например, opac.ruslan.ru:210/lounb
	 *StartURL - место для сохранения полученных данных (по умолчанию в папке results рядом с jar файлом)
	 *Reg - количество записей в одном сохраняемом файле (по умолчанию - 1000)
	 *Active - если равен "save", то сохранять файлы, иначе не сохранять (для получения только информации о том, сколько файлов доступно)
	 *Struct_loc - язык для поиска данных, например, rus или eng (по умолчанию все языки)
	 *Time - кодировка, если известна, иначе CharsetDetector будет распозновать */
    public static void main(String[] args){
		Query q = new Query("","","opac.ruslan.ru:210/lounb","C:/Users/cuda/Desktop/4/results","","UTF-8","1000","","ru","","save");
		System.out.println(executeClassMethod("C:/Users/cuda/Desktop/3/ZHarvester.jar", "main.ZHarvester", q));	
    }
}