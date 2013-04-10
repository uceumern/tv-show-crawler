package com.example.tvshowcrawler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings
{
	private Settings()
	{
	}

	public boolean getCatchUp()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(context.getString(R.string.pref_key_catchup), false);
	}

	public Context getContext()
	{
		return context;
	}

	public boolean getPrefer720p()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(context.getString(R.string.pref_key_720p), false);
	}

	public String getTransmissionServer()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(context.getString(R.string.pref_key_transmission_server),
				context.getString(R.string.pref_transmission_server_default));
	}

	public boolean getX264()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(context.getString(R.string.pref_key_x264), false);
	}

	public void setContext(Context context)
	{
		this.context = context;
	}

	private static Settings instance = null;

	public static Settings getInstance()
	{
		if (instance == null)
		{
			instance = new Settings();
		}
		return instance;
	}

	private Context context = null; // remember application context
}
