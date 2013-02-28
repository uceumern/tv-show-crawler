package com.example.tvshowcrawler;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TVShowAdapter extends ArrayAdapter<TVShow>
{
	private List<TVShow> items;
	private Context context;

	public TVShowAdapter(Context context, int textViewResourceId, List<TVShow> objects)
	{
		super(context, textViewResourceId, objects);
		this.items = objects;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView;
		if (view == null)
		{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.list_row, null);
		}

		TVShow item = items.get(position);
		if (item != null)
		{
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView status = (TextView) view.findViewById(R.id.status);
			TextView last_episode_content = (TextView) view.findViewById(R.id.last_episode_content);
			TextView next_episode_content = (TextView) view.findViewById(R.id.next_episode_content);
			TextView current = (TextView) view.findViewById(R.id.current);

			title.setText(item.getName());
			status.setText(item.getShowStatus());
			if (item.getLastEpisode() != null)
				last_episode_content.setText(item.getLastEpisode().toString());
			else
				last_episode_content.setText("null");
			if (item.getNextEpisode() != null)
				next_episode_content.setText(item.getNextEpisode().toString());
			else
				next_episode_content.setText("null");
			current.setText(String.format("S%02dE%02d", item.getSeason(), item.getEpisode()));
		}

		return view;
	}

}
