package sf.hmg.turntest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class PageWidget extends View {

	private static final String TAG = "hmg";
	private int mWidth = 480;//组件的宽度
	private int mHeight = 800;//组件的高度
	private int mCornerX = 0; // 拖拽点对应的页脚x
	private int mCornerY = 0;// 拖拽点对应的页脚y

//	Path类封装由直线段，二次曲线和三次曲线组成的复合（多个轮廓）几何路径。
//	可以使用canvas.drawPath（path，paint）对其进行填充或描边绘制（基于Paint的Style），
//	也可以用于剪切或在路径上绘制文本。
	private Path mPath0;	//把书页翻起来后看到的背面区域∪把书页翻起来后看到的下一页的一角
	private Path mPath1;	//把书页翻起来后看到的背面区域

	Bitmap mCurPageBitmap = null; // 当前页
	Bitmap mNextPageBitmap = null;//下一页

	PointF mTouch = new PointF(); // 拖拽点	；	保存两个坐标(x,y)

//	起点和终点都只有一个，但是控制点可以多个，甚至是0，0的时候就是直线啦！
	//这里的是1个控制点的贝塞尔曲线，顶点是曲线的顶点
	PointF mBezierStart1 = new PointF(); // 贝塞尔曲线起始点
	PointF mBezierControl1 = new PointF(); // 贝塞尔曲线控制点
	PointF mBeziervertex1 = new PointF(); // 贝塞尔曲线顶点
	PointF mBezierEnd1 = new PointF(); // 贝塞尔曲线结束点

	// 另一条贝塞尔曲线
	PointF mBezierStart2 = new PointF();
	PointF mBezierControl2 = new PointF();
	PointF mBeziervertex2 = new PointF();
	PointF mBezierEnd2 = new PointF();

	float mMiddleX;							//触摸点和（四）角的中点的横坐标
	float mMiddleY;							//触摸点和（四）角的中点的纵坐标
	float mDegrees;							//∠hef的度数
	float mTouchToCornerDis;				//触摸点到（四）角的距离

//	通过4x5颜色矩阵转换颜色的滤色器。该滤镜可用于更改像素的饱和度，从YUV转换为RGB等。
	ColorMatrixColorFilter mColorMatrixFilter;

	Matrix mMatrix;
	float[] mMatrixArray = { 0, 0, 0, 0, 0, 0, 0, 0, 1.0f };

	boolean mIsRTandLB; // 是否属于右上左下
	float mMaxLength = (float) Math.hypot(mWidth, mHeight);		//对角线return sqrt(x^2+y^2)
	int[] mBackShadowColors;
	int[] mFrontShadowColors;
	//具有颜色渐变的Drawable，用于按钮，背景等。
	GradientDrawable mBackShadowDrawableLR;		//背后？左到右
	GradientDrawable mBackShadowDrawableRL;		//右到左
	GradientDrawable mFolderShadowDrawableLR;	//上面的那页从左向右折起来的背面
	GradientDrawable mFolderShadowDrawableRL;	//上面的那页从右向左折起来的背面

	GradientDrawable mFrontShadowDrawableHBT;	//水平下到上
	GradientDrawable mFrontShadowDrawableHTB;	//水平上到下
	GradientDrawable mFrontShadowDrawableVLR;	//垂直左到右
	GradientDrawable mFrontShadowDrawableVRL;	//垂直右到左

	Paint mPaint;

//	此类封装了滚动。您可以使用滚动条（Scroller或OverScroller）收集产生滚动动画所需的数据，
//	例如，响应于挥动手势。滚动条会随着时间的推移为您跟踪滚动偏移量，但不会自动将这些位置应用于您的视图。
//	您有责任获得和应用新坐标的速率应可使滚动动画看起来平滑。

//	Scroller只是个计算器，提供插值计算，让滚动过程具有动画属性，但它并不是UI，也不是辅助UI滑动，反而是单纯地为滑动提供计算。
	Scroller mScroller;

	public PageWidget(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mPath0 = new Path();
		mPath1 = new Path();
		createDrawable();

		/*
		Android在用画笔的时候有三种Style，分别是
		Paint.Style.STROKE 只绘制图形轮廓（描边）
		Paint.Style.FILL 只绘制图形内容
		Paint.Style.FILL_AND_STROKE 既绘制轮廓也绘制内容
		 */
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);

		ColorMatrix cm = new ColorMatrix();
		float array[] = { 0.55f, 0, 0, 0, 80.0f, 0, 0.55f, 0, 0, 80.0f, 0, 0,
				0.55f, 0, 80.0f, 0, 0, 0, 0.2f, 0 };
		cm.set(array);
		mColorMatrixFilter = new ColorMatrixColorFilter(cm);
		mMatrix = new Matrix();

		mScroller = new Scroller(getContext());

		mTouch.x = 0.01f; // 不让x,y为0,否则在点计算时会有问题
		mTouch.y = 0.01f;
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 计算拖拽点对应的拖拽脚
	 */

	//左上角的点坐标为（0，0）
	public void calcCornerXY(float x, float y) {
		if (x <= mWidth / 2)
			mCornerX = 0;			//页面的四个角
		else
			mCornerX = mWidth;
		if (y <= mHeight / 2)
			mCornerY = 0;
		else
			mCornerY = mHeight;
		if ((mCornerX == 0 && mCornerY == mHeight)
				|| (mCornerX == mWidth && mCornerY == 0))
			mIsRTandLB = true;				//属于右上左下
		else
			mIsRTandLB = false;				//不属于右上左下
	}

	public boolean doTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		//手指在组件上移动时实时获取触摸点的坐标并刷新重绘界面
		if (event.getAction() ==
				MotionEvent.ACTION_MOVE) {
			mTouch.x = event.getX();
			mTouch.y = event.getY();
			//实现界面刷新重绘界面
			this.postInvalidate();

//			invalidate()主线程
//			在UI线程调用
//			postInvalidate()工作者线程
//			在非UI线程调用
//			把主要是负责控制UI界面的显示、更新和控件交互的线程称为UI线程
		}
		//手指在组件上按下时实时获取触摸点的坐标
		if (event.getAction() ==
				MotionEvent.ACTION_DOWN) {
			mTouch.x = event.getX();
			mTouch.y = event.getY();
			// calcCornerXY(mTouch.x, mTouch.y);
			// this.postInvalidate();
		}
		//手指在组件上抬起时判断翻起的那页是否能够翻过去，如果可以就开始翻页动画，如果不可以该页顺着翻起的轨迹落下，最后实时刷新重绘界面
		if (event.getAction() ==
				MotionEvent.ACTION_UP) {
			if (canDragOver()) {
				startAnimation(1200);
			} else {
				mTouch.x = mCornerX - 0.09f;
				mTouch.y = mCornerY - 0.09f;
			}

			this.postInvalidate();
		}
		// return super.onTouchEvent(event);
		return true;
//		事件被消费
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 求解直线P1P2和直线P3P4的交点坐标
	 */
	public PointF getCross(PointF P1, PointF P2, PointF P3, PointF P4) {
		PointF CrossP = new PointF();
		// 二元函数通式： y=ax+b
		float a1 = (P2.y - P1.y) / (P2.x - P1.x);
		float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);

		float a2 = (P4.y - P3.y) / (P4.x - P3.x);
		float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
		CrossP.x = (b2 - b1) / (a1 - a2);
		CrossP.y = a1 * CrossP.x + b1;
		return CrossP;
	}


	//
	private void calcPoints() {
		//g点坐标
		mMiddleX = (mTouch.x + mCornerX) / 2;
		mMiddleY = (mTouch.y + mCornerY) / 2;

		//e点坐标
		mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
				* (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
		mBezierControl1.y = mCornerY;

		//h点坐标
		mBezierControl2.x = mCornerX;
		mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
				* (mCornerX - mMiddleX) / (mCornerY - mMiddleY);

		// Log.i("hmg", "mTouchX  " + mTouch.x + "  mTouchY  " + mTouch.y);
		// Log.i("hmg", "mBezierControl1.x  " + mBezierControl1.x
		// + "  mBezierControl1.y  " + mBezierControl1.y);
		// Log.i("hmg", "mBezierControl2.x  " + mBezierControl2.x
		// + "  mBezierControl2.y  " + mBezierControl2.y);


		//c点坐标
		mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x)
				/ 2;
		mBezierStart1.y = mCornerY;

		// 当mBezierStart1.x < 0或者mBezierStart1.x > 480时
		// 如果继续翻页，会出现BUG故在此限制
		if (mTouch.x > 0 && mTouch.x < mWidth) {
			if (mBezierStart1.x < 0 || mBezierStart1.x > mWidth) {
				if (mBezierStart1.x < 0)
					mBezierStart1.x = mWidth - mBezierStart1.x;

				float f1 = Math.abs(mCornerX - mTouch.x);
				float f2 = mWidth * f1 / mBezierStart1.x;
				mTouch.x = Math.abs(mCornerX - f2);

				float f3 = Math.abs(mCornerX - mTouch.x)
						* Math.abs(mCornerY - mTouch.y) / f1;
				mTouch.y = Math.abs(mCornerY - f3);

				mMiddleX = (mTouch.x + mCornerX) / 2;
				mMiddleY = (mTouch.y + mCornerY) / 2;

				mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
						* (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
				mBezierControl1.y = mCornerY;

				mBezierControl2.x = mCornerX;
				mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
						* (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
				// Log.i("hmg", "mTouchX --> " + mTouch.x + "  mTouchY-->  "
				// + mTouch.y);
				// Log.i("hmg", "mBezierControl1.x--  " + mBezierControl1.x
				// + "  mBezierControl1.y -- " + mBezierControl1.y);
				// Log.i("hmg", "mBezierControl2.x -- " + mBezierControl2.x
				// + "  mBezierControl2.y -- " + mBezierControl2.y);
				mBezierStart1.x = mBezierControl1.x
						- (mCornerX - mBezierControl1.x) / 2;
			}
		}

		//j点坐标
		mBezierStart2.x = mCornerX;
		mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y)
				/ 2;


//		Math.hypot方法返回所有参数的平方和的平方根。
		//af的长度
		mTouchToCornerDis = (float) Math.hypot((mTouch.x - mCornerX),
				(mTouch.y - mCornerY));

		//b点坐标
		mBezierEnd1 = getCross(mTouch, mBezierControl1, mBezierStart1,
				mBezierStart2);

		//k点坐标
		mBezierEnd2 = getCross(mTouch, mBezierControl2, mBezierStart1,
				mBezierStart2);

		// Log.i("hmg", "mBezierEnd1.x  " + mBezierEnd1.x + "  mBezierEnd1.y  "
		// + mBezierEnd1.y);
		// Log.i("hmg", "mBezierEnd2.x  " + mBezierEnd2.x + "  mBezierEnd2.y  "
		// + mBezierEnd2.y);

		/*
		 * mBeziervertex1.x 推导
		 * ((mBezierStart1.x+mBezierEnd1.x)/2+mBezierControl1.x)/2 化简等价于
		 * (mBezierStart1.x+ 2*mBezierControl1.x+mBezierEnd1.x) / 4
		 */


		//d点坐标
		mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
		mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;

		//i点坐标
		mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
		mBeziervertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
	}







	//画当前页
	private void drawCurrentPageArea(Canvas canvas, Bitmap bitmap, Path path) {
		mPath0.reset();
		mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);

//		贝赛尔曲线的实现
//		第一个参数：控制点的x坐标
//		第二个参数：控制点的y坐标
//		第三个参数：结束点的x坐标
//		第四个参数：结束点的y坐标
		mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x,
				mBezierEnd1.y);

		//画直线
		mPath0.lineTo(mTouch.x, mTouch.y);

		mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);

		mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x,
				mBezierStart2.y);
		mPath0.lineTo(mCornerX, mCornerY);
