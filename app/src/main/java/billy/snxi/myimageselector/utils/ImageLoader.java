package billy.snxi.myimageselector.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片通用异步加载类，可根据参数来加载网络图片和本地图片文件<br/>
 * 对外通用方法为getInstance()
 */
public class ImageLoader {
    /**
     * 图片加载的顺序类型
     */
    public enum LoadType {
        FRIST_IN_FIRST_LOAD,    //先进先加载
        LAST_IN_FIRST_LOAD;     //后进先加载
    }

    /**
     * 根据url存放图片缓存
     */
    private LruCache<String, Bitmap> mLruCacheUrlBitmap;
    /**
     * 异步加载队列的线程池
     */
    private ExecutorService mExecutorService;
    /**
     * 线程任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 更新UI的UIHandler
     */
    private Handler mUIHandler;
    /**
     * 后台轮询线程及handler
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /**
     * 线程信号量，用于人为的控制当前并行线程运行的个数，可人为的阻塞和释放阻塞线程，可以在多线程之间使用<br/>
     * 常用于多线程用到同一个变量时，为防止变量还未初始化时，在另一线程被使用的问题，
     * 可以将线程先阻塞，待初始化线程执行后，再释放阻塞，让多线程继续执行
     */
    private Semaphore mSemaphorePoolThreadHandler;
    /**
     * 用于控制mExecutorService线程池在多线程数已达到并发上限时，则此时停止继续向线程池中添加Runnable任务
     * 待线程池中有线程执行完后，再释放一个信号量来通知程序可以继续向线程池中添加Runnable任务，
     * 此时再根据加载类型（LoadType）来从任务队列中获取相应的顺序任务<br/>
     * 原理：由于本类设置了加载类型，若不限制线程池添加线程，当线程任务一加入任务队列时，
     * 就会通知mPoolThreadHandler来执行线程任务，此时几乎是来一个线程执行一个线程，导致线程任务队列始终只有一个，
     * 从而导致加载类型获取任务顺序几乎无效
     */
    private Semaphore mSemaphorePoolThreadExecute;
    /**
     * 加载模式，先进先加载、后进先加载
     */
    private LoadType mLoadType = LoadType.LAST_IN_FIRST_LOAD;
    /**
     * 屏幕的信息，宽和高
     */
    private int mScreenWidth;
    private int mScreenHeight;
    /**
     * 设置当前加载类是否处于停止状态
     */
    private boolean isStopLoadTask = false;
    /**
     * 单例模式
     */
    private static ImageLoader mImageLoader;


    private ImageLoader(LoadType loadType, int threadPoolCount) {
        mLoadType = loadType;
        if (threadPoolCount <= 0) {
            int cpuCount = Runtime.getRuntime().availableProcessors();
            if (cpuCount < 1) {
                cpuCount = 1;
            }
            threadPoolCount = cpuCount * 2;
        }
        //初始化单例对象中的相关变量
        init(threadPoolCount);
    }

    /**
     * 单例模式并进行异步线程来加载图片<br/>
     * 加载模式：后进先出<br/>
     * 异步加载图片线程池的线程数量：cpu核心数*2
     */
    public static ImageLoader getInstance() {
        return getInstance(LoadType.LAST_IN_FIRST_LOAD, 0);
    }


