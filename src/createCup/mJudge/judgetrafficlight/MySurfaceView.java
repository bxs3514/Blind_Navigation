
package createCup.mJudge.judgetrafficlight;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

public class MySurfaceView extends SurfaceView implements Callback, OnBufferingUpdateListener, 
	OnCompletionListener, OnPreparedListener{

	private SurfaceView surfaceView;  
	private SurfaceHolder surfaceHolder;  
	private MediaPlayer mediaPlayer;  
	private Context context;
	 
	@SuppressWarnings("deprecation")
	public MySurfaceView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		this.context = context;
		this.surfaceView = new SurfaceView(context);
		this.surfaceHolder = this.surfaceView.getHolder();  
        this.surfaceHolder.addCallback(this);  
        
        this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); 
        this.mediaPlayer = new MediaPlayer();

		//MediaPlayer.create(context, R.raw.a);
	}
	
	public void getJudge(int Judge)
	{
	}
	 
	public void imstop(){
		mediaPlayer.stop();
	}
	
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		//if(Judge == 1)
			//MediaPlayer.create(context, R.raw.red2);
		//else
			//MediaPlayer.create(context, R.raw.green2);
		
		mp.setVolume(16f, 16f);
	}
	
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		try {
        	if (mp != null) mp.stop();
        	mp.prepare();
        	mp.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

}
