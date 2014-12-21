/*
 * 		Author :  Bicycle Knight
 * 		Function : Judge traffic light and tell people the statement of traffic light
 * 		Last edition time : 3/22/2013
 */

package createCup.mJudge.judgetrafficlight;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.example.map_navigation.R;

public class JudgeTrafficLight extends Activity implements CvCameraViewListener {
	private static final String TAG = "JTlight::Activity";

	private Mat mRgba;
	private Mat mGrayMat;
	private Mat mHsvMat;
	private Mat circles;
	private JavaCameraView mOpenCvCameraView;
	//private MediaPlayer warnRedLight;
	//private MediaPlayer tellGreenLight;

	private MyVibrate myVibrate;
	// private MySurfaceView mySurfaceView;

	private byte row, column;

	private double getHSV[] = new double[3];

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public JudgeTrafficLight() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_judge_traffic_light);

		mOpenCvCameraView = (JavaCameraView) findViewById(R.id.javaCameraView1);
		mOpenCvCameraView.setCvCameraViewListener(this);

		new Scalar(255, 0, 0, 255);
		//warnRedLight = MediaPlayer.create(this, R.raw.red2);
		//tellGreenLight = MediaPlayer.create(this, R.raw.green2);

		// mySurfaceView = new MySurfaceView(this);
		myVibrate = new MyVibrate(this);

	}


	@Override
	public void onPause() {
		mOpenCvCameraView.disableView();
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mGrayMat = new Mat(height, width, CvType.CV_8UC1);
		mHsvMat = new Mat(height, width, CvType.CV_8UC3);
	}

	public void onCameraViewStopped() {
		mRgba.release();
		mGrayMat.release();
	}

	// h /* 0~360 degrees */
	// s /* 0 ~ 1.0 */
	// v /* 0 ~ 1.0 */
	Scalar HSVtoRGB(double h, double s, double v) {
		double f, p, q, t;
		if (s == 0) { // achromatic (grey)
			return new Scalar(v, v, v);
		}
		h /= 60; // sector 0 to 5
		int i = (int) Math.floor(h);
		f = h - i; // factorial part of h
		p = v * (1 - s);
		q = v * (1 - s * f);
		t = v * (1 - s * (1 - f));
		switch (i) {
		case 0:
			return new Scalar(v, t, p);
		case 1:
			return new Scalar(q, v, p);
		case 2:
			return new Scalar(p, v, t);
		case 3:
			return new Scalar(p, q, v);
		case 4:
			return new Scalar(t, p, v);
		default: // case 5:
			return new Scalar(v, p, q);
		}
	}

	public Mat onCameraFrame(Mat inputFrame) {
		circles = new Mat();
		inputFrame.copyTo(mRgba);
		Imgproc.cvtColor(mRgba, mGrayMat, Imgproc.COLOR_RGBA2GRAY);
		Imgproc.cvtColor(mRgba, mHsvMat, Imgproc.COLOR_RGB2HSV);

		Imgproc.HoughCircles(mGrayMat, circles, Imgproc.CV_HOUGH_GRADIENT, 1d,
				(double) mGrayMat.height() / 4, 100, 50, 2, 100);

		if (circles.cols() > 0) {
			for (int x = 0; x < circles.cols(); x++) {
				double vCircle[] = circles.get(0, x);

				if (vCircle == null)
					break;

				row = (byte) vCircle[0];
				column = (byte) vCircle[1];
				double getOneHSV[] = new double[3];
				for (int i = 0; i < 3; i++)
					getHSV[i] = 0;
				int total = 0;
				for (byte r = (byte) (Math.max(0, row - 3)); r <= row; r++) {
					for (byte c = (byte) (Math.max(0, column - 3)); c <= column; c++) {
						getOneHSV = mHsvMat.get(r, c);
						for (int i = 0; i < 3; i++)
							getHSV[i] += getOneHSV[i];
						total++;
					}
				}
				for (int i = 0; i < 3; i++)
					getHSV[i] /= total;
				Scalar getRGB = HSVtoRGB(getHSV[0], getHSV[1], getHSV[2]);
				//Core.putText(mRgba, "HSV:" + getHSV[0] + "," + getHSV[1] + ","
						//+ getHSV[2], new Point(100, 100*(x+1)), 3, 1, new Scalar(255,
						//255, 255), 3);

				if ((getHSV[0] >= 0 && getHSV[0] <= 10)
						|| (getHSV[0] >= 350 && getHSV[0] <= 360)) {
					// mySurfaceView.getJudge(1);
					// mySurfaceView.onPrepared(warnRedLight);
					// mySurfaceView.onCompletion(warnRedLight);
					myVibrate.onVibrate();
				} else if ((getHSV[0] >= 110 && getHSV[0] <= 130)) {
					// mySurfaceView.getJudge(0);
					// mySurfaceView.onPrepared(tellGreenLight);
					// mySurfaceView.onPrepared(tellGreenLight);
				}

				// draw the found circle
				Core.circle(
						mRgba,
						new Point(Math.round(vCircle[0]), Math
								.round(vCircle[1])), (int) Math
								.round(vCircle[2]), getRGB);
				Core.circle(
						mRgba,
						new Point(Math.round(vCircle[0]), Math
								.round(vCircle[1])), 3, getRGB);

			}
		} else {
			//warnRedLight.stop();
			//tellGreenLight.stop();
			myVibrate.onStop();
			// Core.putText(mRgba, String.valueOf("0"), new Point(100, 100),
			// 3, 1, new Scalar(255, 0, 0, 255), 2);
		}
		return mRgba;
	}
}
