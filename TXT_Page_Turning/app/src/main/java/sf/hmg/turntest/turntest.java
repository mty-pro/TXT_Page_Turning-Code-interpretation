package sf.hmg.turntest;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class turntest extends Activity {
	/** Called when the activity is first created. */
	private PageWidget mPageWidget;             //自定义组件
	Bitmap mCurPageBitmap, mNextPageBitmap;		//位图
	Canvas mCurPageCanvas, mNextPageCanvas;		//画布
	BookPageFactory pagefactory;                //翻页工厂类实例

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		隐藏标题栏失效
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		让整个窗体全屏
//		getWindow() 是Activity 中的一个方法，获取当前的window
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mPageWidget = new PageWidget(this);
		setContentView(mPageWidget);

//		返回具有指定宽度和高度的可变位图，静态方法
//		Bitmap.Config：位图配置描述了像素的存储方式；
//		ARGB_8888：每个像素存储在4个字节上。
		mCurPageBitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		mNextPageBitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);

		mCurPageCanvas = new Canvas(mCurPageBitmap);
		mNextPageCanvas = new Canvas(mNextPageBitmap);


		//工厂实例
		pagefactory = new BookPageFactory(480, 800);


//		第一个参数是包含你要加载的位图资源文件的对象（一般写成 getResources（）就ok了）；
//		第二个时你需要加载的位图资源的Id。
		pagefactory.setBgBitmap(BitmapFactory.decodeResource(
				this.getResources(), R.drawable.bg));

		try {
////			pagefactory.openbook("/sdcard/test.txt");
////			pagefactory.openbook("/storage/emulated/0/e-book.txt");
			pagefactory.openbook("/storage/emulated/0/e-book1.txt");
			pagefactory.onDraw(mCurPageCanvas);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Toast.makeText(this, "电子书不存在,请将《test.txt》放在SD卡根目录下",
					Toast.LENGTH_SHORT).show();
		}

		mPageWidget.setBitmaps(mCurPageBitmap, mCurPageBitmap);

		mPageWidget.setOnTouchListener(new OnTouchListener() {
//			该方法的返回值类型主要用于控制是否执行onTouchEvent()方法
//			及onTouchEvent()方法内部内置的各种click点击事件是否执行。
//			利用此特性可以单独拦截某个View的点击事件
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				// TODO Auto-generated method stub
				
				boolean ret=false;
				if (v == mPageWidget) {
					if (e.getAction() == MotionEvent.ACTION_DOWN) {
						mPageWidget.abortAnimation();
						mPageWidget.calcCornerXY(e.getX(), e.getY());

						pagefactory.onDraw(mCurPageCanvas);
						if (mPageWidget.DragToRight()) {
							try {
								pagefactory.prePage();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}						
							if(pagefactory.isfirstPage())
								return false;

							pagefactory.onDraw(mNextPageCanvas);

						} else {
							try {
								pagefactory.nextPage();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if(pagefactory.islastPage())
								return false;
							//mNextPageCanvas来传改变mCurPageBitmap
							pagefactory.onDraw(mNextPageCanvas);
						}
						mPageWidget.setBitmaps(mCurPageBitmap,
								mNextPageBitmap);
					}
                 
					 ret = mPageWidget.doTouchEvent(e);
					return ret;
				}
				return false;
			}

		});
	}
}