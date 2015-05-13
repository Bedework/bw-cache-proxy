/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.cache.rewrite;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;

import java.text.MessageFormat;

/**
 * User: douglm
 * Date: 4/21/15
 * Time: 1:54 PM
 */
public class RewriteHandler implements Handler<HttpServerRequest> {
    private Logger log;

    private boolean debugEnabled;
    private int instanceId;
    private HttpClient client;
    private int requestCounter = 0;

    /**
     * Constructor.
     * @param log for logging
     * @param instanceId for logging
     * @param client our client
     */
    public RewriteHandler(final Logger log,
                          final int instanceId,
                          final HttpClient client) {
        this.log = log;
        debugEnabled = log.isDebugEnabled();
        this.instanceId = instanceId;
        this.client = client;
    }

    @Override
    public void handle(final HttpServerRequest request) {
        final int requestId = requestCounter++;

        final String uri = request.uri();
        final String method = request.method();

        debug(requestId, "Rewriting: " + uri);

        request.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                debug(requestId, "    Error caught ({0}), ending client request", event.getMessage());
//                clientReq.end();
            }
        });
        request.endHandler(new VoidHandler() {
            public void handle() {
                debug(requestId, "    Ending client request");
//                clientReq.end();
            }
        });
    }

    /*
    ###################################################
#
#  Licensed to Jasig under one or more contributor license
#  agreements. See the NOTICE file distributed with this work
#  for additional information regarding copyright ownership.
#  Jasig licenses this file to you under the Apache License,
#  Version 2.0 (the "License"); you may not use this file
#  except in compliance with the License. You may obtain a
#  copy of the License at:
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on
#  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied. See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
####################################################

class FeedModel
  attr_accessor :urlType, :reqParams
  attr_reader :listAction, :gridAction, :categoriesAction, :groupsAction, :eventAction, :downloadAction, :calendarsAction
  def initialize(urlType, reqParams)
    @urlType, @reqParams = urlType, reqParams
    @gridAction = 'main/setViewPeriod.do'
    #@listAction = 'main/listEvents.do'
    @listAction = 'main/eventsFeed.do'
    @categoriesAction = 'widget/categories.do'
    @groupsAction = 'widget/groups.do'
    @calendarsAction = 'calendar/fetchPublicCalendars.do'
    @eventAction = 'event/eventView.do'
    @downloadAction = 'misc/export.gdo'
  end

  def convertCats(catStr) # Convert ~ seperated string to bedework &cat=category format
    workStringArr = catStr.split(/~/)
    longCats = ''
    workStringArr.each do |workStr|
      longCats += '&catuid=' + workStr
    end
    return longCats
  end

  def addDateDashes (dateStr)
    return dateStr[0,4] + '-' + dateStr[4,2] + '-' + dateStr[6,2]
  end

  def cleanCats(catStr) #strip .html from from request and urlencode spaces
    workString = catStr.gsub('.html', '')
    workString = workString.gsub(' ', '%20')
    return workString
  end

  def getSkin(feedType) # determine by feedType
    return APP_CONFIG['skin'][feedType]
  end

  def buildUrl() # dispatcher

    target = case urlType
      when 'jsonDays' then getTarget2("json", "days")
      when 'icsDays' then getTarget2("ics", "days")
      when 'htmlDays' then getTarget2("html", "days")
      when 'xmlDays' then getTarget2("xml", "days")
      when 'rssDays' then getTarget2("rss", "days")
      when 'jsonRange' then getTarget2("json", "range")
      when 'icsRange' then getTarget2("ics", "range")
      when 'htmlRange' then getTarget2("html", "range")
      when 'xmlRange' then getTarget2("xml", "range")
      when 'rssRange' then getTarget2("rss", "range")
      #when 'genFeedPeriod' then getTarget(myFilter, "gen", "period", myObjName)
      #when 'icsPeriod' then getTarget(myFilter, "ics", "period", myObjName)
      when 'categories' then getCategories()
      when 'groups' then getGroups()
      when 'calendars' then getCalendars()
      when 'htmlEvent' then getEventTarget()
      when 'download' then getDownloadTarget()
      # when 'external' then getExt()
    end

    return target
  end

  def getTarget(filter, genOrIcs, daysRangeOrPeriod, obj)

    skin = getSkin(reqParams[:skin])
    skinSplits = skin.split(/-/)
    output = skinSplits[1]

    if reqParams[:filter]
      filter = reqParams[:filter]
    else
      filter = 'no--filter'
    end
    if reqParams[:objName]
      obj = reqParams[:objName]
    else
      obj = 'no--object'
    end

    if filter == 'no--filter'
      filterParam = ''
    else
      encodedFilter = CGI::escape(filter.gsub('-_','|'))
      filterParam = "&fexpr=" + encodedFilter
    end

    if obj == 'no--object'
      objParam = ''
    else
      objParam = "&setappvar=objName(" + obj + ")"
    end

    # set Bedework action
    if daysRangeOrPeriod == 'period'
      action = gridAction
    else
      action = listAction
    end

    bedeUrl = TARGETSERVER + "/" + action + "?f=y&calPath=/public/cals/MainCal"

    # build the Bedework URL and return it.
    if genOrIcs == 'ics'
      bedeUrl += "&format=text/calendar&setappvar=summaryMode(details)" + filterParam + objParam
    else
      bedeUrl += "&skinName=" + skin + "&setappvar=summaryMode(details)" + filterParam + objParam
    end
    if daysRangeOrPeriod == "range"
      if reqParams[:startDate] and reqParams[:endDate]
        if reqParams[:startDate] != "00000000"
          startD = addDateDashes(reqParams[:startDate])
          bedeUrl += "&start=" + startD
        end
        if reqParams[:endDate] != "00000000"
          endD = addDateDashes(reqParams[:endDate])
          bedeUrl += "&end=" + endD
        end
      end
    elsif daysRangeOrPeriod == "days"
      if (reqParams[:days] != "0")
        bedeUrl +="&days=" + reqParams[:days]
      end
    else
      if reqParams[:date] != "00000000"
        bedeUrl += "&date=" + reqParams[:date]
      end

      case reqParams[:period]
        when 'day' then bedeUrl +="&viewType=dayView"
        when 'week' then bedeUrl +="&viewType=weekView"
        when 'month' then bedeUrl +="&viewType=monthView"
        when 'year' then bedeUrl +="&viewType=yearView"
      end
    end
    return bedeUrl
  end

  def getTarget2(output, daysRangeOrPeriod)

    if reqParams[:filter] == 'no--filter'
      filterParam = ''
    else
      encodedFilter = CGI::escape(reqParams[:filter])
      filterParam = "&fexpr=" + encodedFilter
    end

    if output == "json"
      obj = reqParams[:objName]
      if obj == 'no--object'
        objParam = ''
      else
        objParam = "&setappvar=objName(" + obj + ")"
      end
    else
      objParam = ''
    end

    # set Bedework action
    #if daysRangeOrPeriod == 'period'
    #  action = gridAction
    #else
      action = listAction
    #end

    bedeUrl = TARGETSERVER + "/" + action + "?f=y&calPath=/public/cals/MainCal"

    # build the Bedework URL and return it.
    if output == 'ics'
      bedeUrl += "&format=text/calendar&setappvar=summaryMode(details)" + filterParam
    else
      skin = getSkin(reqParams[:skin])
      bedeUrl += "&skinName=" + skin + "&setappvar=summaryMode(details)" + filterParam + objParam
    end
    if daysRangeOrPeriod == "range"
      if reqParams[:startDate] and reqParams[:endDate]
        if reqParams[:startDate] != "00000000"
          startD = addDateDashes(reqParams[:startDate])
          bedeUrl += "&start=" + startD
        end
        if reqParams[:endDate] != "00000000"
          endD = addDateDashes(reqParams[:endDate])
          bedeUrl += "&end=" + endD
        end
      end
    elsif daysRangeOrPeriod == "days"
      if (reqParams[:days] != "0")
        bedeUrl +="&days=" + reqParams[:days]
      end
    else
      if reqParams[:date] != "00000000"
        bedeUrl += "&date=" + reqParams[:date]
      end

      case reqParams[:period]
        when 'day' then bedeUrl +="&viewType=dayView"
        when 'week' then bedeUrl +="&viewType=weekView"
        when 'month' then bedeUrl +="&viewType=monthView"
        when 'year' then bedeUrl +="&viewType=yearView"
      end
    end
    return bedeUrl
  end

  def getCategories()
    currSkin = getSkin(reqParams[:skin])
    obj = reqParams[:objName]
    bedeUrl = TARGETSERVER + "/" + categoriesAction + "?f=y&skinName=" + currSkin + "&setappvar=objName(" + obj + ")&calPath=/public/cals/MainCal"
    return bedeUrl
  end

  def getGroups()
    currSkin = getSkin(reqParams[:skin])
    obj = reqParams[:objName]
    bedeUrl = TARGETSERVER + "/" + groupsAction + "?f=y&skinName=" + currSkin + "&setappvar=objName(" + obj + ")&calPath=/public/cals/MainCal"
    return bedeUrl
  end

  def getCalendars()
    currSkin = getSkin(reqParams[:skin])
    obj = reqParams[:objName]
    bedeUrl = TARGETSERVER + "/" + calendarsAction + "?f=y&skinName=" + currSkin + "&setappvar=objName(" + obj + ")&calPath=/public/cals/MainCal"
    bedeUrl += '&setappvar=setSelectionAction(calsuiteSetSelectionAction)'
    return bedeUrl
  end

  def getDownloadTarget()
    calPathParam = '?f=y&calPath=%2Fpublic%2Fcals%2FMainCal'
    guidParam = "&guid=" + reqParams[:guid].gsub('_', '.')
    bedeUrl = TARGETSERVER + "/" + downloadAction + calPathParam + guidParam
    bedeUrl += "&recurrenceId=" + reqParams[:recurrenceId]
    bedeUrl += '&nocache=no&contentName=' + reqParams[:fileName]
    return bedeUrl
  end


  def getEventTarget() # Return a specific event in desired skin
    currSkin = getSkin(reqParams[:skin])
    skinParam = "?f=y&skinName=" + currSkin
    guidParam = "&guid=" + reqParams[:guid].gsub('_', '.')
    calPathParam = '&calPath=%2Fpublic%2Fcals%2FMainCal'
    bedeUrl = TARGETSERVER + "/" + eventAction + skinParam + calPathParam + guidParam
    bedeUrl += "&recurrenceId=" + reqParams[:recurrenceId]
    if reqParams[:date]
      bedeUrl += "&date=" + reqParams[:date]
    end
    return bedeUrl
  end

  #def getExt()

  #  return APP_CONFIG['externals'][reqParams[:feedName]]
  #end

end

     */

    /**
     * Write a debug message to standard output if debug is enabled.
     * @param id
     * @param message
     * @param args
     */
    protected final void debug(int id,
                               String message, Object ... args) {
        if (debugEnabled) {
            log.debug("" + instanceId + "-" + id + ":: " + MessageFormat.format(message, args));
        }
    }
}