    /**
     * 单例模式并进行异步线程来加载图片
     *
     * @param loadType        加载类型，可设置为“先进先出”或“后进先出”
     * @param threadPoolCount 异步加载图片线程池的线程数量，若<=0，则默认为cpu核心数*2
     */
    public static ImageLoader getInstance(LoadType loadType, int threadPoolCount) {
        if (mImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (mImageLoader == null) {
                    mImageLoader = new ImageLoader(loadType, threadPoolCount);
                }
            }
        }
        return mImageLoader;
    }

    /**
     * 类创建时初始化相关变量
     */
    private void init(int threadPoolCount) {
        //获取当前程序的最大内存
        int appMaxMemory = (int) Runtime.getRuntime().maxMemory();
        mLruCacheUrlBitmap = new LruCache<String, Bitmap>(appMaxMemory / 5) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        //创建线程池
        mExecutorService = Executors.newFixedThreadPool(threadPoolCount);
        //创建线程任务队列
        mTaskQueue = new LinkedList<>();
        //创建线程信号量，用于控制并行线程的执行的个数，此处设置0为表示阻塞所有线程
        mSemaphorePoolThreadHandler = new Semaphore(0);
        //创建线程信号量，用于控制并行线程的执行的个数，此处设置与线程池中最大线程数一致，
        mSemaphorePoolThreadExecute = new Semaphore(threadPoolCount);
        //更新ui的handler
        mUIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg != null) {
                    ImageHolder holder = (ImageHolder) msg.obj;
                    ImageView imageView = holder.imageView;
                    if (imageView.getTag().toString().equals(holder.url)) {
                        imageView.setImageBitmap(holder.bitmap);
                    }
                }
            }
        };
        //后台轮询线程，利用Handler和MessageQueue循环机制来实现后台线程循环
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                //后台轮询线程的handler
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (mTaskQueue.size() == 0) {
                            return;
                        }
                        mExecutorService.execute(getTaskQueue());
                        try {
                            //向线程池添加一个线程时，则信号量阻塞一个线程，当线程池并发线程达到上限时，则不再向线程池中添加线程
                            mSemaphorePoolThreadExecute.acquire();
                        } catch (InterruptedException e) {
                        }
                    }
                };
                //由于有其它线程有使用mPoolThreadHandler变量，因此此处在变量初始化完成后，释放线程信号量，不限制并行线程执行的个数
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
    }

    /**
     * 根据加载类型来返回对应的任务
     */
    private Runnable getTaskQueue() {
        if (mLoadType == LoadType.FRIST_IN_FIRST_LOAD) {
            return mTaskQueue.removeFirst();
        } else if (mLoadType == LoadType.LAST_IN_FIRST_LOAD) {
            return mTaskQueue.removeLast();
        }
        return mTaskQueue.removeLast();
    }

    /**
     * ImageLoader类对外的开放用来加载图片的主方法，用于加载网络url图片
     *
     * @param url
     * @param imageView
     */
    public void loadImageFromURL(final String url, final ImageView imageView) {
        loadImage(url, imageView, true);
    }

    /**
     * ImageLoader类对外的开放用来加载图片的主方法，用于加载本地FilePath图片
     *
     * @param path
     * @param imageView
     */
    public void loadImageFromFilePath(String path, ImageView imageView) {
        loadImage(path, imageView, false);
    }

    /**
     * 加载图片的主方法，并可根据图片的来源（网络图片的url或本地图片path）来获取图片信息
     *
     * @param path           图片来源，可为网络图片的url或本地图片的FilePath
     * @param imageView      需要显示的ImageView
     * @param isNetworkImage 若为图片来源于网络url，则为true；若图片来源为本地文件path，则为false
     */
    private void loadImage(final String path, final ImageView imageView, final boolean isNetworkImage) {
        //先从缓存中获取图片信息
        Bitmap bitmap = getBitmapFromLruCache(path);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }
        //当处于停止状态，则不异步加载图片
        if (isStopLoadTask) {
            return;
        }
        //异步处理加载图片
        addTaskToQuere(new Runnable() {
            @Override
            public void run() {
                if (isStopLoadTask) {
                    return;
                }
//                Log.d("tag-billy", path);
                //加载图片并返回压缩后的图片
                Bitmap bitmap = getCompressBitmap(path, getImageViewSizes(imageView), isNetworkImage);
                if (bitmap == null) {
                    Log.d("tag-billy", "获取本地图片失败！" + path);
                    return;
                }
                //将图片加入缓存中
                addBitmapToLruCache(path, bitmap);
                //发送消息
                sendMessageToUIHandler(path, imageView, bitmap);
                //线程池中的线程执行完后，释放一个信号量，停止阻塞一个线程，允许程序继续添加线程池中添加线程
                mSemaphorePoolThreadExecute.release();
            }
        });
    }

    /**
     * 发送消息到UIHandler来通知更新UI
     *
     * @param url
     * @param imageView
     * @param bitmap
     */
    private void sendMessageToUIHandler(String url, ImageView imageView, Bitmap bitmap) {
        Message msg = mUIHandler.obtainMessage();
        ImageHolder bean = new ImageHolder(url, imageView, bitmap);
        msg.obj = bean;
        mUIHandler.sendMessage(msg);
    }

    /**
     * 向线程队列中添加线程任务
     *
     * @param runnable
     */
    private synchronized void addTaskToQuere(Runnable runnable) {
        mTaskQueue.add(runnable);
        //由于mPoolThreadHandler是在后台轮训子线程中初始化，因此需要进行判断并进行线程阻塞，直到初始化完成
        if (mPoolThreadHandler == null) {
            try {
                mSemaphorePoolThreadHandler.acquire();
            } catch (InterruptedException e) {
            }
        }
        mPoolThreadHandler.sendEmptyMessage(0);
    }

    /**
     * 将bitmap添加至缓存中
     *
     * @param url
     * @param bm
     */
    private void addBitmapToLruCache(String url, Bitmap bm) {
        if (mLruCacheUrlBitmap.get(url) == null) {
            mLruCacheUrlBitmap.put(url, bm);
        }
    }

    /**
     * 从缓存中获取bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCacheUrlBitmap.get(key);
    }

    /**
     * 根据view的显示尺寸来对bitmap进行压缩，以节省内存
     *
     * @param path
     * @param viewSizes
     * @return
     */
    private Bitmap getCompressBitmap(String path, int[] viewSizes, boolean isNetworkImage) {
        //对bitmap进行参数配置
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        //为true时，可不读出图片的情况下获取到图片的大小，再根据大小来设置压缩比例
        bitmapOptions.inJustDecodeBounds = true;
        //先不读取图片的数据，只获取图片的大小信息，再根据ImageView与图片的大小来进行压缩
        byte[] imageBytes = null;
        if (isNetworkImage) {
            //若为网络图片，则获取对应的io流，再转化为byte[]
            InputStream is = null;
            try {
                //URL().openStream()此方法获取的InputStream对象只能使用一次，因此不能直接使用BitmapFactory.decodeStream()
                //需要将InputStream转化为byte[]后再处理压缩
                is = new URL(path).openStream();
                imageBytes = getBytesFromInputStream(is);
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                }
            }
        } else {
            BitmapFactory.decodeFile(path, bitmapOptions);
        }
        //设置图片压缩比，为>=1的整数，实际压缩时为1/inSampleSize
        bitmapOptions.inSampleSize = getBitmapInSampleSize(bitmapOptions, viewSizes);
        bitmapOptions.inJustDecodeBounds = false;
        //对根据inSampleSize的值来返回压缩后的bitmap
        Bitmap bitmap = null;
        if (isNetworkImage) {
            bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);
        } else {
            bitmap = BitmapFactory.decodeFile(path, bitmapOptions);
        }
        return bitmap;
    }

    /**
     * 将InputStream转化为byte[]
     */
    private byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    /**
     * 获取ImageView的尺寸，方便后续对根据此尺寸对图片进行压缩
     *
     * @param imageView
     * @return
     */
    private int[] getImageViewSizes(ImageView imageView) {
        int width, heigth;
        //先获取view的实际大小
        width = imageView.getWidth();
        heigth = imageView.getHeight();
        //若值不合法，则获取layout中设置的值
        if (width <= 0 || heigth <= 0) {
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            if (width <= 0) {
                width = layoutParams.width;
            }
            if (heigth <= 0) {
                heigth = layoutParams.height;
            }
        }
        //若layout中设置是wrap_content(-2)或match_parent(-1)，则使用屏幕大小
        //此处一般在layout中已经设定图片的显示范围，比如GridView，一行显示3个图片，则此时图片的宽度可以设定为：mScreenWidth/3
        if (width <= 0 || heigth <= 0) {
            if (mScreenWidth == 0 || mScreenHeight == 0) {
                DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
                mScreenWidth = displayMetrics.widthPixels;
                mScreenHeight = displayMetrics.heightPixels;
            }
            if (width <= 0) {
                width = mScreenWidth;
            }
            if (heigth <= 0) {
                heigth = mScreenHeight;
            }
        }
        return new int[]{width, heigth};
    }

    /**
     * 根据图片的尺码与ImageView的显示尺寸来计算压缩比例
     *
     * @param bitmapOptions
     * @param viewSizes
     * @return
     */
    private int getBitmapInSampleSize(BitmapFactory.Options bitmapOptions, int[] viewSizes) {
        //获取图片的实际尺寸
        int realWidth = bitmapOptions.outWidth;
        int realHeight = bitmapOptions.outHeight;
        //计算宽度压缩比
        int sampleSizeWidth = Math.round(1.0f * realWidth / viewSizes[0]);
        int sampleSizeHeigth = Math.round(1.0f * realHeight / viewSizes[1]);
        //取宽和高压缩比的较大值
        int resultSampleSize = Math.max(sampleSizeWidth, sampleSizeHeigth);
        if (resultSampleSize < 1) {
            resultSampleSize = 1;
        }
        return resultSampleSize;
    }

    /**
     * 设置加载任务的状态，停止或执行加载任务
     */
    public void setLoadTaskState(boolean isStopLoad) {
        isStopLoadTask = isStopLoad;
        if (isStopLoadTask) {
            //清除所有加载任务
            mTaskQueue.clear();
        }
    }

    class ImageHolder {
        String url;
        ImageView imageView;
        Bitmap bitmap;

        public ImageHolder(String url, ImageView imageView, Bitmap bitmap) {
            this.url = url;
            this.imageView = imageView;
            this.bitmap = bitmap;
        }
    }

}
