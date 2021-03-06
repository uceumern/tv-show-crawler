package com.example.tvshowcrawler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.tvshowcrawler.TVShow.EnumTVShowStatus;

public class UpdateShowService extends IntentService
{
	public UpdateShowService()
	{
		super("UpdateShowService");
	}

	public UpdateShowService(String name)
	{
		super(name);
	}

	private JSONObject buildRequestObject(String sendMethod, JSONObject arguments) throws JSONException
	{
		// Build request for method
		JSONObject request = new JSONObject();
		request.put("method", sendMethod);
		request.put("arguments", arguments);
		request.put("tag", 0);
		return request;
	}

	/**
	 * Build the URL of the Transmission web UI from the user settings.
	 * 
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl()
	{
		return "http://" + Settings.getInstance().getTransmissionServer() + "/transmission/rpc";
	}

	private void initialize()
	{
		// Register http sockets
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));

		int timeout = 5000;
		String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0";

		// Standard parameters
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, timeout);
		HttpConnectionParams.setSoTimeout(httpparams, timeout);
		HttpProtocolParams.setUserAgent(httpparams, userAgent);

		httpclient = new DefaultHttpClient(new ThreadSafeClientConnManager(httpparams, registry),
				httpparams);
	}

	private boolean isTransmissionServerOnline()
	{
		boolean ret = false;
		// check remote transmission server
		Log.i(TAG, "Checking Transmission server at: " + Settings.getInstance().getTransmissionServer());
		try
		{
			URL url = new URL("http://" + Settings.getInstance().getTransmissionServer());
			// check if server answers
			String html = TVShow.readFromUrl(url);
			if (html == null || html.isEmpty())
			{
				Log.i(TAG, "Transmission server offline.");
				return ret;
			}
			else
			{
				ret = true;
			}
		} catch (MalformedURLException ex)
		{
			Log.e(TAG, "Error while checking Transmission server: " + ex.toString());
			return ret;
		}
		return ret;
	}

	private JSONObject makeRequest(JSONObject data)
	{
		try
		{
			// Initialize the HTTP client
			if (httpclient == null)
			{
				initialize();
			}
			final String sessionHeader = "X-Transmission-Session-Id";

			// Setup request using POST stream with URL and data
			HttpPost httppost = new HttpPost(buildWebUIUrl());
			StringEntity se = new StringEntity(data.toString(), "UTF-8");
			httppost.setEntity(se);

			// Send the stored session token as a header
			if (sessionToken != null)
			{
				httppost.addHeader(sessionHeader, sessionToken);
			}

			// Execute
			HttpResponse response = httpclient.execute(httppost);

			// Authentication error?
			if (response.getStatusLine().getStatusCode() == 401)
			{
				return null;
			}

			// 409 error because of a session id?
			if (response.getStatusLine().getStatusCode() == 409)
			{

				// Retry post, but this time with the new session token that was encapsulated in the 409 response
				sessionToken = response.getFirstHeader(sessionHeader).getValue();
				httppost.addHeader(sessionHeader, sessionToken);
				response = httpclient.execute(httppost);
			}

			HttpEntity entity = response.getEntity();
			if (entity != null)
			{
				// Read JSON response
				java.io.InputStream instream = entity.getContent();
				BufferedInputStream bis = new BufferedInputStream(instream);
				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				int current = 0;
				while ((current = bis.read()) != -1)
				{
					baf.append((byte) current);
				}
				String result = new String(baf.toByteArray());
				JSONObject json = new JSONObject(result);
				instream.close();

				Log.d(TAG, "Success: " + (result.length() > 200 ? result.substring(0, 200) + "... (" +
						result.length() + " chars)" : result));

				// Return the JSON object
				return json;
			}

			Log.e(TAG, "Error: No entity in HTTP response");

		} catch (Exception e)
		{
			Log.e(TAG, "Error: " + e.toString());
		}
		return null;
	}

	private void notifyShowUpdated(TVShow show)
	{
		Intent localIntent = new Intent(BROADCAST_TVSHOW_UPDATED_ACTION);
		localIntent.putExtra("tvShow", show);
		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	private void notifyTransmissionServerOffline()
	{
		Intent localIntent = new Intent(BROADCAST_TRANSMISSION_SERVER_OFFLINE_ACTION);
		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	private boolean startDownload(String magnetLink)
	{
		JSONObject request = new JSONObject();
		try
		{
			request.put("filename", magnetLink);
			JSONObject joAnswer = makeRequest(buildRequestObject("torrent-add", request));
			if (joAnswer != null)
			{
				Log.d(TAG, joAnswer.toString(4));
				return true;
			}
		} catch (JSONException e)
		{
			Log.e(TAG, e.toString());
		}
		return false;
	}

	private void startShowAction(TVShow show, boolean serverOnline)
	{
		switch (show.getStatus())
		{
		case Error:
		case NotChecked:
		case TorrentNotFound:
		case UpToDate:
			// recheck
			updateShow(show, serverOnline);
			break;
		case NewEpisodeAvailable:
			// start download
			if (serverOnline && show.getMagnetLink() != null)
			{
				boolean success = startDownload(show.getMagnetLink());
				Log.d(TAG, "startDownload success: " + success);
				if (success)
				{
					// return to unchecked state
					show.setStatus(EnumTVShowStatus.NotChecked);
					// update show season and episode to the season and episode of the fetched item
					show.setSeason(show.getDownloadItem().getSeason());
					show.setEpisode(show.getDownloadItem().getEpisode());
				}
				else
				{
					show.setStatus(EnumTVShowStatus.Error);
				}
			}
			notifyShowUpdated(show);
			break;
		case Working:
			// do nothing
		default:
			break;
		}
	}

	private void updateShow(TVShow show, boolean serverOnline)
	{
		show.setStatus(EnumTVShowStatus.Working);
		notifyShowUpdated(show);
		show.updateTVRageShowInfoAndEpisodeList(false);
		notifyShowUpdated(show);
		show.update();
		notifyShowUpdated(show);
		if (show.getStatus() == EnumTVShowStatus.NewEpisodeAvailable)
		{
			boolean success = false;
			TorrentItem torrentItem = show.getDownloadItem();
			String magnetLink = torrentItem.getMagnetLink();
			if (magnetLink != null && serverOnline)
			{
				Log.i(TAG,
						String.format("Fetching torrent for '%s S%02dE%02d'...", show.getName(),
								torrentItem.getSeason(),
								torrentItem.getEpisode()));
				success = startDownload(magnetLink);

				if (success)
				{
					// notify main app that a torrent download has started
					Intent localIntent = new Intent(BROADCAST_NEW_EPISODE_ACTION);
					localIntent.putExtra("tvShow", show);
					LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

					// return to unchecked state
					show.setStatus(EnumTVShowStatus.NotChecked);
					// update show season and episode to the season and episode of the fetched item
					show.setSeason(torrentItem.getSeason());
					show.setEpisode(torrentItem.getEpisode());
				}
				else
				{
					show.setStatus(EnumTVShowStatus.Error);
				}
			}
			notifyShowUpdated(show);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		if (intent.getAction().equals(BROADCAST_START_SHOW_ACTION))
		{
			boolean serverOnline = isTransmissionServerOnline();
			if (!serverOnline)
				notifyTransmissionServerOffline();

			// update single show action
			TVShow show = intent.getExtras().getParcelable("com.example.tvshowcrawler.tvShow");
			startShowAction(show, serverOnline);
			notifyShowUpdated(show);
		}
		else if (intent.getAction().equals(BROADCAST_UPDATE_ALL_SHOWS_ACTION))
		{
			boolean serverOnline = isTransmissionServerOnline();
			if (!serverOnline)
				notifyTransmissionServerOffline();

			// update all action
			// get tvShows from intent
			assert (intent.hasExtra("com.example.tvshowcrawler.tvShows"));
			tvShows = intent.getParcelableArrayListExtra("com.example.tvshowcrawler.tvShows");

			// update all shows
			for (TVShow show : tvShows)
			{
				updateShow(show, serverOnline);
			}
			Intent localIntent = new Intent(BROADCAST_DONE_ACTION);
			// Broadcasts the Intent to receivers in this app.
			LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
		else if (intent.getAction().equals(BROADCAST_GET_SHOW_LIST))
		{
			String showName = intent.getExtras().getString("com.example.tvshowcrawler.tvShowName");
			Log.i(TAG, "Retrieving TVRage show list...");
			// http://services.tvrage.com/feeds/search.php?show=some show name
			String url = String.format("http://services.tvrage.com/feeds/search.php?show=%s",
					showName.replace(" ", "%20"));
			String rawText;
			try
			{
				rawText = TVShow.readFromUrl(new URL(url));
			} catch (MalformedURLException e)
			{
				Log.e(TAG, e.toString());
				rawText = null;
			}
			if (rawText == null)
			{
				Log.i(TAG, "Retrieving TVRage show info failed.");
				return;
			}

			// parse xml
			StringReader srReader = new StringReader(rawText);
			TVRageXmlParser parser = new TVRageXmlParser();
			TVShows showList = null;
			Intent localIntent = new Intent(BROADCAST_GET_SHOW_LIST_COMPLETE);
			try
			{
				showList = parser.parseShowList(srReader);

				if (showList != null && !showList.isEmpty())
				{
					localIntent.putParcelableArrayListExtra("com.example.tvshowcrawler.tvShows", showList);
				}

			} catch (XmlPullParserException e)
			{
				Log.e(TAG, e.toString());
			} catch (IOException e)
			{
				Log.e(TAG, e.toString());
			}

			LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}

	// Defines a custom Intent action
	public static final String BROADCAST_START_SHOW_ACTION = "com.example.tvshowcrawler.BROADCAST_START_SHOW_ACTION";
	public static final String BROADCAST_UPDATE_ALL_SHOWS_ACTION = "com.example.tvshowcrawler.BROADCAST_UPDATE_ALL_SHOWS_ACTION";
	public static final String BROADCAST_TVSHOW_UPDATED_ACTION = "com.example.tvshowcrawler.BROADCAST_TVSHOW_UPDATED_ACTION";
	public static final String BROADCAST_DONE_ACTION = "com.example.tvshowcrawler.BROADCAST_DONE_ACTION";
	public static final String BROADCAST_NEW_EPISODE_ACTION = "com.example.tvshowcrawler.BROADCAST_NEW_EPISODE_ACTION";
	public static final String BROADCAST_TRANSMISSION_SERVER_OFFLINE_ACTION = "com.example.tvshowcrawler.BROADCAST_TRANSMISSION_SERVER_OFFLINE_ACTION";
	public static final String BROADCAST_GET_SHOW_LIST = "com.example.tvshowcrawler.BROADCAST_GET_SHOW_LIST";
	public static final String BROADCAST_GET_SHOW_LIST_COMPLETE = "com.example.tvshowcrawler.BROADCAST_GET_SHOW_LIST_COMPLETE";

	private static final String TAG = "UpdateShowService";

	private List<TVShow> tvShows;

	private DefaultHttpClient httpclient;
	private String sessionToken;
}
