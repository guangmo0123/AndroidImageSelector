package billy.snxi.myimageselector.bean;

/**
 * Created by Administrator on 2018-05-26.
 */
public class CourseBean {
    private int id;
    private java.lang.String picURL;
    private java.lang.String name;
    private java.lang.String description;
    private int learner;

    public CourseBean() {
    }

    public CourseBean(int id, java.lang.String picURL, java.lang.String name, java.lang.String description, int learner) {
        this.id = id;
        this.picURL = picURL;
        this.name = name;
        this.description = description;
        this.learner = learner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public java.lang.String getPicURL() {
        return picURL;
    }

    public void setPicURL(java.lang.String picURL) {
        this.picURL = picURL;
    }

    public java.lang.String getName() {
        return name;
    }

    public void setName(java.lang.String name) {
        this.name = name;
    }

    public java.lang.String getDescription() {
        return description;
    }

    public void setDescription(java.lang.String description) {
        this.description = description;
    }

    public int getLearner() {
        return learner;
    }

    public void setLearner(int learner) {
        this.learner = learner;
    }
}
