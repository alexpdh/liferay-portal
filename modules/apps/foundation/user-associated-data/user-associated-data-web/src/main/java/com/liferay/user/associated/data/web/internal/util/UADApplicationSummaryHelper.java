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

package com.liferay.user.associated.data.web.internal.util;

import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.PortletURLUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.user.associated.data.anonymizer.UADEntityAnonymizer;
import com.liferay.user.associated.data.display.UADEntityDisplay;
import com.liferay.user.associated.data.web.internal.display.UADApplicationSummaryDisplay;
import com.liferay.user.associated.data.web.internal.registry.UADRegistry;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Drew Brokke
 */
@Component(immediate = true, service = UADApplicationSummaryHelper.class)
public class UADApplicationSummaryHelper {

	public SearchContainer<UADApplicationSummaryDisplay> createSearchContainer(
		RenderRequest renderRequest, RenderResponse renderResponse,
		long userId) {

		PortletRequest portletRequest =
			(PortletRequest)renderRequest.getAttribute(
				JavaConstants.JAVAX_PORTLET_REQUEST);
		LiferayPortletResponse liferayPortletResponse =
			_portal.getLiferayPortletResponse(
				(PortletResponse)renderRequest.getAttribute(
					JavaConstants.JAVAX_PORTLET_RESPONSE));

		PortletURL currentURL = PortletURLUtil.getCurrent(
			_portal.getLiferayPortletRequest(portletRequest),
			liferayPortletResponse);

		SearchContainer<UADApplicationSummaryDisplay> searchContainer =
			new SearchContainer<>(portletRequest, currentURL, null, null);

		Stream<UADApplicationSummaryDisplay>
			uadApplicationSummaryDisplayStream =
				getUADApplicationSummaryDisplayStream(userId);

		List<UADApplicationSummaryDisplay> uadApplicationSummaryDisplays =
			uadApplicationSummaryDisplayStream.collect(Collectors.toList());

		searchContainer.setResults(uadApplicationSummaryDisplays);

		searchContainer.setTotal(uadApplicationSummaryDisplays.size());

		return searchContainer;
	}

	public List<UADEntityAnonymizer> getApplicationUADEntityAnonymizers(
		String applicationName) {

		Stream<UADEntityDisplay> uadEntityDisplayStream =
			getApplicationUADEntityDisplayStream(applicationName);

		return uadEntityDisplayStream.map(
			UADEntityDisplay::getKey
		).map(
			key -> _uadRegistry.getUADEntityAnonymizer(key)
		).collect(
			Collectors.toList()
		);
	}

	public Stream<UADEntityDisplay> getApplicationUADEntityDisplayStream(
		String applicationName) {

		Stream<UADEntityDisplay> uadEntityDisplayStream =
			_uadRegistry.getUADEntityDisplayStream();

		return uadEntityDisplayStream.filter(
			uadEntityDisplay -> applicationName.equals(
				uadEntityDisplay.getApplicationName()));
	}

	public String getDefaultUADRegistryKey(String applicationName) {
		Stream<UADEntityDisplay> uadEntityDisplayStream =
			getApplicationUADEntityDisplayStream(applicationName);

		return uadEntityDisplayStream.map(
			UADEntityDisplay::getKey
		).findFirst(
		).get();
	}

	public int getReviewableUADEntitiesCount(
		Stream<UADEntityDisplay> uadEntityDisplayStream, long userId) {

		return uadEntityDisplayStream.map(
			uadEntityDisplay -> uadEntityDisplay.getKey()
		).map(
			key -> _uadRegistry.getUADEntityAggregator(key)
		).mapToInt(
			uadEntityAggregator -> uadEntityAggregator.count(userId)
		).sum();
	}

	public UADApplicationSummaryDisplay getUADApplicationSummaryDisplay(
		String applicationName, long userId) {

		return new UADApplicationSummaryDisplay(
			getReviewableUADEntitiesCount(
				getApplicationUADEntityDisplayStream(applicationName), userId),
			applicationName, getDefaultUADRegistryKey(applicationName));
	}

	public Stream<UADApplicationSummaryDisplay>
		getUADApplicationSummaryDisplayStream(long userId) {

		Stream<UADEntityDisplay> uadEntityDisplayStream =
			_uadRegistry.getUADEntityDisplayStream();

		return uadEntityDisplayStream.map(
			UADEntityDisplay::getApplicationName
		).distinct(
		).sorted(
		).map(
			applicationName ->
				getUADApplicationSummaryDisplay(applicationName, userId)
		);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UADApplicationSummaryHelper.class);

	@Reference
	private Portal _portal;

	@Reference
	private UADRegistry _uadRegistry;

}