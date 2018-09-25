package billy.snxi.myimageselector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import billy.snxi.myimageselector.adapter.PhotoAdater;
import billy.snxi.myimageselector.bean.FloderBean;
import billy.snxi.myimageselector.utils.MyImageFileFilter;
import billy.snxi.myimageselector.utils.PhotoDirPopupWindow;

/**
 * 从网络中进行异步加载图片
 */
public class LoadImageByPhotoActivity extends Activity {
    private static final String TAG = "billy";
    private static final String PHOTO_DIR_PATH = "/mnt/shared/Image";
    private Context context;
    private GridView gv_photo;
    private TextView tv_dir_path, tv_dir_image_count;
    private List<String> mList;
    private List<FloderBean> mParentDirList;
    private PhotoAdater mAdater;
    private RelativeLayout rl_layout_dir;
    private ProgressDialog mProgressDialog;
    private MyImageFileFilter mMyimageFileFilter;
    private String mDirPath;
    private int mDirImageCount;
    private PhotoDirPopupWindow mPhotoDirPopupWindow;
    private PhotoDirPopupWindow.PhotoDirItemClickListener mPhotoDirItemClickListener;
    //保存已被选择的图片路径
    private static Set<String> mSelectImageSet = new HashSet<>();
    //保存已被选择的图片的父目录路径
    private static Set<String> mDirSelectImageSet = new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_load_image);
        context = this;
        initView();
    }

    private void initView() {
        gv_photo = (GridView) findViewById(R.id.gv_photo);
        tv_dir_path = (TextView) findViewById(R.id.tv_dir_path);
        tv_dir_image_count = (TextView) findViewById(R.id.tv_dir_image_count);
        rl_layout_dir = (RelativeLayout) findViewById(R.id.rl_layout_dir);
        mParentDirList = new ArrayList<>();
        mList = new ArrayList<>();
        mMyimageFileFilter = new MyImageFileFilter();
    }

    /**
     * 加载课程列表
     *
     * @param view
     */
    public void onLoadPhoto(View view) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                mProgressDialog = ProgressDialog.show(context, "扫描图片", "正在扫描中，请稍候...");
                mProgressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                //访问根目录文件/夹信息
                File dir = new File(PHOTO_DIR_PATH);
                File[] listFiles = dir.listFiles();
                if (listFiles == null) {
                    Log.d(TAG, dir.getPath() + ",获取根目录文件信息失败");
                    return false;
                }
                //将根据目录及子表目录文件夹处理保存起来
                initParentDirDatas(listFiles);
                //首次加载显示根目录图片信息
                addImagePathToList(mParentDirList.get(0));
                return true;
            }

            @Override
            protected void onPostExecute(Boolean isSuccess) {
                mProgressDialog.dismiss();
                if (isSuccess) {
                    UpdatePhotoListData();
                    initPhotoDirPopupWindowView();
                } else {
                    Toast.makeText(context, "扫描图片失败！", Toast.LENGTH_SHORT).show();
                }
            }
        };
        task.execute();
    }

    /**
     * 刷新List中的图片数据
     */
    private void UpdatePhotoListData() {
        mAdater = new PhotoAdater(context, mDirPath, mList, gv_photo);
        gv_photo.setAdapter(mAdater);
        tv_dir_path.setText(mDirPath);
        tv_dir_image_count.setText(mDirImageCount + "");
    }

    /**
     * 根据指定目录上的所有图片信息
     *
     * @param floderBean
     */
    private void addImagePathToList(FloderBean floderBean) {
        mList.clear();
        mDirPath = floderBean.getPath();
        File file = new File(mDirPath);
        File[] files = file.listFiles(mMyimageFileFilter);
        mDirImageCount = files.length;
        for (int i = 0; i < mDirImageCount; i++) {
            mList.add(files[i].getName());
        }
    }

    /**
     * 获取根目录及其子目录的相关信息（路径、目录首个图片、图片个数）
     *
     * @param listFiles
     * @return
     */
    private void initParentDirDatas(File[] listFiles) {
        mParentDirList.clear();
        //根目录也可能会存在图片，因此根目录也需要添加至图片目录列表中
        mParentDirList.add(new FloderBean(PHOTO_DIR_PATH));
        File file;
        for (int i = 0; i < listFiles.length; i++) {
            file = listFiles[i];
            //将根目录中的所有文件夹路径均保存起来
            if (file.isDirectory()) {
                if (!mParentDirList.contains(file.getAbsolutePath())) {
                    mParentDirList.add(new FloderBean(file.getPath()));
                }
            }
        }
        //获取每个目录中的第一个图片信息
        File[] dirFiles;
        FloderBean floderBean;
        for (int i = 0; i < mParentDirList.size(); i++) {
            floderBean = mParentDirList.get(i);
            dirFiles = new File(floderBean.getPath()).listFiles(mMyimageFileFilter);
            if (dirFiles.length > 0) {
                floderBean.setFirstImageName(dirFiles[0].getName());
                floderBean.setImageCount(dirFiles.length);
            }
        }
    }

    /**
     * 初始化PhotoDirPopupWindowView
     */
    public void initPhotoDirPopupWindowView() {
        mPhotoDirPopupWindow = new PhotoDirPopupWindow(context, mParentDirList);
        mPhotoDirItemClickListener = new PhotoDirPopupWindow.PhotoDirItemClickListener() {
            @Override
            public void onItemClick(FloderBean floderBean) {
                //选择的文件夹与之前相同时，则不处理
                if (mDirPath.equals(floderBean.getPath())) {
                    mPhotoDirPopupWindow.dismiss();
                    return;
                }
                addImagePathToList(floderBean);
                UpdatePhotoListData();
                mPhotoDirPopupWindow.dismiss();
            }
        };
        mPhotoDirPopupWindow.setPhotoDirItemClickListener(mPhotoDirItemClickListener);
        mPhotoDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                changeActivityColorFilter(true);
            }
        });
    }

    /**
     * 当显示或隐藏PopupWindow时，给底部的界面添加底色，类似于关灯或开灯
     *
     * @param isDismiss
     */
    private void changeActivityColorFilter(boolean isDismiss) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (isDismiss) {
            layoutParams.alpha = 1.0f;
        } else {
            layoutParams.alpha = 0.2f;
        }
        getWindow().setAttributes(layoutParams);
    }

    /**
     * 底部布局点击事件，用于切换图片文件夹路径
     *
     * @param view
     */
    public void onChangePhotoDirPath(View view) {
        if (mPhotoDirPopupWindow == null) {
            return;
        }
        if (mPhotoDirPopupWindow.isShowing()) {
            mPhotoDirPopupWindow.dismiss();
        } else {
            mPhotoDirPopupWindow.showAsDropDown(rl_layout_dir, 0, 0);
            //由于选择图片后会更改父文件夹的选择状态，因此需要更新状态
            mPhotoDirPopupWindow.notifyDataSetChanged();
            changeActivityColorFilter(false);
        }
    }

    public static Set<String> getSelectImageSet() {
        return mSelectImageSet;
    }

    public static Set<String> getDirSelectImageSet() {
        return mDirSelectImageSet;
    }

}
