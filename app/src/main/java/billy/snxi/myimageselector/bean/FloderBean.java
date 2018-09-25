package billy.snxi.myimageselector.bean;

/**
 * Created by Administrator on 2018-05-26.
 */
public class FloderBean {
    private String path;
    private String firstImageName;
    private int imageCount;

    public FloderBean(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFirstImageName() {
        return firstImageName;
    }

    public void setFirstImageName(String firstImageName) {
        this.firstImageName = firstImageName;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

}