//		如果连接Path起点和终点能形成一个闭合图形，则会将起点和终点连接起来形成一个闭合图形
		mPath0.close();
		//画完了

		canvas.save();
		canvas.clipPath(path, Region.Op.XOR);

//		把bitmap显示到left, top所指定的左上角位置.
//		第一个参数为要绘制的bitmap对象，
//		第二个参数为图片左上角的x坐标值，
//		第三个参数为图片左上角的y坐标的值，
//		第三个参数为Paint对象。
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.restore();
	}


	//画下面的页面上区域以及上面的阴影
	private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {

		//下一页的区域
		mPath1.reset();
		mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.lineTo(mCornerX, mCornerY);
		mPath1.close();

//		toDegrees() 方法用于将参数转化为角度。
//		Math.atan2函数有两个参数x，y。该函数返回的值也是一个弧度值。
//		它代表的是坐标(0,0)指向坐标(x,y)的向量方向和x轴坐标的角度值。
//		double angle = Math.atan2(x,y);
		//∠hef的度数
		mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
				- mCornerX, mBezierControl2.y - mCornerY));


		int leftx;		//阴影的左端点
		int rightx;		//阴影的右端点
		GradientDrawable mBackShadowDrawable;
		if (mIsRTandLB) {						//如果左下或者右上时
			leftx = (int) (mBezierStart1.x);	//c点横坐标
			rightx = (int) (mBezierStart1.x + mTouchToCornerDis / 4);
			//左上到右下？
			mBackShadowDrawable = mBackShadowDrawableLR;
		} else {
			leftx = (int) (mBezierStart1.x - mTouchToCornerDis / 4);
			rightx = (int) mBezierStart1.x;
			//右上到左下？
			mBackShadowDrawable = mBackShadowDrawableRL;
		}
		canvas.save();

		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);

		canvas.drawBitmap(bitmap, 0, 0, null);

		//画布的旋转
		//第一个参数为旋转角度
		//第二个参数为基准点的x
		//第三个参数为基准点的y
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);


