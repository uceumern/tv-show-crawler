package com.example.tvshowcrawler.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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

public class TVShowTest extends AndroidTestCase {
	public TVShowTest() {
		super();
	}

	@Override
	public void testAndroidTestCaseSetupProperly() {
		// test proper setup of tested class here
		super.testAndroidTestCaseSetupProperly();
	}

	public void testJSONSerialization() {
		EpisodeInfo outInfo = new EpisodeInfo();
		outInfo.setSeason(3);
		outInfo.setEpisode(17);
		outInfo.setTitle("Episode Title");
		outInfo.setAirTime(new GregorianCalendar());

		JSONObject outInfoObject = null;
		try {
			outInfoObject = outInfo.toJSONObject();
		} catch (JSONException e) {
			fail(e.toString());
		}
		String outInfoObjectString = null;
		try {
			outInfoObjectString = outInfoObject.toString(4);
		} catch (JSONException e) {
			fail(e.toString());
		}
		JSONObject inInfoObject = null;
		try {
			inInfoObject = new JSONObject(outInfoObjectString);
		} catch (JSONException e) {
			fail(e.toString());
		}
		EpisodeInfo inInfo = new EpisodeInfo();
		try {
			inInfo.fromJSONObject(inInfoObject);
		} catch (JSONException e) {
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
		try {
			joOut = showOut.toJSONObject();
		} catch (JSONException e) {
			fail(e.toString());
		}
		String jsonString = null;
		try {
			jsonString = joOut.toString(4);
		} catch (JSONException e) {
			fail(e.toString());
		}
		JSONObject joIn = null;
		try {
			joIn = new JSONObject(jsonString);
		} catch (JSONException e) {
			fail(e.toString());
		}
		TVShow showIn = new TVShow();
		try {
			showIn.fromJSONObject(joIn);
		} catch (JSONException e) {
			fail(e.toString());
		}
		assertEquals(showOut, showIn);

		// test file IO
		final String FILENAME = "tvshows-TEST.json";
		TVShows tvShowsOut = new TVShows();
		tvShowsOut.add(showOut);
		try {
			JSONUtils.saveAppJSONFile(getContext(), FILENAME,
					tvShowsOut.toJSONObject());
		} catch (JSONException e) {
			fail(e.toString());
		}
		File file = getContext().getFileStreamPath(FILENAME);
		assertTrue(file.exists());

		TVShows tvShowsIn = new TVShows();
		JSONObject jo = JSONUtils.loadAppJSONFile(getContext(), FILENAME);
		try {
			tvShowsIn.fromJSONObject(jo);
		} catch (JSONException e) {
			fail(e.toString());
		}
		assertEquals(tvShowsOut, tvShowsIn);
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

	public void testParcelable() {
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

	public void testQueryAllSites() {
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
		assertTrue(item.getName().contains(name)
				|| item.getName().contains(name.replaceAll(" ", ".")));
		assertTrue(!item.getMagnetLink().isEmpty());
		assertTrue(item.getMagnetLink().startsWith("magnet:?xt="));
	}

	public void testQueryKickAssTorrents() {
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

	public void testQueryPirateBay() {
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
		assertTrue(item.getName().contains(name)
				|| item.getName().contains(name.replaceAll(" ", ".")));
		assertTrue(!item.getMagnetLink().isEmpty());
		assertTrue(item.getMagnetLink().startsWith("magnet:?xt="));
	}

	public void testReadFromUrl() {
		try {
			URL url = new URL("http://de.wikipedia.org/wiki/Test");
			String html = TVShow.readFromUrl(url);
			assertTrue(html.length() > 0);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void testTimeParsing() {
		// 2013-05-12
		String inputAirDate = "2013-05-12";
		String inputTimeZone = "GMT-5 +DST";
		String inputAirTime = "21:00";

		// remove DST from string, not supported by TimeZone
		if (inputTimeZone.contains(" +DST")) {
			inputTimeZone = inputTimeZone.replace(" +DST", "");
		}
		TimeZone tz = TimeZone.getTimeZone(inputTimeZone);
		assertEquals(-5 * 60 * 60 * 1000, tz.getRawOffset());

		SimpleDateFormat sdfAirDateFormat = new SimpleDateFormat("yyyy-MM-dd",
				Locale.US);
		sdfAirDateFormat.setTimeZone(tz);
		Calendar calAirDate = new GregorianCalendar(tz, Locale.US);
		try {
			calAirDate.setTime(sdfAirDateFormat.parse(inputAirDate));
		} catch (ParseException e) {
			fail(e.toString());
		}

		Calendar calAirTime = new GregorianCalendar(tz, Locale.US);
		try {
			// extract air time (of day)
			SimpleDateFormat sdfAirTimeFormat = new SimpleDateFormat("hh:mm",
					Locale.US);
			sdfAirTimeFormat.setTimeZone(tz);
			calAirTime.setTime(sdfAirTimeFormat.parse(inputAirTime));
		} catch (ParseException e) {
			fail(e.toString());
		}
		int hours = calAirTime.get(Calendar.HOUR_OF_DAY);
		int minutes = calAirTime.get(Calendar.MINUTE);

		assertEquals(21, hours);
		assertEquals(0, minutes);

		calAirDate.add(Calendar.HOUR_OF_DAY, hours);
		calAirDate.add(Calendar.MINUTE, minutes);

		// 2013-05-12 21:00:00.000-0500
		int year = calAirDate.get(Calendar.YEAR);
		int month = calAirDate.get(Calendar.MONTH);
		int day = calAirDate.get(Calendar.DAY_OF_MONTH);
		hours = calAirDate.get(Calendar.HOUR_OF_DAY);
		minutes = calAirDate.get(Calendar.MINUTE);
		int tzOffset = calAirDate.get(Calendar.ZONE_OFFSET);

		assertEquals(2013, year);
		assertEquals(Calendar.MAY, month);
		assertEquals(12, day);
		assertEquals(21, hours);
		assertEquals(0, minutes);
		assertEquals(-5 * 60 * 60 * 1000, tzOffset); // -5 hours in ms
	}

	@Override
	protected void setUp() throws Exception {
		// called before first test
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		// called after last test
		super.tearDown();
	}
}
