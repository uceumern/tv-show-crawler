package com.example.tvshowcrawler;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.example.tvshowcrawler.PullToRefreshListView.OnRefreshListener;

public class MainActivity extends Activity
{
	class DownloadStateReceiver extends BroadcastReceiver
	{
		// Called when the BroadcastReceiver gets an Intent it's registered to receive
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.d("DownloadStateReceiver", intent.getAction());

			if (intent.getAction().equals(UpdateShowService.BROADCAST_TVSHOW_UPDATED_ACTION))
			{
				TVShow show = intent.getExtras().getParcelable("tvShow");
				// update show in internal list
				for (TVShow showIter : tvShows)
				{
					if (showIter.getName().equals(show.getName()))
					{
						tvShows.set(tvShows.indexOf(showIter), show);
						break;
					}
				}
			}

			if (intent.getAction().equals(UpdateShowService.BROADCAST_DONE_ACTION))
			{
				// notify list view that refresh has finished
				setListViewRefreshComplete();
			}

			// update view when show changes
			updateView();
		}
	}

	public void onButtonTestStuffClick(View view)
	{
		loadTVShows();
		TVShow show = new TVShow("New Girl", 2, 17);
		tvShows.add(show);
		saveTVShows();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.menu_add:
			// show editor
			Intent intent = new Intent(Intent.ACTION_INSERT);
			startActivityForResult(intent, REQUEST_CODE_EDIT_SHOW);
			return true;
		case R.id.menu_settings:
			// show settings screen
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.menu_about:
			try
			{
				AlertDialog ad = new AlertDialog.Builder(this).create();
				ad.setTitle("About");
				PackageInfo pInfo;
				pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				String messageString = String.format("TV Show Crawler\n\nVersion: %s", pInfo.versionName);
				ad.setMessage(messageString);
				ad.show();
			} catch (NameNotFoundException e)
			{
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void loadTVShows()
	{
		tvShows = new TVShows();
		// load tvShows from /data/data/com.example.tvshowcrawler/files/tvshows.json
		File file = getBaseContext().getFileStreamPath("tvshows.json");
		if (file.exists())
		{
			JSONObject jo = JSONUtils.loadAppJSONFile(getApplicationContext(), "tvshows.json");
			try
			{
				tvShows.fromJSONObject(jo);
			} catch (JSONException e)
			{
				Log.e(TAG, e.toString());
			}
		}
	}

	private void saveTVShows()
	{
		// save tvShows to /data/data/com.example.tvshowcrawler/files/tvshows.json
		try
		{
			// TODO do some sanity checks to prevent overwriting user data with junk
			JSONUtils.saveAppJSONFile(getApplicationContext(), "tvshows.json", tvShows.toJSONObject());
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void setListViewRefreshComplete()
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				listView.onRefreshComplete();
			}
		});
	}

	private void updateView()
	{
		// update list
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				tvShowAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_EDIT_SHOW)
		{
			if (data.hasExtra("tvShow") && data.hasExtra("tvShowIndex"))
			{
				TVShow editedShow = data.getExtras().getParcelable("tvShow");

				// update show in list
				if (Intent.ACTION_EDIT.equals(data.getAction()))
				{
					int position = data.getExtras().getInt("tvShowIndex");
					TVShow show = tvShows.get(position);
					if (!show.equals(editedShow))
					{
						tvShows.set(position, editedShow);
						updateView();
					}
				}
				else if (Intent.ACTION_INSERT.equals(data.getAction()))
				{
					// add show to list
					tvShows.add(editedShow);
					updateView();
				}

				saveTVShows();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Called when the activity is first created. This is where you should do all of your normal static set up:
		// create views, bind data to lists, etc. This method also provides you with a Bundle containing the activity's
		// previously frozen state, if there was one.

		// disable strict mode
		// TODO move time consuming calls to other threads
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// init Settings singleton
		Settings.getInstance().setContext(getApplicationContext());
		// load show list from file
		loadTVShows();

		updateShowServiceIntent = new Intent(MainActivity.this, UpdateShowService.class);

		IntentFilter tvShowUpdatedIntentFilter = new IntentFilter(UpdateShowService.BROADCAST_TVSHOW_UPDATED_ACTION);
		downloadStateReceiver = new DownloadStateReceiver();
		// Registers the DownloadStateReceiver and its intent filters
		LocalBroadcastManager.getInstance(this).registerReceiver(downloadStateReceiver, tvShowUpdatedIntentFilter);

		IntentFilter tvShowUpdateDoneIntentFilter = new IntentFilter(UpdateShowService.BROADCAST_DONE_ACTION);
		// Registers the receiver with the new filter
		LocalBroadcastManager.getInstance(this).registerReceiver(downloadStateReceiver, tvShowUpdateDoneIntentFilter);

		// bind adapter to show list
		tvShowAdapter = new TVShowAdapter(this, R.layout.list_row, tvShows);
		listView = (PullToRefreshListView) findViewById(R.id.listViewTVShows);
		listView.setAdapter(tvShowAdapter);
		// react to item clicks
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				TVShow tvShow = tvShowAdapter.getItem(position);
				if (tvShow != null)
				{
					Intent intent = new Intent(Intent.ACTION_EDIT);
					intent.putExtra("tvShow", tvShow);
					intent.putExtra("tvShowIndex", position);
					startActivityForResult(intent, REQUEST_CODE_EDIT_SHOW);
				}
			}
		});

		listView.setOnRefreshListener(new OnRefreshListener()
		{
			@Override
			public void onRefresh()
			{
				// start UpdateShowService to refresh listView contents
				updateShowServiceIntent.putParcelableArrayListExtra("com.example.tvshowcrawler.tvShows", tvShows);
				MainActivity.this.startService(updateShowServiceIntent);
			}
		});
	}

	@Override
	protected void onStop()
	{
		// save show list to file
		saveTVShows();
		super.onStop();
	}

	private PullToRefreshListView listView;

	protected static final int REQUEST_CODE_EDIT_SHOW = 0;

	private static final String TAG = "TVShowCrawler";

	private TVShows tvShows;

	private TVShowAdapter tvShowAdapter;

	private Intent updateShowServiceIntent;

	private DownloadStateReceiver downloadStateReceiver;
}
