package billy.snxi.myimageselector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import billy.snxi.myimageselector.adapter.CourseAdater;
import billy.snxi.myimageselector.bean.CourseBean;
import billy.snxi.myimageselector.utils.HttpUtils;

/**
 * 从网络中进行异步加载图片
 */
public class LoadImageByNetworkActivity extends Activity {
    private static final java.lang.String TAG = "billy";
    private static final java.lang.String COURSE_URL = "http://www.imooc.com/api/teacher?type=4&num=30";
    private Context context;
    private ListView lv_course;
    private CourseAdater mAdater;
    private List<CourseBean> mList;
    private ProgressDialog mProgressDialog;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_load_image);
        context = this;
        initView();
    }

    private void initView() {
        lv_course = (ListView) findViewById(R.id.lv_course);
        mList = new ArrayList<>();
        mAdater = new CourseAdater(context, mList);
    }

    /**
     * 加载课程列表
     *
     * @param view
     */
    public void onLoadCourse(View view) {
        AsyncTask<java.lang.String, Void, Boolean> task = new AsyncTask<java.lang.String, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                mProgressDialog = ProgressDialog.show(context, "网络课程", "正在加载中，请稍候...");
                mProgressDialog.show();
            }

            @Override
            protected Boolean doInBackground(java.lang.String... params) {
                try {
                    String result = HttpUtils.readInputStream(new URL(params[0]).openStream());
                    addDataToList(result);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean isSuccess) {
                mProgressDialog.dismiss();
                if (isSuccess) {
                    lv_course.setAdapter(mAdater);
                } else {
                    Toast.makeText(context, "加载网络课程列表失败！", Toast.LENGTH_SHORT).show();
                }
            }
        };
        task.execute(COURSE_URL);
    }

    /**
     * 根据url来从网络获取数据（json）将其转为bean对象并添加至list中
     *
     * @param result
     * @return
     */
    private void addDataToList(java.lang.String result) {
        try {
            JSONObject jsonData = new JSONObject(result);
            CourseBean CourseBean;
            if (jsonData.optInt("status") == 1) {
                JSONArray jsonCourses = jsonData.optJSONArray("data");
                JSONObject jsonCourse;
                for (int i = 0; i < jsonCourses.length(); i++) {
                    jsonCourse = jsonCourses.optJSONObject(i);
                    CourseBean = new CourseBean(
                            jsonCourse.optInt("id"),
                            jsonCourse.optString("picSmall"),
                            jsonCourse.optString("name"),
                            jsonCourse.optString("description"),
                            jsonCourse.optInt("learner"));
                    mList.add(CourseBean);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
