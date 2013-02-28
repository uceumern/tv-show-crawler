package com.example.tvshowcrawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class JSONUtils
{
	private static final String TAG = "JSONUtils";

	public static JSONObject loadAppJSONFile(Context appContext, final String baseName)
	{
		try
		{
			FileInputStream fis = appContext.openFileInput(baseName);
			JSONObject jo = new JSONObject(readAll(fis));
			fis.close();
			return jo;
		} catch (JSONException e)
		{
			Log.e(TAG, e.toString());
			return null;
		} catch (FileNotFoundException e)
		{
			Log.e(TAG, e.toString());
			return null;
		} catch (IOException e)
		{
			Log.e(TAG, e.toString());
			return null;
		}
	}

	/**
	 * Reads the entire contents of the given input stream and returns it as a string. This function does not do any
	 * sort of parsing of the data.
	 * 
	 * @param is
	 *            The input stream to consume.
	 * @return The entire contents of the stream.
	 * @throws IOException
	 *             If the reading API throws.
	 */
	public static String readAll(final InputStream is) throws IOException
	{
		if (null == is)
		{
			throw new IllegalArgumentException(JSONUtils.class.getName() + ".readAll() was passed a null stream!");
		}
		StringBuilder sb = new StringBuilder();
		{
			int rc = 0;
			while ((rc = is.read()) >= 0)
			{
				sb.append((char) rc);
			}
		}
		return sb.toString();
	}

	public static boolean saveAppJSONFile(Context appContext, final String baseName, final JSONObject jo)
			throws JSONException
	{
		return saveAppJSONFile(appContext, baseName, jo.toString(4));
	}

	public static boolean saveAppJSONFile(Context appContext, final String baseName, final String json)
	{
		try
		{
			final FileOutputStream fos = appContext.openFileOutput(baseName, Context.MODE_PRIVATE);
			fos.write(json.getBytes());
			fos.close();
			return true;
		} catch (IOException e)
		{
			Log.e(JSONUtils.class.getName(), "EXCEPTION in saveAppJSONFile(" + baseName + "): " + e, e);
			return false;
		}
	}
}
