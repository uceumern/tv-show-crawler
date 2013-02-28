package com.example.tvshowcrawler;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TVShows extends ArrayList<TVShow> implements JSONable
{
	@Override
	public void fromJSONObject(JSONObject src) throws JSONException
	{
		clear();
		JSONArray ja = src.getJSONArray("shows");
		for (int i = 0; i < ja.length(); i++)
		{
			JSONObject jo = ja.getJSONObject(i);
			TVShow show = new TVShow();
			show.fromJSONObject(jo);
			add(show);
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException
	{
		JSONObject jo = new JSONObject();
		JSONArray ja = new JSONArray();
		for (TVShow show : this)
		{
			ja.put(show.toJSONObject());
		}
		jo.put("shows", ja);
		return jo;
	}

	private static final long serialVersionUID = 1L;
}