//		为Drawable指定边界矩形。这是可绘制对象在调用其draw（）方法时绘制的位置。
//		（int left,
//		int top,
//		int right,
//		int bottom）
		mBackShadowDrawable.setBounds(leftx, (int) mBezierStart1.y, rightx,
				(int) (mMaxLength + mBezierStart1.y));

//		根据可选效果绘制边界（通过setBounds设置），例如alpha（通过setAlpha设置）和滤色器（通过setColorFilter设置）
		mBackShadowDrawable.draw(canvas);
		canvas.restore();
	}

	//设置当前上面一页的位图和底下一页的位图
	public void setBitmaps(Bitmap bm1, Bitmap bm2) {
		mCurPageBitmap = bm1;
		mNextPageBitmap = bm2;
	}

	public void setScreen(int w, int h) {
		mWidth = w;
		mHeight = h;
	}

	@Override
	protected void onDraw(Canvas canvas) {

//		使用srcover porterduff模式用指定的颜色填充整个画布的位图（仅限于当前剪辑）。
//		在整个绘制区域统一涂上指定的颜色。
		canvas.drawColor(0xFFAAAAAA);

		calcPoints();

//		mPath0初始化为默认值
		drawCurrentPageArea(canvas,
				mCurPageBitmap, mPath0);

		//！！！
		drawNextPageAreaAndShadow(canvas,
				mNextPageBitmap);

		drawCurrentPageShadow(canvas);

		drawCurrentBackArea(canvas,
				mCurPageBitmap);
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 创建阴影的GradientDrawable
	 */
	private void createDrawable() {
		int[] color = { 0x333333, 0xb0333333 };
//		给定方向和渐变的颜色数组，创建一个新的渐变可绘制对象。

//		从右向左绘制渐变
		mFolderShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, color);
//		设置此可绘制对象使用的渐变类型
		mFolderShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

//		从左到右绘制渐变
		mFolderShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, color);
