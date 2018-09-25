package billy.snxi.myimageselector.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2018-05-26.
 */
public class HttpUtils {

    /**
     * 读取网络返回的输入流并返回为string
     *
     * @param is
     * @return
     */
    public static String readInputStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
