package com.example.tvshowcrawler;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class TVRageXmlParser
{
	// We don't use namespaces
	private static final String ns = null;

	public TVShow parse(StringReader srReader, TVShow show) throws XmlPullParserException, IOException
	{
		try
		{
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(srReader);
			parser.nextTag();

			TVShow ret = show;
			String airTime = null;
			String airTimeZone = null;

			parser.require(XmlPullParser.START_TAG, ns, "Show");
			while (parser.next() != XmlPullParser.END_TAG)
			{
				if (parser.getEventType() != XmlPullParser.START_TAG)
				{
					continue;
				}
				String tagName = parser.getName();
				if (tagName.equals("name"))
				{
					// show name
					skip(parser);
				}
				else if (tagName.equals("showlink"))
				{
					// url to show on tv rage
					skip(parser);
				}
				else if (tagName.equals("started"))
				{
					// show start date
					skip(parser);
				}
				else if (tagName.equals("ended"))
				{
					// show end date (non-empty when show ended)
					skip(parser);
				}
				else if (tagName.equals("image"))
				{
					// show icon
					skip(parser);
				}
				else if (tagName.equals("status"))
				{
					// show status
					ret.setShowStatus(readText(parser));
				}
				else if (tagName.equals("airtime"))
				{
					// show air time
					airTime = readText(parser);
				}
				else if (tagName.equals("timezone"))
				{
					// show air time time zone
					airTimeZone = readText(parser);
				}
				else if (tagName.equals("Episodelist"))
				{
					// full list of all episodes
					ArrayList<EpisodeInfo> list = readEpisodeList(parser);
					show.setEpisodeList(list);
				}
				else
				{
					skip(parser);
				}
			}
			// update airTimes in episode list
			for (EpisodeInfo eInfo : show.getEpisodeList())
			{
				try
				{
					// extract air time (of day)
					SimpleDateFormat sdfToDate = new SimpleDateFormat("hh:mm", Locale.US);
					Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(airTimeZone));
					cal.setTime(sdfToDate.parse(airTime));
					int hours = cal.get(Calendar.HOUR_OF_DAY);
					int minutes = cal.get(Calendar.MINUTE);

					// set correct time zone, add air time
					Calendar unfixedAirTime = eInfo.getAirTime();
					Log.d(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(unfixedAirTime
							.getTime()));
					unfixedAirTime.setTimeZone(TimeZone.getTimeZone(airTimeZone));
					unfixedAirTime.add(Calendar.HOUR_OF_DAY, hours);
					unfixedAirTime.add(Calendar.MINUTE, minutes);
					Log.d(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(unfixedAirTime
							.getTime()));
					// convert to UTC time
					Calendar localTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
					localTime.setTimeInMillis(cal.getTimeInMillis());
					Log.d(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(localTime
							.getTime()));
					eInfo.setAirTime(localTime);
				} catch (ParseException e)
				{
					Log.e(TAG, e.toString());
					eInfo.setAirTime(null);
				}
			}

			return ret;

		} finally
		{
			srReader.close();
		}
	}

	private ArrayList<EpisodeInfo> readEpisodeList(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		ArrayList<EpisodeInfo> ret = new ArrayList<EpisodeInfo>();

		int currentSeason = 0;
		parser.require(XmlPullParser.START_TAG, ns, "Episodelist");
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}
			String tag = parser.getName();
			if (tag.equals("Season"))
			{
				String number = parser.getAttributeValue(null, "no");
				currentSeason = Integer.parseInt(number);
				ret.addAll(readSeason(parser, currentSeason));
			} else
			{
				skip(parser);
			}
		}
		return ret;
	}

	private ArrayList<EpisodeInfo> readSeason(XmlPullParser parser, int currentSeason) throws XmlPullParserException,
			IOException
	{
		ArrayList<EpisodeInfo> ret = new ArrayList<EpisodeInfo>();
		parser.require(XmlPullParser.START_TAG, ns, "Season");
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("episode"))
			{
				EpisodeInfo eInfo = readEpisode(parser);
				eInfo.setSeason(currentSeason);
				ret.add(eInfo);
			} else
			{
				skip(parser);
			}
		}
		return ret;
	}

	private EpisodeInfo readEpisode(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		EpisodeInfo ret = new EpisodeInfo();
		parser.require(XmlPullParser.START_TAG, ns, "episode");
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}
			String name = parser.getName();
			if (name.equals("seasonnum"))
			{
				String number = readText(parser);
				int episode = Integer.parseInt(number);
				ret.setEpisode(episode);
			} else if (name.equals("airdate"))
			{
				String airdate = readText(parser);
				GregorianCalendar cal = null;
				try
				{
					// 2013-05-12
					SimpleDateFormat sdfToDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
					cal = new GregorianCalendar();
					cal.setTime(sdfToDate.parse(airdate));
				} catch (ParseException e)
				{
					Log.e(TAG, e.toString());
				}

				ret.setAirTime(cal);
			} else if (name.equals("title"))
			{
				String title = readText(parser);
				ret.setTitle(title);
			}
			else
			{
				skip(parser);
			}
		}
		return ret;
	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException
	{
		String result = "";
		if (parser.next() == XmlPullParser.TEXT)
		{
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		if (parser.getEventType() != XmlPullParser.START_TAG)
		{
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0)
		{
			switch (parser.next())
			{
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	private static final String TAG = "TVRageXmlParser";
}
