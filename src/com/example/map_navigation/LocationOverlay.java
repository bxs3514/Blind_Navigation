package com.example.map_navigation;

import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.RouteOverlay;
import com.baidu.mapapi.map.TransitOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKLine;
import com.baidu.mapapi.search.MKPlanNode;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKRoute;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKStep;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRoutePlan;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import createCup.mJudge.judgetrafficlight.JudgeTrafficLight;
import createCup.mJudge.judgetrafficlight.MySurfaceView;

public class LocationOverlay extends Activity implements OnInitListener,OnGestureListener{
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	public static final int WALK_MENU_ID = Menu.FIRST;
	public static final int TRAN_MENU_ID = Menu.FIRST + 1;
	
	private GestureDetector gestureScanner = new GestureDetector(this);
	
	private ArrayList<String> getResults = null;
	
	static private MapView mMapView;
	public MKMapViewListener mMapListener;
	private LocationData locData;
	private MyLocationOverlay myLocationOverlay;
	private LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
    public NotifyLister mNotifyer;
    
    private MKPlanNode stNode;
    private MKPlanNode enNode;
    private MKSearch mSearch;
	private GeoPoint pt;
	public MKStep step;
	public MKLine line;
	public MKRoute route;
	//public MKTransitRoutePlan tRoute;
	
	private TextToSpeech mTts; // TTS对象
	
	private MySurfaceView mySurfaceView;
	private MediaPlayer startSpeaking;
	
	private SmsManager smsManager;
	
	private boolean isSetting = false; // 进入设置标记
	private boolean refreshMap = true;
	private boolean ifSpeakDestination = false;
	private boolean isLongPress;
	private boolean isTurn = false;
	private boolean isDesChange = false;
	private boolean isLongAfter = false;
	protected String textTitle; // 文本标题
	protected String textContent; // 文本内容
	private String roadInfo;
	private String distanceKm = null;
	private String afterDistance = null;
	private String resultString = null;
	private String emergencyNum = "18810660614";
	private long time;
	private long turnTime;
	private int ifChoose = 0;
	private int walkOrTran = 0;
	
	private static final int REQ_TTS_STATUS_CHECK = 0;
	
