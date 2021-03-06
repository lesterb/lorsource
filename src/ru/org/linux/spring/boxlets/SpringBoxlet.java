/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.spring.boxlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.spring.CacheableController;
import ru.org.linux.spring.commons.CacheProvider;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 0:55:16
 */
public abstract class SpringBoxlet extends AbstractController implements CacheableController{

  protected abstract ModelAndView getData(HttpServletRequest request,
                                          HttpServletResponse response);

  
  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
    ModelAndView mav = getData(request, response);
    if (mav == null){
      mav = new ModelAndView();
    }

    if (request.getParameterMap().containsKey("edit")){
      mav.addObject("editMode", Boolean.TRUE);
    }
    return mav;
  }

  protected abstract CacheProvider getCacheProvider();

  protected <T> T getFromCache(GetCommand<T> callback){
    return getFromCache(getCacheKey(), callback);
  }

  protected <T> T getFromCache(String key, GetCommand<T> callback){
    @SuppressWarnings("unchecked")
    T result = (T) getCacheProvider().getFromCache(key);
    if (result == null){
       result = callback.get();
       getCacheProvider().storeToCache(key, result, getExpiryTime());
    }
    return result;
  }

  public interface GetCommand<T>{
    T get();
  }

  @Override
  public String getCacheKey(){
    return getClass().getName();
  }

  @Override
  public Long getExpiryTime(){
    return DEFAULT_EXPIRE;
  }
}
