package pcapture;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.List;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;


// 大枠はVideoライブラリのCapture.javaをベースとし、
// ビデオキャプチャの処理にsarxos氏のwebcam-captureを使用。
// https://github.com/sarxos/webcam-capture
// https://github.com/processing/processing-video/blob/master/src/processing/video/Capture.java

// 現在の仕様
// ・frameRate()メソッドの中身は未実装。というより、webcam-captureではフレームレート設定できないっぽい。
// ・インスタンス作成時に指定した画像サイズとキャプチャ側で取得可能な画像サイズが異なるとき、
//   自動的に取得可能な画像サイズに変更になる（メッセージも表示される）

public class PCapture extends PImage implements PConstants, WebcamListener {

	public int sourceWidth = 0;
	public int sourceHeight = 0;
	public float sourceFrameRate = -1;
	public float frameRate = -1;

	protected int[] copyPixels = null;
	protected String device;  // device name

	protected boolean capturing = false;
	protected boolean available = false;
	protected boolean firstFrame = true;
	protected boolean outdatedPixels = true;

	protected Method captureEventMethod;
	protected Object eventHandler;

	protected Webcam webcam;
	protected WebcamListener webcamlistener;


	public PCapture(PApplet parent) {
		this(parent, 640, 480, null, 0);
	}

	public PCapture(PApplet parent, String device) {
		this(parent, 640, 480, device, 0);
	}

	public PCapture(PApplet parent, int width, int height) {
		this(parent, width, height, null, 0);
	}

	public PCapture(PApplet parent, int width, int height, float fps) {
		this(parent, width, height, null, fps);
	}

	public PCapture(PApplet parent, int width, int height, String device) {
		this(parent, width, height, device, 0);
	}

	public PCapture(PApplet parent, int width, int height, String device, float fps) {
		// super(width, height, RGB);
		super(width, height, ARGB);
		this.device = device;
		this.frameRate = fps;
		initGStreamer(parent);
	}

	public void frameRate(float ifps) {
		System.err.println("frameRate() is not supported.");
	}

	public boolean available() {
		return available;
	}

	public boolean isCapturing() {
		return capturing;
	}

	public void start() {
		webcam.open(true);  // trueでasync
		Dimension actual_size = webcam.getViewSize();
		sourceWidth = actual_size.width;
		sourceHeight = actual_size.height;
		capturing = true;
	}

	public void stop() {
		capturing = false;
		webcam.close();
	}

	public synchronized void post() {
		// drawの後に呼ばれる処理
	}

	public void dispose() {
		pixels = null;
		parent.g.removeCache(this);
		parent.unregisterMethod("dispose", this);
		parent.unregisterMethod("post", this);
	}

	protected void finalize() throws Throwable {
		try {
			dispose();
		}
		finally {
			super.finalize();
		}
	}

	public synchronized void read() {
		if ( !capturing ) return;

		if (firstFrame) {
			super.init(sourceWidth, sourceHeight, RGB, 1);
			firstFrame = false;
		}
		if (copyPixels == null) {
			copyPixels = new int[sourceWidth * sourceHeight];
		}
		if ( webcam.isOpen() ) {
			BufferedImage bImg = webcam.getImage();
			for (int y=0; y<this.height; y++ ) {
				for (int x=0; x<this.width; x++ ) {
					copyPixels[y * this.width + x] = bImg.getRGB(x, y);
				}
			}
			int[] temp = pixels;
			pixels = copyPixels;
			updatePixels();
			copyPixels = temp;
		}
		available = false;
		// newFrame = true;
	}

	static public String[] list() {
		List<Webcam> webcams = Webcam.getWebcams();
		String[] out = new String [webcams.size()];
		for (int i=0; i<out.length; i++) {
			out[i] = trimNumber(webcams.get(i).getName());
		}
		return out;
	}

	// sarxos/webcam-captureではデバイス名の後部に番号が付くのでそれを削除
	static public String trimNumber(String str) {
		str = str.trim();
		int i = str.lastIndexOf(" ");
		return str.substring(0, i);
	}

	//////////////////////////////////////////////////////////////
	// for sarxos's Webcam-capture

	@Override
	public void webcamOpen(WebcamEvent we) {
	}

	@Override
	public void webcamClosed(WebcamEvent we) {
	}

	@Override
	public void webcamDisposed(WebcamEvent we) {
	}

	@Override
	public void webcamImageObtained(WebcamEvent we) {
		available = true;
		frameRate = sourceFrameRate = (float)webcam.getFPS();
		if ( capturing ) {
			fireCaptureEvent();
		}
	}

	/////////////////////////////////////////////////////////////
	// for PImage

	@Override
	public synchronized void loadPixels() {
		super.loadPixels();
		if ( true ) {
			outdatedPixels = false;
		}
	}

	@Override
	public int get(int x, int y) {
		if (outdatedPixels) loadPixels();
		return super.get(x, y);
	}

	protected void getImpl(int sourceX, int sourceY,
			int sourceWidth, int sourceHeight,
			PImage target, int targetX, int targetY) {
		if (outdatedPixels) loadPixels();
		super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
				target, targetX, targetY);
	}


	////////////////////////////////////////////////////////////
	// Initialization methods.

	protected void initGStreamer(PApplet parent) {
		this.parent = parent;
		String[] devices = list();

		if (devices == null || devices.length <= 0 ) {
			throw new IllegalStateException("Could not find any devices");
		}

		int dev_index = -1;
		if ( device == null ) {
			device = devices[0];
			dev_index = 0;
		} else {
			for (int i=0; i<devices.length; i++) {
				if ( devices[i].equals(device) ) {
					dev_index = i;
				}
			}
		}
		if ( dev_index < 0 ) {
			throw new IllegalStateException("Could not find the device named \"" + device + "\"");
		}
		//println("dev_index = " + dev_index );
		//println("device = " + device);

		webcam = Webcam.getWebcams().get(dev_index);
		Dimension size = new Dimension( this.width, this.height );
		webcam.setCustomViewSizes( new Dimension[]{size} );
		webcam.setViewSize(size);
		webcam.addWebcamListener(this);

		try {
			// Register methods
			parent.registerMethod("dispose", this);
			parent.registerMethod("post", this);
			setEventHandlerObject(parent);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setEventHandlerObject(Object obj) {
		eventHandler = obj;
		try {
			captureEventMethod = eventHandler.getClass().getMethod("captureEvent", PCapture.class);
			return;
		}
		catch (Exception e) {
			// no such method, or an error... which is fine, just ignore
		}
		// captureEvent can alternatively be defined as receiving an Object, to allow
		// Processing mode implementors to support the video library without linking
		// to it at build-time.
		try {
			captureEventMethod = eventHandler.getClass().getMethod("captureEvent", Object.class);
		}
		catch (Exception e) {
			// no such method, or an error... which is fine, just ignore
		}
	}

	private void fireCaptureEvent() {
		if (captureEventMethod != null) {
			try {
				captureEventMethod.invoke(eventHandler, this);
			}
			catch (Exception e) {
				System.err.println("error, disabling captureEvent()");
				e.printStackTrace();
				captureEventMethod = null;
			}
		}
	}

}