//		设置此可绘制对象使用的渐变类型
		mFolderShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowColors = new int[] { 0xff111111, 0x111111 };

		mBackShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
		mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
		mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);


		mFrontShadowColors = new int[] { 0x80111111, 0x111111 };

		mFrontShadowDrawableVLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
		mFrontShadowDrawableVLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableVRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
		mFrontShadowDrawableVRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHTB = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
		mFrontShadowDrawableHTB.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHBT = new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
		mFrontShadowDrawableHBT.setGradientType(GradientDrawable.LINEAR_GRADIENT);
	}




	/**
	 * Author : hmg25 Version: 1.0 Description : 绘制翻起页的阴影
	 */
	//	绘制翻起页上的阴影
	public void drawCurrentPageShadow(Canvas canvas) {
		double degree;
		if (mIsRTandLB) {
			degree = Math.PI
					/ 4
					- Math.atan2(mBezierControl1.y - mTouch.y, mTouch.x
							- mBezierControl1.x);
		} else {
			//45°减去一个角度
			degree = Math.PI
					/ 4
					- Math.atan2(mTouch.y - mBezierControl1.y, mTouch.x
							- mBezierControl1.x);
		}


		// 翻起页阴影顶点与touch点的距离
		double d1 = (float) 25 * 1.414 * Math.cos(degree);
		double d2 = (float) 25 * 1.414 * Math.sin(degree);
		float x = (float) (mTouch.x + d1);
		float y;
		if (mIsRTandLB) {
			y = (float) (mTouch.y + d2);
		} else {
			y = (float) (mTouch.y - d2);
		}

//		清除路径中的所有直线和曲线，使其为空。这不会更改填充类型设置。
		mPath1.reset();
//		将下一个轮廓的起点设置为点（x，y）。
		mPath1.moveTo(x, y);
//		从最后一个点到指定点（x，y）添加一条线。如果没有为此轮廓调用moveTo（），则第一个点将自动设置为（0,0）
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
		mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.close();
		float rotateDegrees;
		canvas.save();


//		使用指定的路径修改当前剪辑。
//		Region.Op.XOR:是全集形状减去交集形状之后的部分
//		Region.Op.INTERSECT:A和B交集的形状
		canvas.clipPath(mPath0, Region.Op.XOR);		//求出当前页的可见部分
		canvas.clipPath(mPath1, Region.Op.INTERSECT);	//
		int leftx;
		int rightx;
		GradientDrawable mCurrentPageShadow;
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl1.x);
			rightx = (int) mBezierControl1.x + 25;
			//垂直左到右？
			mCurrentPageShadow = mFrontShadowDrawableVLR;
		} else {
			leftx = (int) (mBezierControl1.x - 25);
			rightx = (int) mBezierControl1.x + 1;
			mCurrentPageShadow = mFrontShadowDrawableVRL;
		}

		rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x
				- mBezierControl1.x, mBezierControl1.y - mTouch.y));
		canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);

