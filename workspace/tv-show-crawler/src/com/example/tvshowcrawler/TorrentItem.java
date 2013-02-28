package com.example.tvshowcrawler;

public class TorrentItem
{
	public int getEpisode()
	{
		return episode;
	}

	public String getMagnetLink()
	{
		return magnetLink;
	}

	public String getName()
	{
		return name;
	}

	public int getSeason()
	{
		return season;
	}

	public boolean isIs720p()
	{
		return is720p;
	}

	public boolean isIsx264()
	{
		return isx264;
	}

	public void setEpisode(int episode)
	{
		this.episode = episode;
	}

	public void setIs720p(boolean is720p)
	{
		this.is720p = is720p;
	}

	public void setIsx264(boolean isx264)
	{
		this.isx264 = isx264;
	}

	public void setMagnetLink(String magnetLink)
	{
		this.magnetLink = magnetLink;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setSeason(int season)
	{
		this.season = season;
	}

	private String name;
	private String magnetLink;
	private boolean is720p;
	private boolean isx264;
	private int season;
	private int episode;
}
