package billy.snxi.myimageselector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import billy.snxi.myimageselector.bean.CourseBean;
import billy.snxi.myimageselector.R;
import billy.snxi.myimageselector.utils.ImageLoader;

/**
 * Created by Administrator on 2018-05-26.
 */
public class CourseAdater extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<CourseBean> mList;

    public CourseAdater(Context context, List<CourseBean> mList) {
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
            convertView = mInflater.inflate(R.layout.item_course, null);
            holder.iv_pic = (ImageView) convertView.findViewById(R.id.iv_pic);
            holder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            holder.tv_description = (TextView) convertView.findViewById(R.id.tv_description);
            holder.tv_learner = (TextView) convertView.findViewById(R.id.tv_learner);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //设置默认图片，后面会再异步加载正确的图片
        holder.iv_pic.setImageResource(R.mipmap.img_default);
        CourseBean bean = mList.get(position);
        //将path与view绑定
        holder.iv_pic.setTag(bean.getPicURL());
        //使用ImageLoader类加载图片
        ImageLoader.getInstance().loadImageFromURL(bean.getPicURL(), holder.iv_pic);
        holder.tv_name.setText(bean.getId() + "." + bean.getName());
        holder.tv_description.setText(bean.getDescription());
        holder.tv_learner.setText(bean.getLearner() + "");
        return convertView;
    }

    class ViewHolder {
        ImageView iv_pic;
        TextView tv_name;
        TextView tv_description;
        TextView tv_learner;
    }

}
