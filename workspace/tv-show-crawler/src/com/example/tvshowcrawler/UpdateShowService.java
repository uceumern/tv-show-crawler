package com.example.tvshowcrawler;

import java.io.BufferedInputStream;
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

	public List<TVShow> getTvShows()
	{
		return tvShows;
	}

	public void setTvShows(List<TVShow> tvShows)
	{
		this.tvShows = tvShows;
	}

	private boolean isTransmissionServerOnline()
	{
		boolean ret = false;
		// check remote transmission server
		Log.i(TAG, "Checking Transmission server at: " + Settings.getInstance().getTransmissionServer());
		try
		{
			URL url = new URL(Settings.getInstance().getTransmissionServer());
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

	private void onShowUpdated(TVShow show)
	{
		Intent localIntent = new Intent(BROADCAST_TVSHOW_UPDATED_ACTION);
		localIntent.putExtra("tvShow", show);
		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	private void updateShow(TVShow show, boolean serverOnline)
	{
		show.setStatus(EnumTVShowStatus.Working);
		onShowUpdated(show);
		show.update();
		// TODO: send status updates during show.update()
		onShowUpdated(show);
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

				JSONObject request = new JSONObject();
				try
				{
					request.put("filename", magnetLink);
					makeRequest(buildRequestObject("torrent-add", request));
				} catch (JSONException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

//				return new DaemonTaskSuccessResult(task);

				// start magnet link
//				Intent i = new Intent(Intent.ACTION_VIEW);
//				i.setData(Uri.parse(magnetLink));
//				i.setType("application/x-bittorrent");
//				try
//				{
//					sendBroadcast(i);
//					success = true;
//				} catch (Exception e)
//				{
//					Log.e(TAG, e.toString());
//				}
			}
			if (success)
			{
				// return to unchecked state
				show.setStatus(EnumTVShowStatus.NotChecked);
				// update show season and episode
				show.setSeason(torrentItem.getSeason());
				show.setEpisode(torrentItem.getEpisode());
			}
			else
			{
				show.setStatus(EnumTVShowStatus.Error);
			}
			onShowUpdated(show);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		// get tvShows from intent
		tvShows = intent.getParcelableArrayListExtra("com.example.tvshowcrawler.tvShows");

		boolean serverOnline = true;
		// not sure if no transmission mode makes sense?
		if (Settings.getInstance().getEnableTransmission())
		{
			serverOnline = isTransmissionServerOnline();
		}

		// update all shows
		for (TVShow show : tvShows)
		{
			updateShow(show, serverOnline);
		}
		Intent localIntent = new Intent(BROADCAST_DONE_ACTION);
		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	// Defines a custom Intent action
	public static final String BROADCAST_TVSHOW_UPDATED_ACTION = "com.example.tvshowcrawler.BROADCAST_TVSHOW_UPDATED_ACTION";

	public static final String BROADCAST_DONE_ACTION = "com.example.tvshowcrawler.BROADCAST_DONE_ACTION";

	// Defines the key for the status "extra" in an Intent
	public static final String EXTENDED_DATA_STATUS = "com.example.tvshowcrawler.STATUS";

	private static final String TAG = "UpdateShowService";

	private List<TVShow> tvShows;

	private JSONObject buildRequestObject(String sendMethod, JSONObject arguments) throws JSONException
	{
		// Build request for method
		JSONObject request = new JSONObject();
		request.put("method", sendMethod);
		request.put("arguments", arguments);
		request.put("tag", 0);
		return request;
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
//				throw new DaemonException(ExceptionType.AuthenticationFailure,
//						"401 HTTP response (username or password incorrect)");
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

	private void initialize()
	{
		// Register http and https sockets
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
//		SocketFactory https_socket = SSLSocketFactory.getSocketFactory();
//		registry.register(new Scheme("https", https_socket, 443));

		int timeout = 5;
		String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0";

		// Standard parameters
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, timeout);
		HttpConnectionParams.setSoTimeout(httpparams, timeout);
		HttpProtocolParams.setUserAgent(httpparams, userAgent);

		httpclient = new DefaultHttpClient(new ThreadSafeClientConnManager(httpparams, registry),
				httpparams);
	}

	/**
	 * Build the URL of the Transmission web UI from the user settings.
	 * 
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl()
	{
		return "http://zbox:9091/transmission/rpc";
//		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort()
//				+ (settings.getFolder() == null ? "" : settings.getFolder()) + "/transmission/rpc";
	}

	private DefaultHttpClient httpclient;
	private String sessionToken;

	private long rpcVersion = -1;
}
