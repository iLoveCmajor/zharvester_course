package ru.umeta.zharvester;

import org.mozilla.universalchardet.UniversalDetector;

/**
 * The utils class for handling encodings.
 *
 */
public class EncodingUtils {

    private EncodingUtils() {
        //Utils class
    }

    public static String detect(byte[] stream) throws java.io.IOException {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(stream, 0, stream.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();
        return encoding;
    }
}