package com.example.map_navigation;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AndroidTTS extends Activity implements OnInitListener {
    /** Called when the activity is first created. */
	private String inputText = null;
	private Button speakBtn = null;
    private static final int REQ_TTS_STATUS_CHECK = 0;
    private static final String TAG = "TTS Demo";
	private TextToSpeech mTts;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
      //���TTS����Ƿ��Ѿ���װ���ҿ���
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
        
        //inputText = (EditText)findViewById(R.id.inputText);
        //speakBtn = (Button)findViewById(R.id.speakBtn);        
        speakBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mTts.speak(inputText, TextToSpeech.QUEUE_ADD, null);
				//�ʶ�������������
			}
			});
    }
    
    public void setText(String text){
    	inputText = text;
    	mTts.speak(inputText, TextToSpeech.QUEUE_ADD, null);
    }
    
  //ʵ��TTS��ʼ���ӿ�
	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		//TTS Engine��ʼ�����
		if(status == TextToSpeech.SUCCESS)
		{
			int result = mTts.setLanguage(Locale.CHINESE);
			//���÷�������
			if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
			//�ж������Ƿ����
			{
				Log.v(TAG, "Language is not available");
				speakBtn.setEnabled(true);
			}
			else
			{
				mTts.speak("你好", TextToSpeech.QUEUE_ADD, null);
				speakBtn.setEnabled(true);
			}
		}
		
	}
	
	protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQ_TTS_STATUS_CHECK)
		{
			switch (resultCode) {
			case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
				//������ؽ�����TTS Engine������
			{
				mTts = new TextToSpeech(this, this);
				Log.v(TAG, "TTS Engine is installed!");
				
			}
				
				break;
			case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
				//��Ҫ�������������
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
				//ȱ����Ҫ���Ե��������
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
				//ȱ����Ҫ���Եķ������
			{
				//�������������������д�,�������ذ�װ��Ҫ�����
				Log.v(TAG, "Need language stuff:"+resultCode);
				Intent dataIntent = new Intent();
				dataIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(dataIntent);
				
			}
				break;
			case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
				//���ʧ��
			default:
				Log.v(TAG, "Got a failure. TTS apparently not available");
				break;
			}
		}
		else
		{
			//����Intent���صĽ��
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(mTts != null)
			//activity��ͣʱҲֹͣTTS
		{
			mTts.stop();
		}
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//�ͷ�TTS����Դ
		mTts.shutdown();
	}
}