//		为Drawable指定边界矩形。
		mCurrentPageShadow.setBounds(leftx,
				(int) (mBezierControl1.y - mMaxLength), rightx,
				(int) (mBezierControl1.y));
//		根据可选效果绘制边界（通过setBounds设置），例如alpha（通过setAlpha设置）和滤色器（通过setColorFilter设置）。
		mCurrentPageShadow.draw(canvas);
		canvas.restore();


		//处理另一边
		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.close();
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl2.y);
			rightx = (int) (mBezierControl2.y + 25);
			//垂直上到下
			mCurrentPageShadow = mFrontShadowDrawableHTB;
		} else {
			leftx = (int) (mBezierControl2.y - 25);
			rightx = (int) (mBezierControl2.y + 1);
			mCurrentPageShadow = mFrontShadowDrawableHBT;
		}
		rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
				- mTouch.y, mBezierControl2.x - mTouch.x));
		canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
		float temp;
		if (mBezierControl2.y < 0)
			temp = mBezierControl2.y - mHeight;
		else
			temp = mBezierControl2.y;

		int hmg = (int) Math.hypot(mBezierControl2.x, temp);
		if (hmg > mMaxLength)
			mCurrentPageShadow
					.setBounds((int) (mBezierControl2.x - 25) - hmg, leftx,
							(int) (mBezierControl2.x + mMaxLength) - hmg,
							rightx);
		else
			mCurrentPageShadow.setBounds(
					(int) (mBezierControl2.x - mMaxLength), leftx,
					(int) (mBezierControl2.x), rightx);

		// Log.i("hmg", "mBezierControl2.x   " + mBezierControl2.x
		// + "  mBezierControl2.y  " + mBezierControl2.y);
		mCurrentPageShadow.draw(canvas);
		canvas.restore();
	}



	/**
	 * Author : hmg25 Version: 1.0 Description : 绘制翻起页背面
	 */
	private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
		int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
		float f1 = Math.abs(i - mBezierControl1.x);
		int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
		float f2 = Math.abs(i1 - mBezierControl2.y);
		float f3 = Math.min(f1, f2);
		mPath1.reset();
		mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath1.close();
		GradientDrawable mFolderShadowDrawable;
		int left;
		int right;
		if (mIsRTandLB) {
			left = (int) (mBezierStart1.x - 1);
			right = (int) (mBezierStart1.x + f3 + 1);
			mFolderShadowDrawable = mFolderShadowDrawableLR;
		} else {
			left = (int) (mBezierStart1.x - f3 - 1);
			right = (int) (mBezierStart1.x + 1);
			mFolderShadowDrawable = mFolderShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);

		mPaint.setColorFilter(mColorMatrixFilter);

		//eh的长度
		float dis = (float) Math.hypot(mCornerX - mBezierControl1.x,
				mBezierControl2.y - mCornerY);

		//cos(∠hef)
		float f8 = (mCornerX - mBezierControl1.x) / dis;

		//sin(∠hef)
		float f9 = (mBezierControl2.y - mCornerY) / dis;

		mMatrixArray[0] = 1 - 2 * f9 * f9;
		mMatrixArray[1] = 2 * f8 * f9;
		mMatrixArray[3] = mMatrixArray[1];
		mMatrixArray[4] = 1 - 2 * f8 * f8;
		mMatrix.reset();
