package billy.snxi.myimageselector.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Administrator on 2018-05-26.
 */
public class MyImageFileFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String filename) {
        filename = filename.toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")) {
            return true;
        }
        return false;
    }
}
