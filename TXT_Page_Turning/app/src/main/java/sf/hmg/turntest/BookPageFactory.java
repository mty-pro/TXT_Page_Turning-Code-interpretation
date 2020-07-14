/**
 *  Author :  hmg25
 *  Description :
 */
package sf.hmg.turntest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;

public class BookPageFactory {

	private File book_file = null;		//文件所在的路径
	private MappedByteBuffer m_mbBuf = null;	//基于MappedByteBuffer操作大文件
	private int m_mbBufLen = 0;					//缓冲区长度
	private int m_mbBufBegin = 0;				//缓冲区开始
	private int m_mbBufEnd = 0;					//缓冲区结束
	private String m_strCharsetName = "GBK";	//编码方式
	private Bitmap m_book_bg = null;	//背景
	private int mWidth;					//宽
	private int mHeight;				//高

	private Vector<String> m_lines = new Vector<String>();		//存放行的数组？

	private int m_fontSize = 24;	//字号
	private int m_textColor = Color.BLACK;	//字色
	private int m_backColor = 0xffff9e85; // 背景颜色
	private int marginWidth = 15; // 左右与边缘的距离
	private int marginHeight = 20; // 上下与边缘的距离

	private int mLineCount; // 每页可以显示的行数
	private float mVisibleHeight; // 绘制内容的宽
	private float mVisibleWidth; // 绘制内容的宽
	private boolean m_isfirstPage,m_islastPage;	//第一页还是最后一页

	// private int m_nLineSpaceing = 5;

	private Paint mPaint;			//画笔

