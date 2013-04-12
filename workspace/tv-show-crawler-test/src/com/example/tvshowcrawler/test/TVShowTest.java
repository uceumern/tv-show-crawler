package com.example.tvshowcrawler.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.example.tvshowcrawler.EpisodeInfo;
import com.example.tvshowcrawler.JSONUtils;
import com.example.tvshowcrawler.TVShow;
import com.example.tvshowcrawler.TVShow.EnumTVShowStatus;
import com.example.tvshowcrawler.TVShows;
import com.example.tvshowcrawler.TorrentItem;

public class TVShowTest extends AndroidTestCase
{
	public TVShowTest()
	{
		super();
	}

	@Override
	public void testAndroidTestCaseSetupProperly()
	{
		// test proper setup of tested class here
		super.testAndroidTestCaseSetupProperly();
	}

	public void testJSONSerialization()
	{
		EpisodeInfo outInfo = new EpisodeInfo();
		outInfo.setSeason(3);
		outInfo.setEpisode(17);
		outInfo.setTitle("Episode Title");
		outInfo.setAirTime(new GregorianCalendar());
		
		JSONObject outInfoObject = null;
		try
		{
			outInfoObject = outInfo.toJSONObject();
		} catch (JSONException e) {
			fail(e.toString());
		}
		String outInfoObjectString = null;
		try
		{
			outInfoObjectString = outInfoObject.toString(4);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		JSONObject inInfoObject = null;
		try
		{
			inInfoObject = new JSONObject(outInfoObjectString);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		EpisodeInfo inInfo = new EpisodeInfo();
		try
		{
			inInfo.fromJSONObject(inInfoObject);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		assertEquals(outInfo, inInfo);
		
		String name = "New Girl";
		int season = 1;
		int episode = 17;

		TVShow showOut = new TVShow(name, season, episode);
		ArrayList<String> excludedKeyWords = new ArrayList<String>();
		excludedKeyWords.add("keyword1");
		excludedKeyWords.add("keyword2");
		excludedKeyWords.add("keyword3");
		showOut.setExcludedKeyWords(excludedKeyWords);
		showOut.setLastEpisode(outInfo);
		showOut.setShowStatus("TestShowStaus");
		showOut.setMagnetLink("magnet:blah.fasel");
		showOut.setStatus(EnumTVShowStatus.UpToDate);

		JSONObject joOut = null;
		try
		{
			joOut = showOut.toJSONObject();
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		String jsonString = null;
		try
		{
			jsonString = joOut.toString(4);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		JSONObject joIn = null;
		try
		{
			joIn = new JSONObject(jsonString);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		TVShow showIn = new TVShow();
		try
		{
			showIn.fromJSONObject(joIn);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		assertEquals(showOut, showIn);

		// test file IO
		final String FILENAME = "tvshows-TEST.json";
		TVShows tvShowsOut = new TVShows();
		tvShowsOut.add(showOut);
		try
		{
			JSONUtils.saveAppJSONFile(getContext(), FILENAME, tvShowsOut.toJSONObject());
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		File file = getContext().getFileStreamPath(FILENAME);
		assertTrue(file.exists());

		TVShows tvShowsIn = new TVShows();
		JSONObject jo = JSONUtils.loadAppJSONFile(getContext(), FILENAME);
		try
		{
			tvShowsIn.fromJSONObject(jo);
		} catch (JSONException e)
		{
			fail(e.toString());
		}
		assertEquals(tvShowsOut, tvShowsIn);
	}

	public void testParcelable()
	{
		EpisodeInfo outInfo = new EpisodeInfo();
		outInfo.setSeason(3);
		outInfo.setEpisode(17);
		outInfo.setTitle("Episode Title");
		outInfo.setAirTime(new GregorianCalendar());
		
		String name = "New Girl";
		int season = 1;
		int episode = 17;

		TVShow showOut = new TVShow(name, season, episode);
		showOut.setLastEpisode(outInfo);
		showOut.setShowStatus("TestShowStaus");
		showOut.setMagnetLink("magnet:blah.fasel");
		ArrayList<String> excludedKeyWords = new ArrayList<String>();
		excludedKeyWords.add("keyword1");
		excludedKeyWords.add("keyword2");
		excludedKeyWords.add("keyword3");
		showOut.setExcludedKeyWords(excludedKeyWords);

		Parcel parcel = Parcel.obtain();
		showOut.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		TVShow createFromParcel = TVShow.CREATOR.createFromParcel(parcel);

		assertEquals(showOut, createFromParcel);
		parcel.recycle();
	}

	public void testQueryAllSites()
	{
		String name = "New Girl";
		int season = 1;
		int episode = 17;

		TVShow show = new TVShow(name, season, episode);

		assertEquals(show.getTorrentItems(), null);
		show.queryAllSites(season, episode);
		ArrayList<TorrentItem> torrentItems = show.getTorrentItems();
		assertTrue(torrentItems != null);
		assertTrue(torrentItems.size() > 0);
		TorrentItem item = torrentItems.get(0);
		assertTrue(item.getName().contains(name) || item.getName().contains(name.replaceAll(" ", ".")));
		assertTrue(!item.getMagnetLink().isEmpty());
		assertTrue(item.getMagnetLink().startsWith("magnet:?xt="));
	}

	public void testQueryKickAssTorrents()
	{
		String name = "New Girl";
		int season = 1;
		int episode = 17;

		TVShow show = new TVShow(name, season, episode);

		assertEquals(show.getTorrentItems(), null);
		show.queryKickAssTorrents(season, episode);
		ArrayList<TorrentItem> torrentItems = show.getTorrentItems();
		assertTrue(torrentItems != null);
		assertTrue(torrentItems.size() > 0);
		TorrentItem item = torrentItems.get(0);
		assertTrue(item.getName().contains(name));
		assertTrue(!item.getMagnetLink().isEmpty());
		assertTrue(item.getMagnetLink().startsWith("magnet:?xt="));
	}

	public void testQueryPirateBay()
	{
		String name = "New Girl";
		int season = 1;
		int episode = 17;

		TVShow show = new TVShow(name, season, episode);

		assertEquals(show.getTorrentItems(), null);
		show.queryPirateBay(season, episode);
		ArrayList<TorrentItem> torrentItems = show.getTorrentItems();
		assertTrue(torrentItems != null);
		assertTrue(torrentItems.size() > 0);
		TorrentItem item = torrentItems.get(0);
		assertTrue(item.getName().contains(name) || item.getName().contains(name.replaceAll(" ", ".")));
		assertTrue(!item.getMagnetLink().isEmpty());
		assertTrue(item.getMagnetLink().startsWith("magnet:?xt="));
	}

	public void testReadFromUrl()
	{
		try
		{
			URL url = new URL("http://de.wikipedia.org/wiki/Test");
			String html = TVShow.readFromUrl(url);
			assertTrue(html.length() > 0);
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void setUp() throws Exception
	{
		// called before first test
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception
	{
		// called after last test
		super.tearDown();
	}
	
	public void testNewTVRageParsing() {
		String name = "Game of Thrones";
		int season = 3;
		int episode = 2;
		String id = "24493";

		TVShow show = new TVShow(name, season, episode);
		show.setId(id);
		
		show.updateTVRageShowInfoAndEpisodeList(false);
		
	}
}
