package com.sjdf.util;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;;

public class PagerArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String PAGE = "page";
    private static final String SIZE = "size";
    private static final Integer MAX_SIZE = 500;

    public boolean supportsParameter(MethodParameter parameter) {
        //仅作用于添加了注解PagerResolver的参数
        return parameter.getParameterAnnotation(PagerResolver.class) != null;
    }

    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        PagerResolver pagerResolver = parameter.getParameterAnnotation(PagerResolver.class);
        Boolean required = pagerResolver.required();
        String page = webRequest.getParameter(PAGE);
        String size = webRequest.getParameter(SIZE);
        if(required) {
             if(page == null || size == null) {
                 throw new Exception("page or limit param do not exist!");
             }
        }
        PagerInfo pager = new PagerInfo();
        if(page != null) {
            pager.setPage(Integer.parseInt(page));
        }
        if(size != null) {
            int sizeInt = Integer.parseInt(size);
            pager.setSize(sizeInt > MAX_SIZE ? MAX_SIZE : sizeInt);
        }
        return pager;
    }

}