	private TextView routeMessage;
	private TextView distanceMessage;
    private TextView destination;
    private TextView turn;
    private TextView linkman;
	private Button confirm;
	int index =0;
		
	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
            Toast.makeText(LocationOverlay.this, "msg:" +msg.what, Toast.LENGTH_SHORT).show();
        };
    };
    
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_locationoverlay);
        
        //检查TTS数据是否已经安装并且可用
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        mTts = new TextToSpeech(this, this);
        
        //startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
        mySurfaceView = new MySurfaceView(this);
        startSpeaking = MediaPlayer.create(this, R.raw.green);
        
        MapInit();
        ButtonInit();
    }
    
    private void ButtonInit(){
    	linkman = (TextView)findViewById(R.id.linkman);
    	confirm = (Button)findViewById(R.id.confirm);
    	confirm.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				emergencyNum = linkman.getText().toString();
			}
    		
    	});
    }
    
    private void MapInit(){
    	
    	DemoApplication app = (DemoApplication)this.getApplication();
		if (app.mBMapManager == null) {
			app.mBMapManager = new BMapManager(this);
			app.mBMapManager.init(DemoApplication.strKey,new DemoApplication.MyGeneralListener());
		}
        
        mMapView = (MapView)findViewById(R.id.bmapView);
        mMapView.getController();
        initMapView();
        
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        
        
        mNotifyer = new NotifyLister();
        mNotifyer.SetNotifyLocation(42.03249652949337,113.3129895882556,3000,"bd09ll");
        mLocClient.registerNotify(mNotifyer);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();
        mMapView.getController().setZoom(14);
        mMapView.getController().enableClick(true);
        
        routeMessage = (TextView)findViewById(R.id.routeInformation);
        distanceMessage = (TextView)findViewById(R.id.distance);
        
        mMapView.displayZoomControls(true);
        mMapListener = new MKMapViewListener() {
			
			@Override
			public void onMapMoveFinish() {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onClickMapPoi(MapPoi mapPoiInfo) {
				// TODO Auto-generated method stub
				String title = "";
				if (mapPoiInfo != null){
					title = mapPoiInfo.strText;
					Toast.makeText(LocationOverlay.this,title,Toast.LENGTH_SHORT).show();
				}
			}
			
		};
		mMapView.regMapViewListener(DemoApplication.getInstance().mBMapManager, mMapListener);
		myLocationOverlay = new MyLocationOverlay(mMapView);
		locData = new LocationData();
	    myLocationOverlay.setData(locData);
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		mMapView.refresh();
		
		MKSearchListener mKSearchListener = new MKSearchListener(){

        	@Override
            public void onGetPoiDetailSearchResult(int type, int error) {
            }
            
			public void onGetDrivingRouteResult(MKDrivingRouteResult res,
					int error) {
				if (error != 0 || res == null) {
					//Toast.makeText(LocationOverlay.this, "锟斤拷歉锟斤拷未锟揭碉拷锟斤拷锟�, Toast.LENGTH_SHORT).show();
					return;
				}
				RouteOverlay routeOverlay = new RouteOverlay(LocationOverlay.this, mMapView);
			    routeOverlay.setData(res.getPlan(0).getRoute(0));
			    mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.refresh();
			    mMapView.getController().animateTo(res.getStart().pt);
			}

			public void onGetTransitRouteResult(MKTransitRouteResult res,
					int error) {
				if (error != 0 || res == null) {
					//Toast.makeText(LocationOverlay.this, "锟斤拷歉锟斤拷未锟揭碉拷锟斤拷锟�, Toast.LENGTH_SHORT).show();
					return;
				}
				TransitOverlay  routeOverlay = new TransitOverlay (LocationOverlay.this, mMapView);
				routeOverlay.setData(res.getPlan(0));
				line = res.getPlan(0).getLine(0);
			    //route = res.getPlan(0).getRoute(0);
			    
                int distanceM = line.getDistance();
                distanceKm = String.valueOf(distanceM / 1000) +"."+String.valueOf(distanceM % 1000);
                
                //step = line.getTitle();
                //System.out.println("!!!"+res.getPlan(0).getNumLines());
                routeMessage.setText(line.getGetOnStop().name + line.getTip());
                
                roadInfo = line.getTip();
                                
                distanceMessage.setText("");
			    
                //routeOverlay.setData(route);
                
                //routeOverlay.setData(tRoute);
                if(refreshMap == true){
                	mMapView.getOverlays().clear();
    			    mMapView.getOverlays().add(routeOverlay);
    			    mMapView.getController().animateTo(pt);
    			    mMapView.refresh();
    			    mMapView.getController().animateTo(res.getStart().pt);
    			    refreshMap = false;
                }
                
			    /*mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.getController().animateTo(pt);
			    mMapView.refresh();
			    mMapView.getController().animateTo(res.getStart().pt);*/
			}

			public void onGetWalkingRouteResult(MKWalkingRouteResult res, int error) {
				if (error != 0 || res == null) {
					//Toast.makeText(LocationOverlay.this, "锟斤拷歉锟斤拷未锟揭碉拷锟斤拷锟�, Toast.LENGTH_SHORT).show();
					return;
				}
				RouteOverlay routeOverlay = new RouteOverlay(LocationOverlay.this, mMapView);
			    routeOverlay.setData(res.getPlan(0).getRoute(0));
			    
			    route = res.getPlan(0).getRoute(0);
			    
                int distanceM = route.getDistance();
                distanceKm = String.valueOf(distanceM / 1000) +"."+String.valueOf(distanceM % 1000);

                step = route.getStep(0);
                routeMessage.setText(step.getContent());
                
                roadInfo = step.getContent();
                                
                distanceMessage.setText("distance:" + distanceKm + " KM        ");
                //for (int i = 0; i < route.getNumSteps(); i++) {
                    //MKStep step1 = route.getStep(i);
                    //System.out.println("distance:" + distanceKm + " KM        ");
                    //if(distanceKm != null)
                    	//mTts.speak(distanceKm, TextToSpeech.QUEUE_FLUSH, null);
                    //System.out.println("Information:"+step.getContent());
                //}

                routeOverlay.setData(route);
                
                
                if(refreshMap == true){
                	mMapView.getOverlays().clear();
    			    mMapView.getOverlays().add(routeOverlay);
    			    mMapView.getController().animateTo(pt);
    			    mMapView.refresh();
    			    mMapView.getController().animateTo(res.getStart().pt);
    			    refreshMap = false;
                }
			    
			}
			public void onGetAddrResult(MKAddrInfo res, int error) {
				
			}
			public void onGetPoiResult(MKPoiResult res, int arg1, int arg2) {
				
			}
			public void onGetBusDetailResult(MKBusLineResult result, int iError) {
				
			}

			@Override
			public void onGetSuggestionResult(MKSuggestionResult res, int arg1) {
				
			}
        	
        };
		
		mSearch = new MKSearch();
        mSearch.setDrivingPolicy(MKSearch.ECAR_DIS_FIRST);
        mSearch.init(app.mBMapManager, mKSearchListener);
    }
    
    public void startTts(){
    	mySurfaceView.onPrepared(startSpeaking);
    	mySurfaceView.onCompletion(startSpeaking);
    	try{
			Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
					RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话");
			
			startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "无法连接服务器", 1).show();
		}
    }
       
    @Override
	public boolean onTouchEvent(MotionEvent arg0) {
		switch (arg0.getAction()) {
			case MotionEvent.ACTION_DOWN:
				time = System.currentTimeMillis();
				isLongPress = false;
				break;
	
			case MotionEvent.ACTION_UP:
				if (System.currentTimeMillis() - time > 1000) {
					isLongPress = true;
				}
				break;
			default:
				break;
		}
		return gestureScanner.onTouchEvent(arg0);
	}
    
    @Override
	public void onLongPress(MotionEvent e) {
    	if(isLongAfter == false){
    		mTts.stop();
    		mTts.speak("1、求助，2、联系人，3、红绿灯检测，4、走路前往，5、交通工具出行，。请说出您要执行的操作",
    				TextToSpeech.QUEUE_ADD, null);
    		while(mTts.isSpeaking() == true){
    			
    		}
    		startTts();
    	}
    		
    	else{
    		isLongAfter = false;
    		mTts.speak("您想去目的地是:"+ line.getGetOnStop().name.substring(0, 
    				line.getGetOnStop().name.length()-1),
    				TextToSpeech.QUEUE_ADD, null);
    		
			destination.setText(line.getGetOnStop().name.substring(
					0, line.getGetOnStop().name.length()-1));
			walkOrTran = 0;
			
			stNode = new MKPlanNode();
			stNode.pt = pt;
			enNode = new MKPlanNode();
			enNode.name = destination.getText().toString();
			
			mSearch.walkingSearch("北京", stNode, "北京", enNode);
			while(mTts.isSpeaking() == true){
				//try {  
					//Log.i(TAG, "sleep(1000)...");  
					//Thread.sleep(1);  
				//} catch (InterruptedException e) {
            	
				//} 
			}
			
    	}
    		
    	while(mTts.isSpeaking() == true){
    		
    	}
    	
		isLongPress = false;
	}
    
	//@SuppressLint("NewApi")
	private void NavigationProcess(){
    	destination = (TextView) findViewById(R.id.destination);
    	
    	/*if(destination.isDirty() == true)
    		isDesChange = true;
    	else
    		isDesChange = false;*/
    		
    	stNode = new MKPlanNode();
		stNode.pt = pt;
		enNode = new MKPlanNode();
		enNode.name = destination.getText().toString();
		
		if(walkOrTran == 0){
			mSearch.walkingSearch("北京", stNode, "北京", enNode);
		}
		else{
			mSearch.transitSearch("北京", stNode, enNode);
			//walkOrTran = 0;
		}
		//System.out.println(destination.getText());
		/*if(destination.equals("")){
			distanceMessage.setText("Distance:"+destination.getText().toString());
			routeMessage.setText("没有找到相应位置");
		}
		else{
			
		}*/
		//}else if(walkingNavigation.equals(v)){
		//	mSearch.walkingSearch("北京", stNode, "北京", enNode);
		//}else{
			//mSearch.transitSearch("北京", stNode, enNode);
		//}
	}
    
    @Override
	protected void onStart() {
    	MapInit();//初始化地图
		//checkTtsData(); // 校验TTS引擎安装及资源状态
    	
		super.onStart();
	}
    
    @Override
    protected void onPause() {
        mMapView.onPause();
        if (isSetting) {
			//checkTtsData(); // 校验TTS引擎安装及资源状态
			isSetting = false;
		}
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        mMapView.onResume();
        // 从设置返回后重新绑定TTS，避免仍用旧引擎 //
		if (isSetting) {
			//checkTtsData(); // 校验TTS引擎安装及资源状态
			isSetting = false;
		}
        super.onResume();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mMapView.onSaveInstanceState(outState);
    	
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mMapView.onRestoreInstanceState(savedInstanceState);
    }
    
    public void testUpdateClick(){
        mLocClient.requestLocation();
    }
    private void initMapView() {
        mMapView.setLongClickable(true);
        LocationOverlay.mMapView.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
						time = System.currentTimeMillis();
						isLongPress = false;
						break;
					case MotionEvent.ACTION_UP:
						if(System.currentTimeMillis() - time > 1000)
							isLongPress = true;
						break;
					default:
						break;
				}
				
				return gestureScanner.onTouchEvent(event); 
			}
        	
        });
        
        LocationOverlay.mMapView.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View arg0) {
				// TODO Auto-generated method stub
				if(isLongPress == true)
					startTts();
				isLongPress = false;
				
				return true;
			}
        	
        });
    }
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        menu.add(0, WALK_MENU_ID, 0, "交通工具");
        menu.add(0, TRAN_MENU_ID, 0, "步行");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
		    case WALK_MENU_ID:
		    	walkOrTran = 1;
		    return true;
		    case TRAN_MENU_ID:
		    	walkOrTran = 0;
		    return true;
	    }
	    return super.onOptionsItemSelected(item);
    }
    
    public class MyLocationListenner implements BDLocationListener {
    	
    	private double disA = 0;
    	private double disB = 0;
    	
        @Override
        public void onReceiveLocation(BDLocation location) {
        	
        	//TextView lt = (TextView)findViewById(R.id.locationT);
        	
            if (location == null)
                return ;
            locData.latitude = location.getLatitude();
            locData.longitude = location.getLongitude();
            locData.direction = 2.0f;
            locData.accuracy = location.getRadius();
            locData.direction = location.getDerect();
            mLocClient.requestLocation();
            //lt.setText("location:(" + locData.longitude + "," + locData.latitude+")");
            if(distanceKm != null){
            	pt = new GeoPoint((int)(location.getLatitude()*1e6), 
            			(int)(location.getLongitude()*1e6));
            	NavigationProcess();
            	
            	disA = Double.parseDouble(distanceKm);
            	if(afterDistance != null)
            		disB = Double.parseDouble(afterDistance);
            	//System.out.println(disA + "=?" + disB);
            	if(disA != disB){
            		turnTime = System.currentTimeMillis();
            		refreshMap = true;
            		int distanceM;
            		if(walkOrTran == 0){
            			distanceM = route.getDistance();
            		}
            		else{
            			distanceM =line.getDistance();
            		}
	                distanceKm = String.valueOf(distanceM / 1000) +"."+String.valueOf(distanceM % 1000);
	                //if(ifSpeakDestination == true){
	                	
	                	//ifSpeakDestination = false;
	                //}
	                //if(isDesChange = true)
	                	//mTts.stop();
	                if (mTts.isSpeaking() == true)
	                	mTts.stop();
	                if(walkOrTran == 0)
	                	mTts.speak("距离:" + distanceKm + "千米", TextToSpeech.QUEUE_ADD, null);
	                
	            	myLocationOverlay.setData(locData);
	                mMapView.refresh();
	                
	                if(walkOrTran == 0){
	                	step = route.getStep(0);
	                	roadInfo = step.getContent();
	                	mTts.speak(roadInfo, TextToSpeech.QUEUE_ADD, null);
	                }
	                	
	                else{
	                	roadInfo = "在"+line.getGetOnStop().name+"上车" + line.getTip();
	                	mTts.speak(roadInfo, TextToSpeech.QUEUE_ADD, null);
	                	mTts.speak("若想了解到达车站的方式请长按", TextToSpeech.QUEUE_ADD, null);
	                	isLongAfter = true;
	                }
	                	
	                //if(mTts.isSpeaking() == false){
	                	
	                //}
	                	
	                
	                for(int i = 2; i < roadInfo.length()-2; i++){
	                	int a = roadInfo.charAt(i-1);
		                char b = roadInfo.charAt(i);
		                char c = roadInfo.charAt(i+1);
		                int d = roadInfo.charAt(i+2);
		                
		                //System.out.println(a+"!!"+d);
	                	if((a <= 48 || a >= 58) && (b =='1' || b == '3') 
	                			&& c == '0'&&(d <= 48 || d >= 58)){
	                		isTurn = true;
	                	}
	                }
            	}
            	
                if(isTurn == true && System.currentTimeMillis() - turnTime > 15000){
                	turn = (TextView)findViewById(R.id.turn);
                	step = route.getStep(1);
	    	        roadInfo = step.getContent();
	    	        for(int i = 0; i < roadInfo.length(); i++){
	                	if(roadInfo.charAt(i) == '左'){
	                		//turn.setText("左转   ");
	                	}
	                	else if(roadInfo.charAt(i) == '右'){
	                		//turn.setText("右转   ");
	                	}
	                	else{
	                		
	                	}
	                }
	    	        //mTts.speak(roadInfo, TextToSpeech.QUEUE_ADD, null);
	    	        mTts.speak(turn.getText().toString(), TextToSpeech.QUEUE_ADD, null);
	    	        isTurn = false;
	    	        //System.out.println("!!"+isTurn);
                }
                		
            	afterDistance = distanceKm;
            }
            else
            {
            	//System.out.println("null");
            	if(disA == 0 && disB == 0){
            		pt = new GeoPoint((int)(location.getLatitude()*1e6), 
                    		(int)(location.getLongitude()*1e6));
            		NavigationProcess();
                    mMapView.getController().animateTo(pt);
                    myLocationOverlay.setData(locData);
	                mMapView.refresh();
	                refreshMap = true;
            	}
            }
            
        }
        
        public void onReceivePoi(BDLocation poiLocation) {
            if (poiLocation == null){
                return ;
            }
        }
    }
    
    public class NotifyLister extends BDNotifyListener{
        public void onNotify(BDLocation mlocation, float distance) {
        }
    }

    @Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		//TTS Engine初始化完成
		if(status == TextToSpeech.SUCCESS)
		{
			int result = mTts.setLanguage(Locale.CHINESE);
			//设置发音语言
			if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
			//判断语言是否可用
			{
				System.out.println("Language is not available");
			}
			else
			{
				mTts.speak("欢迎使用盲人导航，请长按屏幕，打开主菜单。"
						, TextToSpeech.QUEUE_ADD, null);
			}
			
		}
		
	}
	
	protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(requestCode==VOICE_RECOGNITION_REQUEST_CODE && resultCode==RESULT_OK){
			resultString="";
			ArrayList<String> results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			if(ifChoose == 0){
				destination.setText("");
				//String tran = results.get(0);
				//System.out.println(results.get(0));
				if(results.get(0).equals("红绿灯")||results.get(0).equals("3")){
					mTts.stop();
					mTts.speak("开始红绿灯检测.",TextToSpeech.QUEUE_ADD, null);
					ifChoose = 0;
					Intent startOpencv = new Intent(LocationOverlay.this, JudgeTrafficLight.class);
					LocationOverlay.this.startActivity(startOpencv);
				}
				else if(results.get(0).equals("联系人")||results.get(0).equals("2")){
					ifChoose = 3;
					mTts.speak("请说出要新紧急联系人号码",TextToSpeech.QUEUE_ADD, null);
					while(mTts.isSpeaking() == true){
						
					}
					startTts();
				}
				else if(results.get(0).equals("求助")||results.get(0).equals("1")){
		
					mTts.speak("紧急情况",TextToSpeech.QUEUE_ADD, null);
					smsManager = SmsManager.getDefault();
					PendingIntent sentIntent = PendingIntent.getBroadcast(LocationOverlay.this, 0, new Intent(), 0);
					smsManager.sendTextMessage(emergencyNum, null, 
							"我在坐标（"+locData.longitude+","+locData.latitude+"）遭遇麻烦，请速来帮助我。", 
							sentIntent, null);
				}
				else if(results.get(0).equals("走路")||results.get(0).equals("4")){
					walkOrTran = 0;
					mTts.speak("您选择了步行出行",TextToSpeech.QUEUE_ADD, null);
					
					ifChoose = 1;
					mTts.speak("请说出您想去的目的地",TextToSpeech.QUEUE_ADD, null);
					while(mTts.isSpeaking() == true){
					}
					startTts();
				}
				else if(results.get(0).equals("交通工具")||results.get(0).equals("5")){
					walkOrTran = 1;
					mTts.speak("您选择了乘交通工具出行",TextToSpeech.QUEUE_ADD, null);
					
					ifChoose = 1;
					mTts.speak("请说出您想去的目的地",TextToSpeech.QUEUE_ADD, null);
					while(mTts.isSpeaking() == true){
					}
					startTts();
				}
				else{
					ifChoose = 0;
					mTts.speak("请重新说一遍",TextToSpeech.QUEUE_ADD, null);
					while(mTts.isSpeaking() == true){
					}
				}
				
			}
			else if(ifChoose == 1){
				getResults = results;
				
				if(getResults.get(0).equals("红绿灯")){
					mTts.stop();
					mTts.speak("开始红绿灯检测.",TextToSpeech.QUEUE_ADD, null);
					ifChoose = 0;
					Intent startOpencv = new Intent(LocationOverlay.this, JudgeTrafficLight.class);
					LocationOverlay.this.startActivity(startOpencv);
					walkOrTran = 0;
				}
				else if(results.get(0).equals("求助")){
					
					mTts.speak("紧急情况",TextToSpeech.QUEUE_ADD, null);
					smsManager = SmsManager.getDefault();
					PendingIntent sentIntent = PendingIntent.getBroadcast(LocationOverlay.this, 0, new Intent(), 0);
					smsManager.sendTextMessage(emergencyNum, null, 
							"我在坐标（"+locData.longitude+","+locData.latitude+"）遭遇麻烦，请速来帮助我。", 
							sentIntent, null);
					ifChoose = 0;
				}
				else{
					mTts.stop();
					mTts.speak("已获得以下结果", TextToSpeech.QUEUE_ADD, null);
					
					if(results.size() < 3){
						for(int i = 0; i < results.size();i++){
							mTts.speak((i+1)+ "," +getResults.get(i), TextToSpeech.QUEUE_ADD, null);
							//resultString+=results.get(i);
						}
					}
					else{
						for(int i = 0; i < 3;i++){
							mTts.speak((i+1)+ "," +getResults.get(i), TextToSpeech.QUEUE_ADD, null);
							//resultString+=results.get(i);
						}
					}
					mTts.speak("请说出您想去目的地的序号",TextToSpeech.QUEUE_ADD, null);
					ifChoose = 2;
					
					while(mTts.isSpeaking() == true){
						/*try {  
							//Log.i(TAG, "sleep(1000)...");  
							Thread.sleep(1);  
						} catch (InterruptedException e) {
	                	
						} */
					}
					
					startTts();
				}
				/*try{
					Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
					
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
							RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
					
					intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话");
					
					startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
				}catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					mTts.speak("无法连接服务器", TextToSpeech.QUEUE_ADD, null);
				}*/
			}
			else if(ifChoose == 2){
				//String tempString = "";
				//tempString += results.get(0);
				try{
					int aim = Integer.valueOf(results.get(0)).intValue();
					resultString+=getResults.get(aim-1);
					//ifSpeakDestination = true;
					
					if(resultString.equals("红绿灯")){
						mTts.stop();
						mTts.speak("开始红绿灯检测.",TextToSpeech.QUEUE_ADD, null);
						Intent startOpencv = new Intent(LocationOverlay.this, JudgeTrafficLight.class);
						LocationOverlay.this.startActivity(startOpencv);
						walkOrTran = 0;
					}
					
					else if(results.get(0).equals("求助")){
						
						mTts.speak("紧急情况",TextToSpeech.QUEUE_ADD, null);
						smsManager = SmsManager.getDefault();
						PendingIntent sentIntent = PendingIntent.getBroadcast(LocationOverlay.this, 0, new Intent(), 0);
						smsManager.sendTextMessage(emergencyNum, null, 
								"我在坐标（"+locData.longitude+","+locData.latitude+"）遭遇麻烦，请速来帮助我。", 
								sentIntent, null);
						walkOrTran = 0;
					}
					else{
						mTts.speak("您想去目的地是:"+ resultString,TextToSpeech.QUEUE_ADD, null);
						if(resultString != "")
							destination.setText(resultString);
						else
							destination.setText("");
						while(mTts.isSpeaking() == true){
							//try {  
								//Log.i(TAG, "sleep(1000)...");  
								//Thread.sleep(1);  
							//} catch (InterruptedException e) {
		                	
							//} 
						}
					}
					
				}catch(NumberFormatException e){
					if(results.equals("重新输入")){
						mTts.stop();
						mTts.speak("请重新说出您的目的地"+ resultString,TextToSpeech.QUEUE_ADD, null);
						ifChoose = 1;
					}
					else{
						resultString+=getResults.get(0);
						mTts.speak("您想去目的地是:"+ resultString,TextToSpeech.QUEUE_ADD, null);
						if(resultString != "")
							destination.setText(resultString);
						else
							destination.setText("");
						while(mTts.isSpeaking() == true){
							//try {  
								//Log.i(TAG, "sleep(1000)...");  
								//Thread.sleep(1);  
							//} catch (InterruptedException e) {
		                	
							//} 
						}
					}
				}
				ifChoose = 0;
			}
			else if(ifChoose == 3){
				emergencyNum = results.get(0);
				//System.out.println(emergencyNum);
				mTts.speak("新联系人号码："+emergencyNum,TextToSpeech.QUEUE_ADD, null);
				linkman.setText(emergencyNum);
				ifChoose = 0;
			}
			//Toast.makeText(this, resultString, 1).show();
		}
		
		if(requestCode == REQ_TTS_STATUS_CHECK)
		{
			switch (resultCode) {
			case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
				//这个返回结果表明TTS Engine可以用
			{
				mTts = new TextToSpeech(this, this);
				//Log.v(TAG, "TTS Engine is installed!");
				
			}
				
			break;
			case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
				//需要的语音数据已损坏
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
				//缺少需要语言的语音数据
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
				//缺少需要语言的发音数据
			{
				//这三种情况都表明数据有错,重新下载安装需要的数据
				//Log.v(TAG, "Need language stuff:"+resultCode);
				Intent dataIntent = new Intent();
				dataIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(dataIntent);
				
			}
				break;
			case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
				//检查失败
			default:
				//Log.v(TAG, "Got a failure. TTS apparently not available");
				break;
			}
		}
		else
		{
			//其他Intent返回的结果
		}
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//释放TTS的资源
		mTts.shutdown();
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
}