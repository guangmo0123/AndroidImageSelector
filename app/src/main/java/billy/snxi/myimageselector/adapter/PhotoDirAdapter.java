package billy.snxi.myimageselector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import billy.snxi.myimageselector.LoadImageByPhotoActivity;
import billy.snxi.myimageselector.R;
import billy.snxi.myimageselector.bean.FloderBean;
import billy.snxi.myimageselector.utils.ImageLoader;

/**
 * Created by Administrator on 2018-05-28.
 */
public class PhotoDirAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<FloderBean> mList;

    public PhotoDirAdapter(Context context, List<FloderBean> mList) {
        mInflater = LayoutInflater.from(context);
        this.mList = mList;
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
            convertView = mInflater.inflate(R.layout.item_photo_dir, null);
            holder.iv_pic = (ImageView) convertView.findViewById(R.id.iv_pic);
            holder.tv_dir_path = (TextView) convertView.findViewById(R.id.tv_dir_path);
            holder.tv_image_count = (TextView) convertView.findViewById(R.id.tv_image_count);
            holder.iv_select_state = (ImageView) convertView.findViewById(R.id.iv_select_state);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //重置状态，设置默认图片，后面会再异步加载正确的图片
        holder.iv_pic.setImageResource(R.mipmap.img_default);
        holder.iv_select_state.setImageBitmap(null);

        FloderBean bean = mList.get(position);
        final String filePath = bean.getPath() + "/" + bean.getFirstImageName();
        //将path与view绑定
        holder.iv_pic.setTag(filePath);
        //使用ImageLoader类加载图片
        ImageLoader.getInstance().loadImageFromFilePath(filePath, holder.iv_pic);

        holder.tv_dir_path.setText(bean.getPath());
        holder.tv_image_count.setText(bean.getImageCount() + "");
        //若该文件夹下有图片被选中，则该文件夹后面也显示勾选标记
        if (LoadImageByPhotoActivity.getDirSelectImageSet().contains(bean.getPath())) {
            holder.iv_select_state.setImageResource(R.mipmap.selector);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView iv_pic;
        TextView tv_dir_path;
        TextView tv_image_count;
        ImageView iv_select_state;
    }
}
