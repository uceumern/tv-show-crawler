package com.example.tvshowcrawler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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
				&& episode == rhs.getEpisode()/*
											 * && airTime.getTimeInMillis() == rhs.getAirTime().getTimeInMillis()
											 */;
	}

	@Override
	public void fromJSONObject(JSONObject src) throws JSONException
	{
		title = src.getString("title");
		season = src.getInt("season");
		episode = src.getInt("episode");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.US);
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.US);
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

	public static EpisodeInfo fromString(String input)
	{
		try
		{
			EpisodeInfo ei = new EpisodeInfo();
			// should be: SeasonxEpisode^Title^AirDate
			// e.g.: 01x19^Secrets^Apr/03/2012
			String temp = input;
			String[] episodeInfo = temp.split("\\^");
			{
				String[] splitMe = episodeInfo[0].split("x");
				ei.setSeason(Integer.parseInt(splitMe[0]));
				ei.setEpisode(Integer.parseInt(splitMe[1]));
			}
			ei.setTitle(episodeInfo[1]);
			ei.setAirTime(extractUTC(episodeInfo[2]));
			return ei;
		} catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	private static Calendar extractUTC(String input)
	{
		Calendar cal = null;
		try
		{
			SimpleDateFormat sdfToDate = new SimpleDateFormat("MMM/dd/yyyy", Locale.US);
			cal = new GregorianCalendar(TimeZone.getTimeZone("ET"));
			cal.setTime(sdfToDate.parse(input));
			cal.add(Calendar.HOUR_OF_DAY, 21); // shows usually air around 9pm ET

			// shows need some time to appear on torrent sites... maybe 6-12 hours?
			cal.add(Calendar.HOUR_OF_DAY, 6);

			// convert to local (UTC) time
			Calendar localTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			localTime.setTimeInMillis(cal.getTimeInMillis());
			cal = localTime;
		} catch (ParseException ex2)
		{
			ex2.printStackTrace();
		}
		return cal;
	}
}
