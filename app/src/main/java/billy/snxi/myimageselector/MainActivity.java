package billy.snxi.myimageselector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by Administrator on 2018-05-25.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 异步加载网络图片
     *
     * @param view
     */
    public void onLoadNetworkImage(View view) {
        startActivity(new Intent(this, LoadImageByNetworkActivity.class));
    }

    /**
     * 异步本地相册图片
     *
     * @param view
     */
    public void onLoadLocalImage(View view) {
        startActivity(new Intent(this, LoadImageByPhotoActivity.class));
    }
}
