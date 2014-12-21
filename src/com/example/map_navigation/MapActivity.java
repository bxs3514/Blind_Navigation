package com.example.map_navigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.RouteOverlay;
import com.baidu.mapapi.map.TransitOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKPlanNode;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;

public class MapActivity extends Activity {
	
	private BMapManager mBMapMan;
	private MapView mMapView;
	private MKSearch mSearch;
	private MyLocationOverlay myLocationOverlay;
	private LocationData locData;
	public MKMapViewListener mMapListener;
	
	private TextView start;
	private TextView end;
	private Button drivingNavigation;
	private Button walkingNavigation;
	private Button busNavigation;
	private Button navigation;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_map);
        
        DemoApplication app = (DemoApplication)this.getApplication();
		if (app.mBMapManager == null) {
			app.mBMapManager = new BMapManager(this);
			app.mBMapManager.init(DemoApplication.strKey,new DemoApplication.MyGeneralListener());
		}
		
		mMapView = (MapView)findViewById(R.id.bmapsView);
		mMapView.getController();
		initMapView();
		
		mMapView.regMapViewListener(DemoApplication.getInstance().mBMapManager, mMapListener);
		myLocationOverlay = new MyLocationOverlay(mMapView);
		locData = new LocationData();
	    myLocationOverlay.setData(locData);
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		mMapView.refresh();
        
        mMapView.getController().enableClick(true);
        mMapView.getController().setZoom(14);
        mMapView.displayZoomControls(true);
        mMapView.setDoubleClickZooming(true);        
        
        //�����¼�
        mSearch = new MKSearch();
        //mSearch.setDrivingPolicy(MKSearch.ECAR_DIS_FIRST);
        mSearch.init(app.mBMapManager, new MKSearchListener(){

            @Override
            public void onGetPoiDetailSearchResult(int type, int error) {
            }
            
			public void onGetDrivingRouteResult(MKDrivingRouteResult res,
					int error) {
				if (error != 0 || res == null) {
					Toast.makeText(MapActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_SHORT).show();
					return;
				}
				RouteOverlay routeOverlay = new RouteOverlay(MapActivity.this, mMapView);
			    routeOverlay.setData(res.getPlan(0).getRoute(0));
			    mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.refresh();
			    mMapView.getController().animateTo(res.getStart().pt);
			}

			public void onGetTransitRouteResult(MKTransitRouteResult res,
					int error) {
				if (error != 0 || res == null) {
					Toast.makeText(MapActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_SHORT).show();
					return;
				}
				TransitOverlay  routeOverlay = new TransitOverlay (MapActivity.this, mMapView);
			    routeOverlay.setData(res.getPlan(0));
			    mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.refresh();
			    mMapView.getController().animateTo(res.getStart().pt);
			}

			public void onGetWalkingRouteResult(MKWalkingRouteResult res,
					int error) {
				if (error != 0 || res == null) {
					Toast.makeText(MapActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_SHORT).show();
					return;
				}
				RouteOverlay routeOverlay = new RouteOverlay(MapActivity.this, mMapView);
	
			    routeOverlay.setData(res.getPlan(0).getRoute(0));
			    mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.refresh();
			    mMapView.getController().animateTo(res.getStart().pt);
			    
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

        });
        // ������ť��Ӧ
    	drivingNavigation = (Button)findViewById(R.id.driving);
    	busNavigation = (Button)findViewById(R.id.bus);
    	walkingNavigation = (Button)findViewById(R.id.walking);
    	navigation = (Button)findViewById(R.id.navigation);
    	
        OnClickListener clickListener = new OnClickListener(){
			public void onClick(View v) {
					SearchButtonProcess(v);
			}
        };
        
        drivingNavigation.setOnClickListener(clickListener); 
        busNavigation.setOnClickListener(clickListener); 
        walkingNavigation.setOnClickListener(clickListener);
        
        navigation.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent;
				intent = new Intent(MapActivity.this, LocationOverlay.class);
				MapActivity.this.startActivity(intent);
				//MapActivity.this.finish();
			}
        	
        });
	}
	
	void SearchButtonProcess(View v){
		start = (TextView) findViewById(R.id.start);
		end = (TextView) findViewById(R.id.end);
		
		MKPlanNode stNode = new MKPlanNode();
		stNode.name = start.getText().toString();
		MKPlanNode enNode = new MKPlanNode();
		enNode.name = end.getText().toString();
		
		if(drivingNavigation.equals(v)){
			mSearch.drivingSearch("北京", stNode, "北京", enNode);	
		}else if(walkingNavigation.equals(v)){
			mSearch.walkingSearch("北京", stNode, "北京", enNode);
		}else{
			mSearch.transitSearch("北京", stNode, enNode);
		}
	}
	
	private void initMapView() {
        mMapView.setLongClickable(true);
        //mMapController.setMapClickEnable(true);
        //mMapView.setSatellite(false);
    }
	
	@Override
	protected void onDestroy(){
	        mMapView.destroy();
	        if(mBMapMan!=null){
	                mBMapMan.destroy();
	                mBMapMan=null;
	        }
	        super.onDestroy();
	}
	@Override
	protected void onPause(){
	        mMapView.onPause();
	        if(mBMapMan!=null){
	                mBMapMan.stop();
	                //myLocationOverlay.disableCompass();
	                
	        }
	        super.onPause();
	}
	@Override
	protected void onResume(){
	        mMapView.onResume();
	        if(mBMapMan!=null){
	                mBMapMan.start();
	                //myLocationOverlay.enableCompass();
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
    
    /*Class<?> mActivities[] = {
    	LocationOverlay.class	
    };*/
    
}


