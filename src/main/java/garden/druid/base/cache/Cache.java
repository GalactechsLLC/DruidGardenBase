package garden.druid.base.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import garden.druid.base.logging.Logger;

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
 * This Class is used as a rolling cache intended for use in a webserver, it uses a LinkedHashMap under the hood and is thread safe
 *   
 * @param <T> The object type to be used as a key
 * @param <U> The object type to be cached
 **/
public class Cache<T, U> {
	private LinkedHashMap<T, U> internalMap;
	private ReadWriteLock readWriteLock;
	private final int size;

	public Cache(int size) {
		if (size <= 0) {
			Logger.getInstance().log(Level.WARNING, "Warning, No Max Size Set for Cache, this could consume alot fo memory");
			this.size = Integer.MAX_VALUE;
		} else {
			this.size = size;
		}
		internalMap = new LinkedHashMap<T, U>(size) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Entry<T, U> eldest) {
				return size() > size && size > 0;
			}
		};
		readWriteLock = new ReentrantReadWriteLock();
	}

	public int size() {
		return this.size;
	}

	public void put(T t, U u) {
		Lock writeLock = readWriteLock.writeLock();
		try {
			writeLock.lock();
			internalMap.put(t, u);
		} finally {
			writeLock.unlock();
		}
	}

	public U get(T t) {
		Lock readLock = readWriteLock.readLock();
		try {
			readLock.lock();
			return internalMap.get(t);
		} finally {
			readLock.unlock();
		}
	}

	public boolean containsKey(T t) {
		Lock readLock = readWriteLock.readLock();
		try {
			readLock.lock();
			return internalMap.containsKey(t);
		} finally {
			readLock.unlock();
		}
	}

	public U remove(T t) {
		Lock writeLock = readWriteLock.writeLock();
		try {
			writeLock.lock();
			return internalMap.remove(t);
		} finally {
			writeLock.unlock();
		}
	}

	public Set<Entry<T, U>> entrySet() {
		Lock readLock = readWriteLock.readLock();
		try {
			readLock.lock();
			return internalMap.entrySet();
		} finally {
			readLock.unlock();
		}
	}

	public String toString() {
		Lock readLock = readWriteLock.readLock();
		try {
			if (internalMap == null || internalMap.size() == 0) {
				return "{}";
			}
			readLock.lock();
			StringBuilder builder = new StringBuilder();
			builder.append("{");
			for (Entry<T, U> entry : internalMap.entrySet()) {
				builder.append("(\"");
				builder.append(entry.getKey().toString());
				builder.append("\",\"");
				builder.append(entry.getValue().toString());
				builder.append("\"),");
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append("}");
			return builder.toString();
		} finally {
			readLock.unlock();
		}
	}
}
