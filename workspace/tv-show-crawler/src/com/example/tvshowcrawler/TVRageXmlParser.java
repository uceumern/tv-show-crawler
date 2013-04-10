package com.example.tvshowcrawler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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

			parser.require(XmlPullParser.START_TAG, ns, "Show");
			while (parser.next() != XmlPullParser.END_TAG)
			{
				if (parser.getEventType() != XmlPullParser.START_TAG)
				{
					continue;
				}
				String name = parser.getName();
				String tagName = parser.getName();
				if (tagName.equals("name"))
				{
					// show name
				}
				else if (tagName.equals("showlink"))
				{
					// url to show on tv rage
				}
				else if (tagName.equals("started"))
				{
					// show start date
				}
				else if (tagName.equals("ended"))
				{
					// show end date (non-empty when show ended)
				}
				else if (tagName.equals("image"))
				{
					// show icon
				}
				else if (tagName.equals("status"))
				{
					// show status
					ret.setShowStatus(readText(parser));
				}
				else if (tagName.equals("airtime"))
				{
					// show air time
				}
				else if (tagName.equals("timezone"))
				{
					// show air time time zone
				}
				else if (tagName.equals("Episodelist"))
				{
					// full list of all episodes
					ArrayList<EpisodeInfo> list = readEpisodeList(parser);
				}
				else
				{
					skip(parser);
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
				parser.nextTag();
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
			// Starts by looking for the entry tag
			if (name.equals("epnum"))
			{
				String number = readText(parser);
				int episode = Integer.parseInt(number);
				ret.setEpisode(episode);
			} else if (name.equals("airdate"))
			{
				String airdate = readText(parser);
//				ret.setAirTime(airdate);
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
}