	public BookPageFactory(int w, int h) {		//构造函数
		// TODO Auto-generated constructor stub
		mWidth = w;
		mHeight = h;
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);		//抗锯齿
		mPaint.setTextAlign(Align.LEFT);				//左对齐
		mPaint.setTextSize(m_fontSize);					//设置字号
		mPaint.setColor(m_textColor);					//设置字体颜色
		mVisibleWidth = mWidth - marginWidth * 2;		//绘制的宽
		mVisibleHeight = mHeight - marginHeight * 2;	//绘制的高
		mLineCount = (int) (mVisibleHeight / m_fontSize); // 可显示的行数
	}

	public void openbook(String strFilePath) throws IOException {
		book_file = new File(strFilePath);
		long lLen = book_file.length();		//返回由此抽象路径名表示的文件的长度
		m_mbBufLen = (int) lLen;

//		RandomAccessFile(book_file, "r")：创建从中读取和向其中写入（可选）的随机访问文件流，该文件由 File 参数指定
//		getChannel()：返回与此文件关联的唯一 FileChannel 对象
//		map(FileChannel.MapMode.READ_ONLY, 0, lLen)：将此通道的文件区域直接映射到内存中。
//		参数：
//		mode - 根据是按只读、读取/写入或专用（写入时拷贝）来映射文件，分别为 FileChannel.MapMode 类中所定义的 READ_ONLY、READ_WRITE 或 PRIVATE 之一
//		position - 文件中的位置，映射区域从此位置开始；必须为非负数
//		size - 要映射的区域大小；必须为非负数且不大于 Integer.MAX_VALUE
//		返回MappedByteBuffer ：直接字节缓冲区，其内容是文件的内存映射区域。


		m_mbBuf = new RandomAccessFile(book_file, "r").getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, lLen);
	}


	// 读取上一段落？
	protected byte[] readParagraphBack(int nFromPos) {
		int nEnd = nFromPos;
		int i;
		byte b0, b1;
		if (m_strCharsetName.equals("UTF-16LE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				b1 = m_mbBuf.get(i + 1);
				if (b0 == 0x0a && b1 == 0x00 && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}

		} else if (m_strCharsetName.equals("UTF-16BE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				b1 = m_mbBuf.get(i + 1);
				if (b0 == 0x00 && b1 == 0x0a && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}
		} else {
			i = nEnd - 1;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				if (b0 == 0x0a && i != nEnd - 1) {		//如果b0为换行并且i不为第一个字符
					i++;								//i之前为换行，++之后就是下一段的第一个字符
					break;
			}
			i--;
			}
		}
		if (i < 0)
			i = 0;
		int nParaSize = nEnd - i;						//得到段落大小
		int j;
		byte[] buf = new byte[nParaSize];
		for (j = 0; j < nParaSize; j++) {
			buf[j] = m_mbBuf.get(i + j);
		}
		return buf;
	}


	// 读取下一段落？
	protected byte[] readParagraphForward(int nFromPos) {
		int nStart = nFromPos;
		int i = nStart;
		byte b0, b1;
		// 根据编码格式判断换行
		if (m_strCharsetName.equals("UTF-16LE")) {
			while (i < m_mbBufLen - 1) {
				b0 = m_mbBuf.get(i++);
				b1 = m_mbBuf.get(i++);
				if (b0 == 0x0a && b1 == 0x00) {
					break;
				}
			}
		} else if (m_strCharsetName.equals("UTF-16BE")) {
			while (i < m_mbBufLen - 1) {
				b0 = m_mbBuf.get(i++);
				b1 = m_mbBuf.get(i++);
				if (b0 == 0x00 && b1 == 0x0a) {
					break;
				}
			}
		} else {
			while (i < m_mbBufLen) {
				b0 = m_mbBuf.get(i++);
				if (b0 == 0x0a) {
					break;
				}
			}
		}
		int nParaSize = i - nStart;
		byte[] buf = new byte[nParaSize];
		for (i = 0; i < nParaSize; i++) {
			buf[i] = m_mbBuf.get(nFromPos + i);
		}
		return buf;
	}


	protected Vector<String> pageDown() {
		String strParagraph = "";
		Vector<String> lines = new Vector<String>();
		while (lines.size() < mLineCount && m_mbBufEnd < m_mbBufLen) {
			byte[] paraBuf = readParagraphForward(m_mbBufEnd); // 读取一个段落
			m_mbBufEnd += paraBuf.length;
			try {
				strParagraph = new String(paraBuf, m_strCharsetName);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String strReturn = "";
			if (strParagraph.indexOf("\r\n") != -1) {		//回车换行
				strReturn = "\r\n";
				strParagraph = strParagraph.replaceAll("\r\n", "");		//正则表达式   字符
			} else if (strParagraph.indexOf("\n") != -1) {
				strReturn = "\n";
				strParagraph = strParagraph.replaceAll("\n", "");
			}

			if (strParagraph.length() == 0) {			//段落长度为0添加到vector数组中
				lines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				//计算制定长度的字符串（字符长度、字符个数、显示的时候真实的长度）
				//measureForwards为true,从头开始测否则从尾向前测
				//maxWidth 最大的宽度
				//measuredWidth实测长度
				int nSize = mPaint.breakText(strParagraph, true, mVisibleWidth,
						null);
				lines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);
				if (lines.size() >= mLineCount) {
					break;
				}
			}
			if (strParagraph.length() != 0) {
				try {
					m_mbBufEnd -= (strParagraph + strReturn)
							.getBytes(m_strCharsetName).length;
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return lines;
	}

	protected void pageUp() {
		if (m_mbBufBegin < 0)
			m_mbBufBegin = 0;
		Vector<String> lines = new Vector<String>();
		String strParagraph = "";
		while (lines.size() < mLineCount && m_mbBufBegin > 0) {
			Vector<String> paraLines = new Vector<String>();
			byte[] paraBuf =
					readParagraphBack(m_mbBufBegin);
			m_mbBufBegin -= paraBuf.length;
			try {
				strParagraph = new String(paraBuf, m_strCharsetName);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			strParagraph = strParagraph.replaceAll("\r\n", "");
			strParagraph = strParagraph.replaceAll("\n", "");

			if (strParagraph.length() == 0) {
				paraLines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				int nSize = mPaint.breakText(strParagraph, true, mVisibleWidth,
						null);
				paraLines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);
			}
			lines.addAll(0, paraLines);
		}
		while (lines.size() > mLineCount) {
			try {
				m_mbBufBegin += lines.get(0).getBytes(m_strCharsetName).length;
				lines.remove(0);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		m_mbBufEnd = m_mbBufBegin;
		return;
	}

	protected void prePage() throws IOException {
		if (m_mbBufBegin <= 0) {
			m_mbBufBegin = 0;
			m_isfirstPage=true;
			return;
		}else
			m_isfirstPage=false;
		m_lines.clear();
		pageUp();
		m_lines = pageDown();
	}

	public void nextPage() throws IOException {
		if (m_mbBufEnd >= m_mbBufLen) {
			m_islastPage=true;
			return;
		}else
			m_islastPage=false;
		m_lines.clear();
		m_mbBufBegin = m_mbBufEnd;
		m_lines = pageDown();
	}

	public void onDraw(Canvas c) {
		if (m_lines.size() == 0)
			m_lines = pageDown();
		if (m_lines.size() > 0) {
			if (m_book_bg == null)
				c.drawColor(m_backColor);
			else
				c.drawBitmap(m_book_bg, 0, 0, null);
			int y = marginHeight;
			for (String strLine : m_lines) {
				y += m_fontSize;


//			    text:要绘制的文字
//				x：绘制原点x坐标
//				y：绘制原点y坐标
//				paint:用来做画的画笔
				c.drawText(strLine, marginWidth, y, mPaint);
			}
		}
		float fPercent = (float) (m_mbBufBegin * 1.0 / m_mbBufLen);
		//计算阅读百分比
		/*
		以"#"补位
		整数部分多了:不会截断,但是排在有效位最前面的0会被删除
		整数部分少了:不作处理
		小数部分多了:截断,建议指定RoundingMode,默认为RoundingMode.HALF_EVEN
		小数部分少了:不作处理
		以"0"补位
		整数部分多了:不会截断,排在有效位前面的0也不会被删除
		整数部分少了:补0
		小数部分多了:截断,建议指定RoundingMode,默认为RoundingMode.HALF_EVEN
		小数部分少了:补0
		*/
		DecimalFormat df = new DecimalFormat("#0.0");
		String strPercent = df.format(fPercent * 100) + "%";
		int nPercentWidth = (int) mPaint.measureText("999.9%") + 1;
		c.drawText(strPercent, mWidth - nPercentWidth, mHeight - 5, mPaint);
	}

	public void setBgBitmap(Bitmap BG) {
		m_book_bg = BG;
	}
	
	public boolean isfirstPage() {
		return m_isfirstPage;
	}
	public boolean islastPage() {
		return m_islastPage;
	}
}
