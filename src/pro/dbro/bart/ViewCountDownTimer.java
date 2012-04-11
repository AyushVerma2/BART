package pro.dbro.bart;

import java.util.ArrayList;
import java.util.Date;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

public class ViewCountDownTimer extends CountDownTimer {
	
	ArrayList timerViews;
	static final long DEPARTING_TRAIN_PADDING_MS = 15*1000; // how long after departure should we display train?
															  // since timer polls only once per minute setting <60s
															  // only effectively removes trains leaving when request is first sent
	static final long MINIMUM_TICK_MS = 1*1000; // disable timer from ticking onStart(); Causes flickering when set on view that has just been
												 // populated with a time. 
	private long COUNTDOWN_TIME_MS; // initial countdown time

	public ViewCountDownTimer(long millisInFuture, long countDownInterval) {
		super(millisInFuture, countDownInterval);
		// TODO Auto-generated constructor stub
	}
	public ViewCountDownTimer(ArrayList tViews, long millisInFuture, long countDownInterval) {
		super(millisInFuture, countDownInterval);
		COUNTDOWN_TIME_MS = millisInFuture;
		timerViews = tViews;
		
	}

	@Override
	public void onFinish() {
		// TODO Auto-generated method stub
		//broadcast message to TheActivity to refresh data from the BART API
		sendMessage(2);
	}

	@Override
	public void onTick(long millisUntilFinished) {
		// TODO Auto-generated method stub
		if(COUNTDOWN_TIME_MS - millisUntilFinished < MINIMUM_TICK_MS){
			return;
		}
		long now = new Date().getTime(); // do this better
		
		for(int x=0;x<timerViews.size();x++){
			// eta - now = ms till arrival
			long eta = (Long) ((TextView)timerViews.get(x)).getTag();
			if((eta + DEPARTING_TRAIN_PADDING_MS - now ) < 0){
				//eta = 0;
				//eta TextView inside TableRow inside TableLayout
				try{
					if(TheActivity.lastRequest == "route"){
	
						View parent = ((View) ((TextView)timerViews.get(x)).getParent());
						route thisRoute = (route)parent.getTag();
						parent.setVisibility(View.GONE);
						if (thisRoute.isExpanded){
							ViewGroup grandparent = (ViewGroup) parent.getParent();
							//if route view is expanded, the next row in Table will be route detail
							grandparent.getChildAt((grandparent.indexOfChild(parent)+1)).setVisibility(View.GONE);
						}
						//tableLayout.removeView((View)((View)timerViews.get(x)).getParent());
					}
					else if(TheActivity.lastRequest == "etd"){
						((TextView)timerViews.get(x)).setVisibility(View.GONE);
					}
				}catch(Throwable t){
					// removing departed trains is a 'garnish' so let's not 
					// worry TOO much if some weird casting exception crops up
				}
				
			}
			else if((eta - now ) < 0){
				eta = 0;
			}
			else{
				eta = eta - now;
			}
			((TextView)timerViews.get(x)).setText(String.valueOf((eta) / ( 1000 *60)));
		}
	}
	
	private void sendMessage(int status) { // 0 = service stopped , 1 = service started, 2 = refresh view with call to bartApiRequest()
  	  Log.d("sender", "View countdown expired");
  	  Intent intent = new Intent("service_status_change");
  	  // You can also include some extra data.
  	  intent.putExtra("status", status);
  	  LocalBroadcastManager.getInstance(TheActivity.c).sendBroadcast(intent);
  	}

}
