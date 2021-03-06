package com.example.tvshowcrawler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.example.tvshowcrawler.PullToRefreshListView.OnRefreshListener;
import com.example.tvshowcrawler.TVShow.EnumTVShowStatus;

public class MainActivity extends Activity
{
	class DownloadStateReceiver extends BroadcastReceiver
	{
		// Called when the BroadcastReceiver gets an Intent it's registered to receive
		@Override
		public void onReceive(Context context, Intent intent)
		{
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
			else if (intent.getAction().equals(UpdateShowService.BROADCAST_DONE_ACTION))
			{
				// notify list view that refresh has finished
				setListViewRefreshComplete();
			}
			else if (intent.getAction().equals(UpdateShowService.BROADCAST_NEW_EPISODE_ACTION))
			{
				// display message
				TVShow show = intent.getExtras().getParcelable("tvShow");
				Toast.makeText(getApplicationContext(),
						String.format("%s: New episode download started!", show.getName()),
						Toast.LENGTH_LONG).show();

			}
			else if (intent.getAction().equals(UpdateShowService.BROADCAST_TRANSMISSION_SERVER_OFFLINE_ACTION))
			{
				// show message that the transmission server is offline
				Toast.makeText(
						getApplicationContext(),
						String.format("Transmission server at '%s' is offline!", Settings.getInstance()
								.getTransmissionServer()),
						Toast.LENGTH_LONG).show();
			}

			// update view when show changes
			updateListView();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		// handle context menu choice
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		final long row = info.id;
		Log.d(TAG, "onContextItemSelected: " + row);

		TVShow show = tvShows.get((int) row);

		switch (item.getItemId())
		{
		case R.id.menu_listview_increase:
			show.setEpisode(show.getEpisode() + 1);
			show.setStatus(EnumTVShowStatus.NotChecked);
			updateListView();
			return true;
		case R.id.menu_listview_decrease:
			show.setEpisode(Math.max(show.getEpisode() - 1, 0));
			show.setStatus(EnumTVShowStatus.NotChecked);
			updateListView();
			return true;
		case R.id.menu_listview_magnet:
			if (show.getMagnetLink() != null)
			{
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("magnet link", show.getDownloadItem().getMagnetLink());
				clipboard.setPrimaryClip(clip);
			}
			return true;
		case R.id.menu_listview_reset:
			show.setEpisodeList(null);
			show.setNextEpisode(null);
			show.setLastEpisode(null);
			show.setStatus(EnumTVShowStatus.NotChecked);
			updateListView();
			return true;
		case R.id.menu_listview_delete:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
			// Add the buttons
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					// User clicked OK button
					// remove show from list
					tvShows.remove((int) row);
					updateListView();
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					// User cancelled the dialog
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		// create context menu
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.listview, menu);
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
		// Handle options menu item selection
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
				// sort shows alphabetically
				Collections.sort(tvShows);

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

	private void updateListView()
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
		// handle edit tv show result
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
					}
				}
				else if (Intent.ACTION_INSERT.equals(data.getAction()))
				{
					// add show to list
					tvShows.add(editedShow);
				}

				Collections.sort(tvShows);
				saveTVShows();
				updateListView();
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Called when the activity is first created. This is where you should do all of your normal static set up:
		// create views, bind data to lists, etc. This method also provides you with a Bundle containing the activity's
		// previously frozen state, if there was one.

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// init Settings singleton
		Settings.getInstance().setContext(getApplicationContext());
		// load show list from file
		loadTVShows();

		// create intent for updating tv shows
		updateShowServiceIntent = new Intent(MainActivity.this, UpdateShowService.class);
		updateShowServiceIntent.setAction(UpdateShowService.BROADCAST_UPDATE_ALL_SHOWS_ACTION);

		// create DownloadStateReceiver, which handles all intents from the UpdateShowService
		downloadStateReceiver = new DownloadStateReceiver();
		// register BROADCAST_TVSHOW_UPDATED_ACTION intent
		LocalBroadcastManager.getInstance(this).registerReceiver(downloadStateReceiver,
				new IntentFilter(UpdateShowService.BROADCAST_TVSHOW_UPDATED_ACTION));
		// register BROADCAST_DONE_ACTION intent
		LocalBroadcastManager.getInstance(this).registerReceiver(downloadStateReceiver,
				new IntentFilter(UpdateShowService.BROADCAST_DONE_ACTION));
		// register BROADCAST_TRANSMISSION_SERVER_OFFLINE_ACTION intent
		LocalBroadcastManager.getInstance(this).registerReceiver(downloadStateReceiver,
				new IntentFilter(UpdateShowService.BROADCAST_TRANSMISSION_SERVER_OFFLINE_ACTION));

		// create adapter and bind it to show list
		tvShowAdapter = new TVShowAdapter(this, R.layout.list_row, tvShows);
		listView = (PullToRefreshListView) findViewById(R.id.listViewTVShows);
		listView.setAdapter(tvShowAdapter);

		listView.setShowLastUpdatedText(true);
		listView.setLastUpdatedDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
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

		// handle refresh request from listview
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

		registerForContextMenu(listView);
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
