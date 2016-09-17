package ru.umeta.zharvester;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcXmlWriter;
import org.yaz4j.Record;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;

public class ResultsSaver {
    public static int save(List<Record> lst, String dir, String encoding, Source stylesheet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(10000000); // < 10 000 000 bytes
        Result result = new StreamResult(out);
        MarcXmlWriter writer = new MarcXmlWriter(result, stylesheet);
        for (Record yaz4jRecord : lst) {
            byte[] content = yaz4jRecord.getContent();
            if (content != null) {
                ByteArrayInputStream in = new ByteArrayInputStream(content, 0, content.length);
                MarcReader marcReader = new MarcStreamReader(in, encoding);
                org.marc4j.marc.Record marcRecord = marcReader.next();
                writer.write(marcRecord);
            } else {
                return -1;
            }
        }
        writer.close();
        byte[] bytes = out.toByteArray();

        try (FileOutputStream fileOutputStream = new FileOutputStream(dir)) {
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
