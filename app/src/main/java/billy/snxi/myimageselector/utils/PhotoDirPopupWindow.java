package billy.snxi.myimageselector.utils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.List;

import billy.snxi.myimageselector.R;
import billy.snxi.myimageselector.adapter.PhotoDirAdapter;
import billy.snxi.myimageselector.bean.FloderBean;

/**
 * Created by Administrator on 2018-05-28.
 */
public class PhotoDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private ListView lv_photo_dir;
    private List<FloderBean> mList;
    private View mContentView;
    private PhotoDirAdapter mAdapter;
    private Context context;
    private PhotoDirItemClickListener mPhotoDirItemClickListener;

    public PhotoDirPopupWindow(Context context, List<FloderBean> mList) {
        super(context);
        this.context = context;
        calculateWidthAndHeight(context);
        setWidth(mWidth);
        setHeight(mHeight);
        mContentView = LayoutInflater.from(context).inflate(R.layout.popup_window_photo_dir, null);
        setContentView(mContentView);
        this.mList = mList;
        setFocusable(true);
        setTouchable(true);
        //设置PopupWindow外部可点击
        setOutsideTouchable(true);
        //设置PopupWindow外部可点击的载体，有了载体时上句才有效
        setBackgroundDrawable(new BitmapDrawable());
        //设置点击PopupWindow外部时就会消失
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        intiViews();
    }

    /**
     * 初始化PopueWindow中相关View控件
     */
    private void intiViews() {
        lv_photo_dir = (ListView) mContentView.findViewById(R.id.lv_photo_dir);
        mAdapter = new PhotoDirAdapter(context, mList);
        lv_photo_dir.setAdapter(mAdapter);
        lv_photo_dir.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mPhotoDirItemClickListener != null) {
                    mPhotoDirItemClickListener.onItemClick(mList.get(position));
                }
            }
        });
    }

    /**
     * 计算PopupWindow的宽和高，此处宽使用屏幕宽度，高使用屏幕高度的80%
     */
    private void calculateWidthAndHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mWidth = displayMetrics.widthPixels;
        mHeight = (int) (displayMetrics.heightPixels * 0.7f);
    }

    public void setPhotoDirItemClickListener(PhotoDirItemClickListener photoDirItemClickListener) {
        mPhotoDirItemClickListener = photoDirItemClickListener;
    }

    public interface PhotoDirItemClickListener {
        void onItemClick(FloderBean floderBean);
    }

    /**
     * 刷新List数据，因为该文件夹下图片有选中时会该标记文件夹勾选状态
     */
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }


}