//		将数组中的9个值复制到矩阵中。根据Matrix的实现，
//		可以将它们转换为Matrix中的16.16整数，
//		这样，随后对getValues（）的调用将不会产生完全相同的值。
		mMatrix.setValues(mMatrixArray);
//		用指定的变换转换预矩阵。M'= M * T（dx，dy）
		mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
//		用指定的变换转换后矩阵。M'= T（dx，dy）* M
		mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
		canvas.drawBitmap(bitmap, mMatrix, mPaint);
		// canvas.drawBitmap(bitmap, mMatrix, null);
		mPaint.setColorFilter(null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		mFolderShadowDrawable.setBounds(left, (int) mBezierStart1.y, right,
				(int) (mBezierStart1.y + mMaxLength));
		mFolderShadowDrawable.draw(canvas);
		canvas.restore();
	}


	//重写的方法
	//	computeScroll的作用是计算ViewGroup如何滑动。而computeScroll是通过draw来调用的。
	public void computeScroll() {
		super.computeScroll();

//		计算有没有终止，需要通过mScroller.computeScrollOffset()
//		判断移动过程是否完成
		if (mScroller.computeScrollOffset()) {
			float x = mScroller.getCurrX();
			float y = mScroller.getCurrY();
			mTouch.x = x;
			mTouch.y = y;
			postInvalidate();
		}
	}

	//开始动画
	private void startAnimation(int delayMillis) {
		int dx, dy;
		// dx 水平方向滑动的距离，负值会使滚动向左滚动
		// dy 垂直方向滑动的距离，负值会使滚动向上滚动
		if (mCornerX > 0) {
			dx = -(int) (mWidth + mTouch.x);
		} else {
			dx = (int) (mWidth - mTouch.x + mWidth);
		}
		if (mCornerY > 0) {
			dy = (int) (mHeight - mTouch.y);
		} else {
			dy = (int) (1 - mTouch.y); // 防止mTouch.y最终变为0
		}

//		通过提供起点，行进距离和滚动持续时间来开始滚动。
//		startX	Int：开始的水平滚动偏移量（以像素为单位）。正数会将内容向左滚动。
//		startY	Int：开始的垂直滚动偏移量（以像素为单位）。正数将使内容向上滚动。
//		dx	Int：行进的水平距离。正数会将内容向左滚动。
//		dy	Int：行进的垂直距离。正数将使内容向上滚动。
//		duration	Int：滚动持续时间（以毫秒为单位）。
		mScroller.startScroll((int) mTouch.x, (int) mTouch.y, dx, dy,
				delayMillis);
	}

	//终止动画
	public void abortAnimation() {
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}
	}


	//判断能否自动翻页
	//	从右边翻向左边
	public boolean canDragOver() {
		if (mTouchToCornerDis > mWidth / 10)
			return true;
		return false;
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 是否从左边翻向右边
	 */
	//和上面的函数作用不一样
	public boolean DragToRight() {
		if (mCornerX > 0)
			return false;
		return true;
	}

}
