
package createCup.mJudge.judgetrafficlight;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;

public class MyVibrate {

    private Vibrator vibrator;
    private long [] pattern = new long[4];
    
    public MyVibrate(Activity activity){
    	
    	vibrator = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);
    	
    	for(int i = 0; i < 4; i++){
    		
    		if(i % 2 == 0)
    			pattern[i] = 100;
    		else
    			pattern[i] = 400;
    	}
    }
    
    public void onVibrate(){
    	vibrator.vibrate(pattern, -1);
    }
    
    public void onStop(){  
    	
        vibrator.cancel();  
    }   
}
