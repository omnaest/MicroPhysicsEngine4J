/*

	Copyright 2017 Danny Kunz

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.


*/
package org.omnaest.physics.domain.force.utils;

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

	public static DurationCapture getInstance()
	{
		return new DurationCapture();
	}

}
