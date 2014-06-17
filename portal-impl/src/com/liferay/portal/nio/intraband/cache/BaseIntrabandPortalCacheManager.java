/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.nio.intraband.cache;

import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.cache.PortalCacheManager;
import com.liferay.portal.kernel.nio.intraband.RegistrationReference;
import com.liferay.portal.kernel.nio.intraband.proxy.ExceptionHandler;
import com.liferay.portal.nio.intraband.proxy.IntrabandProxyUtil;
import com.liferay.portal.nio.intraband.proxy.WarnLogExceptionHandler;

import java.io.Serializable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shuyang Zhou
 */
public abstract class BaseIntrabandPortalCacheManager
		<K extends Serializable, V extends Serializable>
	implements PortalCacheManager<K, V> {

	public static Class<? extends PortalCache<?, ?>> getPortalCacheStubClass() {
		return _stubClass;
	}

	@Override
	public void destroy() {
		_portalCaches.clear();
	}

	@Override
	public PortalCache<K, V> getCache(String name) {
		return getCache(name, false);
	}

	@Override
	public PortalCache<K, V> getCache(String name, boolean blocking) {
		PortalCache<K, V> portalCache = _portalCaches.get(name);

		if (portalCache == null) {
			portalCache = (PortalCache<K, V>)IntrabandProxyUtil.newStubInstance(
				_stubClass, name, _registrationReference, _EXCEPTION_HANDLER);

			_portalCaches.put(name, portalCache);
		}

		return portalCache;
	}

	@Override
	public void removeCache(String name) {
		_portalCaches.remove(name);
	}

	private static final ExceptionHandler _EXCEPTION_HANDLER =
		new WarnLogExceptionHandler();

	private static final Class<? extends PortalCache<?, ?>> _stubClass =
		(Class<? extends PortalCache<?, ?>>)
			IntrabandProxyUtil.getStubClass(
				PortalCache.class, PortalCache.class.getName());

	private final Map<String, PortalCache<K, V>> _portalCaches =
		new ConcurrentHashMap<String, PortalCache<K, V>>();
	private RegistrationReference _registrationReference;

}