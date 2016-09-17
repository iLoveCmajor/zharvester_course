package ru.umeta.zharvester;

import org.yaz4j.Connection;
import org.yaz4j.PrefixQuery;
import org.yaz4j.Record;
import org.yaz4j.ResultSet;
import org.yaz4j.exception.ZoomException;
import ru.umeta.harvesting.base.IHarvester;
import ru.umeta.harvesting.base.model.Query;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

public class ZHarvester implements IHarvester {

    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final String JAVA_LIBRARY_PATTERN = System.getProperty(JAVA_LIBRARY_PATH) + ";{0}/libs";
    private static final Query DEFAULT_QUERY =
            new Query("","","sru.gbv.de:80/gvk","","","UTF-8","1000","","eng","","save");

    @Override
    public int harvest(Query query) throws Exception {
        Date startDate = new Date();

        String dir = ZHarvester.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //System.out.println(dir);
        if (dir.substring(0, 4).equals("file"))
            dir = dir.substring(dir.indexOf(":") + 1, dir.lastIndexOf("\\"));
        else
            dir = dir.substring(1, dir.lastIndexOf("/"));

        if (dir.endsWith("/bin")) {
            dir = dir.substring(0, dir.lastIndexOf("/"));
        }

        loadLibraries(dir);

        String encoding = null;
        String url = query.getEndURL();
        String queryStr = "";
        String resultsDir = dir + "/results";
        if (!query.getStartURL().equals(""))
            resultsDir = query.getStartURL();
        new File(resultsDir).mkdirs();
        int num = 1000;
        if (!query.getReg().equals(""))
            num = Integer.parseInt(query.getReg()); //number of records in the collection
        if (!query.getTime().equals(""))
            encoding = query.getTime();
        int counter = 0;
        int res = 0;
        int numberOfRecords = 0;
        boolean noResult = true;
        try (Connection con = new Connection(url, 0)) {
            Source stylesheet = new StreamSource(new File(dir + "/libs/MARC21slim2MODS3-5.xsl"));
            con.setSyntax("marc21");
            try {
                con.connect();
                ResultSet resultSet = null;
                if (query.getStruct_loc().length() != 3)
                    counter++;
                do {
                    try {
                        switch (counter) {
                            case 0:
                                queryStr = "@attr 1=54 '" + query.getStruct_loc() + "'";
                                resultSet = con.search(new PrefixQuery(queryStr));
                                break;
                            case 1:
                                queryStr = "@attr 2=5 @attr 1=4 '0'";
                                resultSet = con.search(new PrefixQuery(queryStr));
                                break;
                            case 2:
                                queryStr = "couldn't search";
                                resultSet = con.search(new PrefixQuery("@or @attr 5=1 @attr 1=31 '2' @attr 5=1 @attr 1=31 '1'"));
                                queryStr = "@or @attr 5=1 @attr 1=31 '2' @attr 5=1 @attr 1=31 '1'";
                        }
                        noResult = false;
                    } catch (ZoomException e) {
                        if (counter == 2)
                            break;
                        counter++;
                    }
                } while (noResult);
                if (query.getActive().equals("save")) {
                    if (resultSet != null && (numberOfRecords = (int) resultSet.getHitCount()) > 0 && resultSet.getRecord(0) != null && resultSet.getRecord(0).getSyntax() != null && (resultSet.getRecord(0).getSyntax().equalsIgnoreCase("usmarc")
                            || resultSet.getRecord(0).getSyntax().equalsIgnoreCase("marc21"))) {
                        int i = 0;
                        while (((encoding == null && i < resultSet.getHitCount()) || (i < resultSet.getHitCount() && !encoding.equals("ISO-8859-7") && !encoding.equals("ISO-8859-1")
                                && !encoding.equals("KOI8-R") && !encoding.equals("WINDOWS-1251") && !encoding.equals("GB18030") && !encoding.equals("WINDOWS-1252") && !encoding.equals("UTF-8"))) && i < 15) {
                            encoding = EncodingUtils.detect(resultSet.getRecord(i).getContent());
                            i++;
                        }
                        if (encoding == null)
                            encoding = "KOI8-R";

                        List<Record> lst = null;
                        for (int k = 0; k < resultSet.getHitCount(); k += num) {
                            if (resultSet.getHitCount() - k >= num)
                                lst = resultSet.getRecords(k, num); //get num records starting from k
                            else {
                                i = (int) resultSet.getHitCount() - k;
                                lst = resultSet.getRecords(k, i);
                            }
                            res += ResultsSaver.save(lst, resultsDir + "/" + (k / num) + ".xml", encoding, stylesheet);
                        }
                    }
                }
                try (PrintWriter writer = new PrintWriter(dir + "/info.txt")) {
                    writer.println("Server: " + url);
                    writer.write("Query: " + queryStr);
                    if (res != 0)
                        writer.write("Number of saved records: " + (numberOfRecords = res));
                    else
                        writer.write("Number of records (not saved): " + (numberOfRecords = (int) ((resultSet == null) ? 0 : resultSet.getHitCount())));
                    if (query.getActive().equals("save"))
                        writer.write("Used encoding: " + encoding);
                    writer.write("Started: " + startDate);
                    writer.write("Finished: " + (new Date()));
                }

            } catch (ZoomException ze) {
                try (FileWriter err = new FileWriter(dir + "/errors.txt", true)) {
                    err.write((new Date()) + "\n" + ze.getMessage());
                }
                return 1;
            }
        }
        return java.lang.Math.max(4, numberOfRecords);
    }

    private void loadLibraries(String dir) throws NoSuchFieldException, IllegalAccessException {
        System.setProperty(JAVA_LIBRARY_PATH, MessageFormat.format(JAVA_LIBRARY_PATTERN, dir));
        Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);
        System.load(dir + "/libs/msvcr120.dll");
        System.load(dir + "/libs/libxml2.dll");
        System.load(dir + "/libs/libxslt.dll");
        System.load(dir + "/libs/yaz5.dll");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.out.println(new ZHarvester().harvest(new Query("", "", args[0], "", "", "", "", "", "", "", "save")));
        } else {
            System.out.println(new ZHarvester().harvest(DEFAULT_QUERY));
        }    
        
    }
}
