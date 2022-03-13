package garden.druid.base.cache;

/**
 * @author Galactechs LLC.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.  
 * 
 * <p> 
 * This Class is used as a simple wrapper object to add a expire time to data of type <T>
 *   
 **/
public class DataCache<T> {

	private final long timeStamp, expireTime;
	private T data;

	public DataCache(T data, long expireTime) {
		this.data = data;
		this.expireTime = expireTime;
		this.timeStamp = System.currentTimeMillis();
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean isExpired() {
		if (this.expireTime > 0) {
			return ((System.currentTimeMillis() - this.timeStamp) > this.expireTime);
		} else {
			return false;
		}
	}
}