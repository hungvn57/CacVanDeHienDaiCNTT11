package com.example.basiclauncher;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class AppTouchListener implements OnTouchListener {

	private Context context;
	private RelativeLayout homeView;
	RelativeLayout searchBar, removeBar;
	Pac pac;
	int leftMargine;
	int topMargine;
	boolean isRomove = false;
	public AppTouchListener(Context context, Pac pac, RelativeLayout homeView, RelativeLayout searchBar, RelativeLayout removeBar){
		this.context = context;
		this.homeView = homeView;
		this.searchBar = searchBar;
		this.removeBar = removeBar;
		this.pac = pac;
	}



	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()){
		case MotionEvent.ACTION_MOVE:
			LayoutParams lp = new LayoutParams(v.getWidth(),v.getHeight());

			leftMargine = (int) event.getRawX()-v.getWidth()/2;
			topMargine = (int) event.getRawY()-v.getHeight()/2;

			if (leftMargine+v.getWidth() > v.getRootView().getWidth())
				leftMargine = v.getRootView().getWidth() - v.getWidth();

			if (leftMargine<0)
				leftMargine =0;

			if (topMargine+v.getHeight() >((View) v.getParent()).getHeight())
				topMargine = ((View) v.getParent()).getHeight() - v.getHeight();

			if (topMargine<removeBar.getHeight()) {
				isRomove = true;
			}

			else
				isRomove = false;


			lp.leftMargin = leftMargine;
			lp.topMargin = topMargine;
			v.setLayoutParams(lp);
			break;
		case MotionEvent.ACTION_UP:
			if(isRomove){
				pac.deleteIcon();
				AppSerializableData objectData = SerializationTools.loadSerializedData();

				objectData.apps.remove(pac.getPos());
				objectData.apps.add(pac.getPos(), null);

				pac.removeHome(context, homeView, pac.getPos());

				for (Pac pacToAddToHome : objectData.apps) {
					if(pacToAddToHome!=null && pacToAddToHome.getPos()>pac.getPos()){
						pacToAddToHome.setPos(pacToAddToHome.getPos()-1);
					}
				}

				SerializationTools.serializeData(objectData);
			}
			searchBar.setVisibility(View.VISIBLE);
			removeBar.setVisibility(View.GONE);
			v.setOnTouchListener(null);
			break;

		}
		return true;
	}

}
