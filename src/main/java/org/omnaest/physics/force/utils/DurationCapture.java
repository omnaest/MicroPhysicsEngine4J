package org.omnaest.physics.force.utils;

public class DurationCapture
{
	private long timeStamp;

	public DurationCapture()
	{
		super();
		this.start();
	}

	public void start()
	{
		this.timeStamp = System.currentTimeMillis();
	}

	public long stop()
	{
		return System.currentTimeMillis() - this.timeStamp;
	}

}
