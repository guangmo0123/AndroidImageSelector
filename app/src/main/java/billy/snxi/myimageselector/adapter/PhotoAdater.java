package billy.snxi.myimageselector.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import billy.snxi.myimageselector.LoadImageByPhotoActivity;
import billy.snxi.myimageselector.R;
import billy.snxi.myimageselector.utils.ImageLoader;

/**
 * Created by Administrator on 2018-05-26.
 */
public class PhotoAdater extends BaseAdapter implements AbsListView.OnScrollListener {

    private LayoutInflater mInflater;
    private String mDirPath;
    private List<String> mList;
    private GridView mGridView;
    //保存已被选择的图片路径
    private Set<String> mSelectImageSet;
    //保存已被选择的图片的父目录路径
    private Set<String> mDirSelectImageSet;
    private int mStart;
    private int mEnd;
    private boolean mFirstLoad;

    public PhotoAdater(Context context, String dirPath, List<String> list, GridView gridView) {
        mInflater = LayoutInflater.from(context);
        mDirPath = dirPath;
        mList = list;
        mGridView = gridView;
        mDirSelectImageSet = LoadImageByPhotoActivity.getDirSelectImageSet();
        mSelectImageSet = LoadImageByPhotoActivity.getSelectImageSet();
        mGridView.setOnScrollListener(this);
        mStart = 0;
        mEnd = 0;
        mFirstLoad = true;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_photo, null);
            holder.iv_pic = (ImageView) convertView.findViewById(R.id.iv_pic);
            holder.iv_select_state = (ImageView) convertView.findViewById(R.id.iv_select_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final String filePath = mDirPath + "/" + mList.get(position);
        //将path与view绑定
        holder.iv_pic.setTag(filePath);

        //给ImageView设置点击事件
        holder.iv_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存被选中的图片
                changeSelectedImageInfo(filePath);
                notifyDataSetChanged();
            }
        });
        //使用ImageLoader类加载图片，此处使用了滚动停止后才加载图片，因此此时不使用直接加载的方法
//        ImageLoader.getInstance().loadImageFromFilePath(filePath, holder.iv_pic);
        //若该图片有被选中，则显示选中标记和添加颜色滤镜
        if (mSelectImageSet.contains(filePath)) {
            holder.iv_pic.setColorFilter(Color.parseColor("#77000000"));
            holder.iv_select_state.setImageResource(R.mipmap.selector);
        } else {
            holder.iv_pic.setColorFilter(null);
            holder.iv_select_state.setImageBitmap(null);
        }
        return convertView;
    }


    /**
     * onScrollStateChanged为AbsListView滚动状态发生改变时调用
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //当停止滚动时，再加载图片
        if (scrollState == SCROLL_STATE_IDLE) {
            ImageLoader.getInstance().setLoadTaskState(false);
            //加载listview中可见的item图片
            loadVisibleItem(mStart, mEnd);
        } else {
            ImageLoader.getInstance().setLoadTaskState(true);
        }
    }

    /**
     * onScroll方法在listview中时一直调用的
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mStart = firstVisibleItem;
        mEnd = firstVisibleItem + visibleItemCount;
        //当为listview首次打开时，由于未触发onScrollStateChanged事件，因此需要单独去加载首页中可见的item
        if (mFirstLoad && visibleItemCount > 0) {
            //加载listview中可见的item图片
            loadVisibleItem(mStart, mEnd);
            mFirstLoad = false;
        }
    }

    /**
     * 加载AbsListView中可见的Item
     *
     * @param start
     * @param end
     */
    private void loadVisibleItem(int start, int end) {
        for (int i = start; i < end; i++) {
            String filePath = mDirPath + "/" + mList.get(i);
            ImageView imageView = (ImageView) mGridView.findViewWithTag(filePath);
            if (imageView != null) {
                ImageLoader.getInstance().loadImageFromFilePath(filePath, imageView);
            }
        }
    }

    /**
     * 保存被选中的图片
     */
    private void changeSelectedImageInfo(String filePath) {
        //若该图片和父文件夹已被选中，则移除该选择中的图片信息
        if (mSelectImageSet.contains(filePath)) {
            mSelectImageSet.remove(filePath);
            //若选择的图片数量为0，则不再循环，直接移除所保存的父文件夹路径
            if (mSelectImageSet.size() == 0) {
                mDirSelectImageSet.remove(mDirPath);
                return;
            }
            boolean isDirIncludeFile = false;
            Iterator<String> iterator = mSelectImageSet.iterator();
            String dirPath;
            while (iterator.hasNext()) {
                dirPath = iterator.next();
                //获取父类文件夹路径
                dirPath = dirPath.substring(0, dirPath.lastIndexOf("/"));
                //若保存的选中图片中父目录与当前目录都不相同，则说明当前目录没有图片被选中
                if (mDirPath.equals(dirPath)) {
                    isDirIncludeFile = true;
                    break;
                }
            }
            //若该文件夹没有图片被选中，则也移除该文件夹路径
            if (!isDirIncludeFile) {
                mDirSelectImageSet.remove(mDirPath);
            }
        } else {
            mSelectImageSet.add(filePath);
            if (!mDirSelectImageSet.contains(mDirPath)) {
                mDirSelectImageSet.add(mDirPath);
            }
        }
    }

    private class ViewHolder {
        ImageView iv_pic;
        ImageView iv_select_state;
    }

}
