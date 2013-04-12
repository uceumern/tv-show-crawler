package com.example.tvshowcrawler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class EpisodeInfo implements JSONable
{
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;

		EpisodeInfo rhs = (EpisodeInfo) obj;
		return title.equals(rhs.getTitle())
				&& season == rhs.getSeason()
				&& episode == rhs.getEpisode();
	}

	@Override
	public void fromJSONObject(JSONObject src) throws JSONException
	{
		title = src.getString("title");
		season = src.getInt("season");
		episode = src.getInt("episode");
		airTime = null;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
		try
		{
			airTime = new GregorianCalendar();
			airTime.setTime(sdf.parse(src.getString("airTime")));
		} catch (ParseException e)
		{
			Log.e(TAG, e.toString());
		}
	}

	public Calendar getAirTime()
	{
		return airTime;
	}

	public int getEpisode()
	{
		return episode;
	}

	public int getSeason()
	{
		return season;
	}

	public String getTitle()
	{
		return title;
	}

	public void setAirTime(Calendar airTime)
	{
		this.airTime = airTime;
	}

	public void setEpisode(int episode)
	{
		this.episode = episode;
	}

	public void setSeason(int season)
	{
		this.season = season;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException
	{
		JSONObject jo = new JSONObject();

		jo.put("title", title);
		jo.put("season", season);
		jo.put("episode", episode);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
		jo.put("airTime", sdf.format(airTime.getTime()));

		return jo;

	}

	public String toString()
	{
		Calendar startDate = new GregorianCalendar();
		Calendar endDate = airTime;

		long totalMillis = endDate.getTimeInMillis() - startDate.getTimeInMillis();
		int days = (int) (totalMillis / 1000) / 3600 / 24;

		if (days >= 0)
		{
			return String.format(Locale.US, "S%02dE%02d (in %d days): %s", season, episode, days, title);
		}
		else
		{
			return String.format(Locale.US, "S%02dE%02d (%d days ago): %s", season, episode, -days, title);
		}
	}

	private static final String TAG = "EpisodeInfo";
	private int season;
	private int episode;
	private String title;

	private Calendar airTime;
}
